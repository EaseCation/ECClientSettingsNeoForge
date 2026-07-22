package net.easecation.clientsettings.feature.zoom;

import net.easecation.clientsettings.profile.model.ZoomActivation;
import net.easecation.clientsettings.profile.model.ZoomSettings;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ZoomControllerTest {

    private static final double EPSILON = 1.0E-9;

    @Test
    void holdModeEntersStaysActiveAndAnimatesOnRelease() {
        ZoomController controller = new ZoomController();
        ZoomSettings settings = settings(ZoomActivation.HOLD, 4.0, 16.0, 10.0, false);

        controller.updateInput(settings, true, true, true);
        assertEquals(ZoomState.ENTERING, controller.state());
        controller.advance(0.001, settings);
        assertEquals(ZoomState.ACTIVE, controller.state());
        assertEquals(0.25, controller.currentMultiplier(), EPSILON);

        controller.updateInput(settings, true, false, true);
        assertEquals(ZoomState.ACTIVE, controller.state());
        controller.updateInput(settings, false, false, true);
        assertEquals(ZoomState.EXITING, controller.state());
        controller.advance(0.001, settings);
        assertEquals(ZoomState.INACTIVE, controller.state());
        assertEquals(1.0, controller.currentMultiplier(), EPSILON);
    }

    @Test
    void toggleModeFlipsOnlyOnClicks() {
        ZoomController controller = new ZoomController();
        ZoomSettings settings = settings(ZoomActivation.TOGGLE, 4.0, 16.0, 10.0, false);

        controller.updateInput(settings, false, true, true);
        controller.advance(0.001, settings);
        assertTrue(controller.desiredActive());
        controller.updateInput(settings, false, false, true);
        assertTrue(controller.desiredActive());
        controller.updateInput(settings, false, true, true);
        assertFalse(controller.desiredActive());
    }

    @Test
    void animationMatchesAxolotlReferenceAtTwentyTicksPerSecond() {
        double firstTick = ZoomController.advanceMultiplier(1.0, 0.25, 7.5, 0.05);
        double secondTick = ZoomController.advanceMultiplier(firstTick, 0.25, 7.5, 0.05);

        assertEquals(0.4375, firstTick, EPSILON);
        assertEquals(0.296875, secondTick, EPSILON);
        assertEquals(0.25, ZoomController.advanceMultiplier(1.0, 0.25, 10.0, 0.0), EPSILON);
        assertEquals(1.0, ZoomController.advanceMultiplier(1.0, 0.25, 7.5, 0.0), EPSILON);
        assertEquals(1.0, ZoomController.advanceMultiplier(1.0, 0.25, 7.5, Double.NaN), EPSILON);
    }

    @Test
    void animationIsFrameRateIndependent() {
        double atTwentyFps = advanceForOneSecond(20);
        double atSixtyFps = advanceForOneSecond(60);
        double atOneHundredFortyFourFps = advanceForOneSecond(144);

        assertEquals(atTwentyFps, atSixtyFps, EPSILON);
        assertEquals(atTwentyFps, atOneHundredFortyFourFps, EPSILON);
    }

    @Test
    void highResolutionScrollAccumulatesAndBoundariesStayConsumed() {
        ZoomController controller = activeController();
        ZoomSettings settings = settings(ZoomActivation.HOLD, 4.0, 5.0, 10.0, true);

        for (int part = 0; part < 3; part++) {
            ZoomController.ScrollResult result = controller.handleScroll(settings, 0.25, true);
            assertTrue(result.consumed());
            assertFalse(result.changed());
        }
        ZoomController.ScrollResult changed = controller.handleScroll(settings, 0.25, true);
        assertTrue(changed.consumed());
        assertTrue(changed.changed());
        assertEquals(5.0, controller.activeDivisor(), EPSILON);

        ZoomController.ScrollResult boundary = controller.handleScroll(settings, 1.0, true);
        assertTrue(boundary.consumed());
        assertFalse(boundary.changed());
        assertEquals(5.0, controller.activeDivisor(), EPSILON);

        assertTrue(controller.handleScroll(settings, -1.0, true).changed());
        assertEquals(4.0, controller.activeDivisor(), EPSILON);
        assertFalse(controller.handleScroll(settings, Double.NaN, true).consumed());
        assertFalse(controller.handleScroll(settings, 1.0, false).consumed());
    }

    @Test
    void scrollDirectionChangeDropsTheOppositePartialStep() {
        ZoomController controller = activeController();
        ZoomSettings settings = settings(ZoomActivation.HOLD, 4.0, 16.0, 10.0, true);

        controller.handleScroll(settings, 0.75, true);
        controller.handleScroll(settings, -0.5, true);
        assertEquals(4.0, controller.activeDivisor(), EPSILON);
        assertTrue(controller.handleScroll(settings, -0.5, true).changed());
        assertEquals(3.0, controller.activeDivisor(), EPSILON);
    }

    @Test
    void divisorChangeStartsFromCurrentMultiplierWithoutJump() {
        ZoomController controller = new ZoomController();
        ZoomSettings settings = settings(ZoomActivation.HOLD, 4.0, 16.0, 7.5, true);
        controller.updateInput(settings, true, false, true);
        controller.advance(0.05, settings);
        double before = controller.currentMultiplier();

        assertTrue(controller.handleScroll(settings, 1.0, true).changed());

        assertEquals(before, controller.currentMultiplier(), EPSILON);
        assertEquals(5.0, controller.activeDivisor(), EPSILON);
    }

    @Test
    void sensitivityTracksTheVisibleAnimationMultiplier() {
        ZoomController controller = new ZoomController();
        ZoomSettings settings = new ZoomSettings(
                true, ZoomActivation.HOLD, 4.0, 16.0, 7.5, false, true, true
        );

        controller.updateInput(settings, true, false, true);
        assertEquals(0.5, controller.effectiveSensitivity(0.5, settings), EPSILON);
        controller.advance(0.05, settings);
        assertEquals(0.5 * 0.4375 * 0.4375, controller.effectiveSensitivity(0.5, settings), EPSILON);
        assertTrue(controller.effectiveCinematicCamera(false, settings));

        double beforeExit = controller.currentMultiplier();
        controller.updateInput(settings, false, false, true);
        controller.advance(0.05, settings);
        assertTrue(controller.currentMultiplier() > beforeExit);
        assertEquals(
                0.5 * controller.currentMultiplier() * controller.currentMultiplier(),
                controller.effectiveSensitivity(0.5, settings),
                EPSILON
        );
    }

    @Test
    void sensitivityAndCinematicOptionsCanRemainUntouched() {
        ZoomController controller = activeController();
        ZoomSettings untouched = new ZoomSettings(
                true, ZoomActivation.HOLD, 4.0, 16.0, 10.0, false, false, false
        );

        assertEquals(0.5, controller.effectiveSensitivity(0.5, untouched), EPSILON);
        assertFalse(controller.effectiveCinematicCamera(false, untouched));
        assertTrue(controller.effectiveCinematicCamera(true, untouched));
    }

    @Test
    void everyInvalidContextAndProfileResetSnapsToVanilla() {
        ZoomSettings settings = settings(ZoomActivation.HOLD, 4.0, 16.0, 7.5, false);
        for (ZoomState targetState : new ZoomState[]{ZoomState.ENTERING, ZoomState.ACTIVE, ZoomState.EXITING}) {
            ZoomController controller = new ZoomController();
            controller.updateInput(settings, true, false, true);
            if (targetState == ZoomState.ACTIVE) {
                controller.advance(1.0, settings);
            } else if (targetState == ZoomState.EXITING) {
                controller.advance(0.05, settings);
                controller.updateInput(settings, false, false, true);
            }
            assertEquals(targetState, controller.state());

            controller.updateInput(settings, false, false, false);

            assertEquals(ZoomState.INACTIVE, controller.state());
            assertEquals(1.0, controller.currentMultiplier(), EPSILON);
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
        assertFalse(controller.handleScroll(disabled, 1.0, true).consumed());
    }

    private static double advanceForOneSecond(int framesPerSecond) {
        double value = 1.0;
        for (int frame = 0; frame < framesPerSecond; frame++) {
            value = ZoomController.advanceMultiplier(value, 0.25, 7.5, 1.0 / framesPerSecond);
        }
        return value;
    }

    private static ZoomController activeController() {
        ZoomController controller = new ZoomController();
        ZoomSettings settings = settings(ZoomActivation.HOLD, 4.0, 16.0, 10.0, true);
        controller.updateInput(settings, true, false, true);
        controller.advance(0.001, settings);
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
