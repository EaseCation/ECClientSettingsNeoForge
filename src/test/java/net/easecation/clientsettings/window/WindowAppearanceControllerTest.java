package net.easecation.clientsettings.window;

import net.easecation.clientsettings.window.protocol.FrameAppearance;
import net.easecation.clientsettings.window.protocol.TitleAppearance;
import net.easecation.clientsettings.window.protocol.WindowAppearanceCommand;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class WindowAppearanceControllerTest {

    @Test
    void appliesAllowedFieldsAndDeduplicatesState() {
        Fixture fixture = new Fixture();
        WindowAppearanceCommand command = new WindowAppearanceCommand(
                TitleAppearance.custom("EaseCation"),
                FrameAppearance.dark()
        );

        assertEquals(WindowAppearanceController.ApplyResult.APPLIED, fixture.controller.apply(command));
        assertEquals("EaseCation", fixture.controller.titleOverride());
        assertEquals(FrameAppearance.dark(), fixture.backend.appearance);
        assertEquals(1, fixture.titleRefreshes.get());
        assertEquals(WindowAppearanceController.ApplyResult.UNCHANGED, fixture.controller.apply(command));
        assertEquals(1, fixture.backend.applyCount);
    }

    @Test
    void disabledFieldsAreNotCachedAndAreRestoredImmediately() {
        Fixture fixture = new Fixture();
        fixture.controller.apply(new WindowAppearanceCommand(
                TitleAppearance.custom("Server"), FrameAppearance.dark()
        ));

        fixture.allowTitle.set(false);
        fixture.allowFrame.set(false);
        fixture.controller.reconcilePermissions();

        assertNull(fixture.controller.titleOverride());
        assertNull(fixture.controller.frameAppearance());
        assertEquals(1, fixture.backend.restoreCount);
        assertEquals(WindowAppearanceController.ApplyResult.DENIED, fixture.controller.apply(
                new WindowAppearanceCommand(TitleAppearance.custom("Ignored"), FrameAppearance.dark())
        ));
        fixture.allowTitle.set(true);
        fixture.allowFrame.set(true);
        fixture.controller.reconcilePermissions();
        assertNull(fixture.controller.titleOverride());
        assertNull(fixture.controller.frameAppearance());
    }

    @Test
    void limitsStateChangesButAlwaysAllowsReset() {
        Fixture fixture = new Fixture();
        for (int index = 0; index < WindowAppearanceController.MAX_CHANGES_PER_SECOND; index++) {
            assertEquals(WindowAppearanceController.ApplyResult.APPLIED, fixture.controller.apply(
                    new WindowAppearanceCommand(TitleAppearance.custom("Title " + index), null)
            ));
        }

        assertEquals(WindowAppearanceController.ApplyResult.RATE_LIMITED, fixture.controller.apply(
                new WindowAppearanceCommand(TitleAppearance.custom("Too fast"), null)
        ));
        fixture.controller.reset();
        assertNull(fixture.controller.titleOverride());

        fixture.clock.addAndGet(1_000_000_000L);
        assertEquals(WindowAppearanceController.ApplyResult.APPLIED, fixture.controller.apply(
                new WindowAppearanceCommand(TitleAppearance.custom("Allowed again"), null)
        ));
    }

    @Test
    void disconnectRestoresEverythingAndClearsRateWindow() {
        Fixture fixture = new Fixture();
        for (int index = 0; index < WindowAppearanceController.MAX_CHANGES_PER_SECOND; index++) {
            fixture.controller.apply(new WindowAppearanceCommand(TitleAppearance.custom("Title " + index), null));
        }

        fixture.controller.disconnect();

        assertNull(fixture.controller.titleOverride());
        assertEquals(WindowAppearanceController.ApplyResult.APPLIED, fixture.controller.apply(
                new WindowAppearanceCommand(TitleAppearance.custom("New connection"), null)
        ));
    }

    private static final class Fixture {
        private final FakeBackend backend = new FakeBackend();
        private final AtomicInteger titleRefreshes = new AtomicInteger();
        private final AtomicBoolean allowTitle = new AtomicBoolean(true);
        private final AtomicBoolean allowFrame = new AtomicBoolean(true);
        private final AtomicLong clock = new AtomicLong();
        private final WindowAppearanceController controller = new WindowAppearanceController(
                backend,
                titleRefreshes::incrementAndGet,
                allowTitle::get,
                allowFrame::get,
                clock::get
        );
    }

    private static final class FakeBackend implements WindowFrameBackend {
        private FrameAppearance appearance;
        private int applyCount;
        private int restoreCount;

        @Override
        public void apply(FrameAppearance appearance) {
            this.appearance = appearance;
            applyCount++;
        }

        @Override
        public void restore() {
            appearance = null;
            restoreCount++;
        }
    }
}
