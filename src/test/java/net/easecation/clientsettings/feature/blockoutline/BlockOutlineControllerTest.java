package net.easecation.clientsettings.feature.blockoutline;

import net.easecation.clientsettings.profile.model.ArgbColor;
import net.easecation.clientsettings.profile.model.BlockOutlineSettings;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockOutlineControllerTest {

    private final BlockOutlineController controller = new BlockOutlineController();

    @Test
    void disabledSettingDoesNotSubmitOrCancelVanilla() {
        AtomicInteger submissions = new AtomicInteger();

        boolean rendered = controller.tryRender(
                new BlockOutlineSettings(false, ArgbColor.parse("#CCFFFFFF")),
                color -> {
                    submissions.incrementAndGet();
                    return true;
                }
        );

        assertFalse(rendered);
        assertEquals(0, submissions.get());
    }

    @Test
    void enabledSettingPassesExactPackedArgbAndUsesSubmissionResult() {
        AtomicInteger submittedColor = new AtomicInteger();
        BlockOutlineSettings settings = new BlockOutlineSettings(true, ArgbColor.parse("#00123456"));

        assertTrue(controller.tryRender(settings, color -> {
            submittedColor.set(color);
            return true;
        }));
        assertEquals(0x00123456, submittedColor.get());
        assertFalse(controller.tryRender(settings, color -> false));
    }

    @Test
    void successiveProfilesAreReadWithoutCachedColor() {
        AtomicInteger submittedColor = new AtomicInteger();

        controller.tryRender(
                new BlockOutlineSettings(true, ArgbColor.parse("#80FF0000")),
                color -> {
                    submittedColor.set(color);
                    return true;
                }
        );
        assertEquals(0x80FF0000, submittedColor.get());

        controller.tryRender(
                new BlockOutlineSettings(true, ArgbColor.parse("#4000FF00")),
                color -> {
                    submittedColor.set(color);
                    return true;
                }
        );
        assertEquals(0x4000FF00, submittedColor.get());
    }
}
