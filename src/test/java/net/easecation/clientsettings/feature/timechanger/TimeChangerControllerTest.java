package net.easecation.clientsettings.feature.timechanger;

import net.easecation.clientsettings.profile.model.TimeChangerMode;
import net.easecation.clientsettings.profile.model.TimeChangerSettings;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimeChangerControllerTest {

    private final Object world = new Object();
    private final TimeChangerController controller = new TimeChangerController();

    @Test
    void resolvesEveryPresetAndCustomEndpoint() {
        assertEquals(1_000, TimeChangerController.fixedDayTime(settings(TimeChangerMode.DAY, 0)));
        assertEquals(6_000, TimeChangerController.fixedDayTime(settings(TimeChangerMode.NOON, 0)));
        assertEquals(12_000, TimeChangerController.fixedDayTime(settings(TimeChangerMode.SUNSET, 0)));
        assertEquals(18_000, TimeChangerController.fixedDayTime(settings(TimeChangerMode.MIDNIGHT, 0)));
        assertEquals(0, TimeChangerController.fixedDayTime(settings(TimeChangerMode.CUSTOM, 0)));
        assertEquals(23_999, TimeChangerController.fixedDayTime(settings(TimeChangerMode.CUSTOM, 23_999)));
    }

    @Test
    void capturesTickingPacketAndRestoresElapsedGameAndDayTime() {
        ClientTimeValues fixed = controller.onServerTime(
                world, 100L, 6_000L, true, settings(TimeChangerMode.MIDNIGHT, 0)
        );
        assertEquals(new ClientTimeValues(100L, 18_000L, false), fixed);
        assertTrue(controller.hasServerBaseline());

        tick(5);
        ClientTimeValues restored = controller.applySettings(
                world, 105L, 18_000L, settings(TimeChangerMode.FOLLOW_SERVER, 0)
        ).orElseThrow();
        assertEquals(new ClientTimeValues(105L, 6_005L, true), restored);
    }

    @Test
    void disabledDaylightCycleAdvancesOnlyGameTime() {
        controller.onServerTime(world, 500L, 12_345L, false, settings(TimeChangerMode.DAY, 0));
        tick(20);

        ClientTimeValues restored = controller.restoreBeforeProfileSwitch(world).orElseThrow();

        assertEquals(new ClientTimeValues(520L, 12_345L, false), restored);
    }

    @Test
    void packetWhileFixedReplacesBaselineButNotVisibleTime() {
        controller.onServerTime(world, 100L, 1_000L, true, settings(TimeChangerMode.NOON, 0));
        tick(8);
        ClientTimeValues second = controller.onServerTime(
                world, 1_000L, 20_000L, true, settings(TimeChangerMode.NOON, 0)
        );
        assertEquals(new ClientTimeValues(1_000L, 6_000L, false), second);

        tick(2);
        assertEquals(
                new ClientTimeValues(1_002L, 20_002L, true),
                controller.restoreBeforeProfileSwitch(world).orElseThrow()
        );
    }

    @Test
    void fixedToFixedUsesCurrentGameTimeWithoutReplacingBaseline() {
        controller.onServerTime(world, 100L, 1_000L, true, settings(TimeChangerMode.DAY, 0));
        tick(5);

        ClientTimeValues noon = controller.applySettings(
                world, 105L, 1_000L, settings(TimeChangerMode.NOON, 0)
        ).orElseThrow();

        assertEquals(new ClientTimeValues(105L, 6_000L, false), noon);
        assertEquals(
                new ClientTimeValues(105L, 1_005L, true),
                controller.restoreBeforeProfileSwitch(world).orElseThrow()
        );
    }

    @Test
    void fixedBeforeFirstPacketSeedsApproximateBaseline() {
        ClientTimeValues fixed = controller.applySettings(
                world, 50L, 7_000L, settings(TimeChangerMode.CUSTOM, 23_999)
        ).orElseThrow();

        assertEquals(new ClientTimeValues(50L, 23_999L, false), fixed);
        assertTrue(controller.hasApproximateBaseline());
        assertFalse(controller.hasServerBaseline());

        controller.onServerTime(world, 80L, 8_000L, true, settings(TimeChangerMode.CUSTOM, 23_999));
        assertTrue(controller.hasServerBaseline());
        assertFalse(controller.hasApproximateBaseline());
    }

    @Test
    void worldAndLogoutClearAllBaselineState() {
        controller.onServerTime(world, 100L, 1_000L, true, settings(TimeChangerMode.DAY, 0));
        Object secondWorld = new Object();
        controller.enterWorld(secondWorld);
        assertFalse(controller.hasServerBaseline());
        assertTrue(controller.applySettings(
                secondWorld, 20L, 2_000L, settings(TimeChangerMode.FOLLOW_SERVER, 0)
        ).isEmpty());

        controller.onServerTime(secondWorld, 20L, 2_000L, true, settings(TimeChangerMode.DAY, 0));
        controller.clear();
        assertFalse(controller.hasServerBaseline());
    }

    @Test
    void longArithmeticWrapsWithoutNarrowing() {
        controller.onServerTime(
                world,
                Long.MAX_VALUE - 1,
                Long.MAX_VALUE - 1,
                true,
                settings(TimeChangerMode.DAY, 0)
        );
        tick(3);

        assertEquals(
                new ClientTimeValues(Long.MIN_VALUE + 1, Long.MIN_VALUE + 1, true),
                controller.restoreBeforeProfileSwitch(world).orElseThrow()
        );
    }

    private void tick(int count) {
        for (int index = 0; index < count; index++) {
            controller.tick();
        }
    }

    private static TimeChangerSettings settings(TimeChangerMode mode, int customTime) {
        return new TimeChangerSettings(mode, customTime);
    }
}
