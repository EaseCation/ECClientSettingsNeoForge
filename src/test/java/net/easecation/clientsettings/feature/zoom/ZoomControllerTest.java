package net.easecation.clientsettings.feature.zoom;

import net.easecation.clientsettings.profile.model.ZoomActivation;
import net.easecation.clientsettings.profile.model.ZoomSettings;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ZoomControllerTest {

    @Test
    void holdModeEntersStaysActiveAndAnimatesOnRelease() {
        ZoomController controller = new ZoomController();
        ZoomSettings settings = settings(ZoomActivation.HOLD, 4.0, 16.0, 10.0, false);

        controller.updateInput(settings, true, true, true);
        assertEquals(ZoomState.ENTERING, controller.state());
        controller.tick();
        assertEquals(ZoomState.ACTIVE, controller.state());
        assertEquals(0.25, controller.interpolatedMultiplier(1.0));

        controller.updateInput(settings, true, false, true);
        assertEquals(ZoomState.ACTIVE, controller.state());
        controller.updateInput(settings, false, false, true);
        assertEquals(ZoomState.EXITING, controller.state());
        controller.tick();
        assertEquals(ZoomState.INACTIVE, controller.state());
        assertEquals(1.0, controller.interpolatedMultiplier(1.0));
    }

    @Test
    void toggleModeFlipsOnlyOnClicks() {
        ZoomController controller = new ZoomController();
        ZoomSettings settings = settings(ZoomActivation.TOGGLE, 4.0, 16.0, 10.0, false);

        controller.updateInput(settings, false, true, true);
        controller.tick();
        assertTrue(controller.desiredActive());
        controller.updateInput(settings, false, false, true);
        assertTrue(controller.desiredActive());
        controller.updateInput(settings, false, true, true);
        assertFalse(controller.desiredActive());
    }

    @Test
    void speedMappingAndSmoothstepHaveDeterministicEndpoints() {
        assertEquals(10, ZoomController.transitionDuration(1.0));
        assertEquals(6, ZoomController.transitionDuration(5.0));
        assertEquals(1, ZoomController.transitionDuration(10.0));
        assertEquals(0.0, ZoomController.smoothstep(-1.0));
        assertEquals(0.0, ZoomController.smoothstep(0.0));
        assertEquals(0.5, ZoomController.smoothstep(0.5));
        assertEquals(1.0, ZoomController.smoothstep(1.0));
        assertEquals(1.0, ZoomController.smoothstep(2.0));
    }

    @Test
    void animationIsMonotonicAndInterpolatesBetweenTicks() {
        ZoomController controller = new ZoomController();
        ZoomSettings settings = settings(ZoomActivation.HOLD, 4.0, 16.0, 5.0, false);
        controller.updateInput(settings, true, false, true);

        double previous = 1.0;
        for (int tick = 0; tick < 6; tick++) {
            controller.tick();
            double current = controller.interpolatedMultiplier(1.0);
            assertTrue(current <= previous);
            assertTrue(controller.interpolatedMultiplier(0.5) >= current);
            previous = current;
        }
        assertEquals(0.25, previous);
    }

    @Test
    void scrollUsesOnlyVerticalSignAndCancelsOnlyOnChange() {
        ZoomController controller = activeController();
        ZoomSettings settings = settings(ZoomActivation.HOLD, 4.0, 5.0, 10.0, true);

        assertFalse(controller.handleScroll(settings, 0.0, true));
        assertTrue(controller.handleScroll(settings, 999.0, true));
        assertEquals(5.0, controller.activeDivisor());
        assertFalse(controller.handleScroll(settings, 1.0, true));
        assertTrue(controller.handleScroll(settings, -999.0, true));
        assertEquals(4.0, controller.activeDivisor());
        assertFalse(controller.handleScroll(settings, Double.NaN, true));
        assertFalse(controller.handleScroll(settings, 1.0, false));
    }

    @Test
    void divisorChangeStartsFromCurrentMultiplierWithoutJump() {
        ZoomController controller = new ZoomController();
        ZoomSettings settings = settings(ZoomActivation.HOLD, 4.0, 16.0, 5.0, true);
        controller.updateInput(settings, true, false, true);
        controller.tick();
        double before = controller.interpolatedMultiplier(1.0);

        assertTrue(controller.handleScroll(settings, 1.0, true));

        assertEquals(before, controller.interpolatedMultiplier(0.0));
        assertEquals(5.0, controller.activeDivisor());
    }

    @Test
    void sensitivityAndCinematicValuesAreTemporaryPureOutputs() {
        ZoomController controller = activeController();
        ZoomSettings enabled = new ZoomSettings(
                true, ZoomActivation.HOLD, 4.0, 16.0, 10.0, false, true, true
        );

        assertEquals(0.5 / 16.0, controller.effectiveSensitivity(0.5, enabled));
        assertTrue(controller.effectiveCinematicCamera(false, enabled));

        ZoomSettings untouched = new ZoomSettings(
                true, ZoomActivation.HOLD, 4.0, 16.0, 10.0, false, false, false
        );
        assertEquals(0.5, controller.effectiveSensitivity(0.5, untouched));
        assertFalse(controller.effectiveCinematicCamera(false, untouched));
        assertTrue(controller.effectiveCinematicCamera(true, untouched));
    }

    @Test
    void everyInvalidContextAndProfileResetSnapsToVanilla() {
        ZoomSettings settings = settings(ZoomActivation.HOLD, 4.0, 16.0, 1.0, false);
        for (ZoomState targetState : new ZoomState[]{ZoomState.ENTERING, ZoomState.ACTIVE, ZoomState.EXITING}) {
            ZoomController controller = new ZoomController();
            controller.updateInput(settings, true, false, true);
            if (targetState == ZoomState.ACTIVE) {
                for (int tick = 0; tick < 10; tick++) {
                    controller.tick();
                }
            } else if (targetState == ZoomState.EXITING) {
                controller.updateInput(settings, false, false, true);
            }
            assertEquals(targetState, controller.state());

            controller.updateInput(settings, false, false, false);

            assertEquals(ZoomState.INACTIVE, controller.state());
            assertEquals(1.0, controller.interpolatedMultiplier(1.0));
            assertFalse(controller.desiredActive());
        }
    }

    @Test
    void disabledFeatureSnapsAndStopsConsumingScroll() {
        ZoomController controller = activeController();
        ZoomSettings disabled = new ZoomSettings(
                false, ZoomActivation.HOLD, 4.0, 16.0, 10.0, true, true, false
        );

        controller.updateInput(disabled, true, true, true);

        assertEquals(ZoomState.INACTIVE, controller.state());
        assertFalse(controller.handleScroll(disabled, 1.0, true));
    }

    private static ZoomController activeController() {
        ZoomController controller = new ZoomController();
        ZoomSettings settings = settings(ZoomActivation.HOLD, 4.0, 16.0, 10.0, true);
        controller.updateInput(settings, true, false, true);
        controller.tick();
        return controller;
    }

    private static ZoomSettings settings(
            ZoomActivation activation,
            double divisor,
            double maxDivisor,
            double speed,
            boolean scroll
    ) {
        return new ZoomSettings(true, activation, divisor, maxDivisor, speed, scroll, true, false);
    }
}
