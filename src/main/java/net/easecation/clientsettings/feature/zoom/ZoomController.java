package net.easecation.clientsettings.feature.zoom;

import net.easecation.clientsettings.profile.model.ZoomActivation;
import net.easecation.clientsettings.profile.model.ZoomSettings;

public final class ZoomController {

    private static final double TICKS_PER_SECOND = 20.0;
    private static final double SETTLE_EPSILON = 1.0E-4;
    private static final double SCROLL_STEP_EPSILON = 1.0E-9;

    private ZoomState state = ZoomState.INACTIVE;
    private boolean desiredActive;
    private double activeDivisor = ZoomSettings.DEFAULT.divisor();
    private double currentMultiplier = 1.0;
    private double targetMultiplier = 1.0;
    private double scrollAccumulator;

    public void updateInput(ZoomSettings settings, boolean keyDown, boolean clicked, boolean validContext) {
        if (!validContext || !settings.enabled()) {
            snapReset(settings);
            return;
        }
        syncDivisorBounds(settings);

        boolean requested = settings.activation() == ZoomActivation.HOLD
                ? keyDown
                : clicked ? !desiredActive : desiredActive;
        setDesiredActive(requested, settings);
    }

    public void advance(double deltaSeconds, ZoomSettings settings) {
        if (state != ZoomState.ENTERING && state != ZoomState.EXITING) {
            return;
        }
        currentMultiplier = advanceMultiplier(
                currentMultiplier,
                targetMultiplier,
                settings.animationSpeed(),
                deltaSeconds
        );
        if (Math.abs(currentMultiplier - targetMultiplier) <= SETTLE_EPSILON) {
            currentMultiplier = targetMultiplier;
            state = desiredActive ? ZoomState.ACTIVE : ZoomState.INACTIVE;
        }
    }

    public ScrollResult handleScroll(ZoomSettings settings, double deltaY, boolean validContext) {
        if (!validContext
                || !settings.enabled()
                || !settings.scrollAdjustment()
                || !desiredActive
                || deltaY == 0.0
                || !Double.isFinite(deltaY)) {
            return ScrollResult.PASS;
        }

        if (scrollAccumulator != 0.0 && Math.signum(scrollAccumulator) != Math.signum(deltaY)) {
            scrollAccumulator = 0.0;
        }
        scrollAccumulator += deltaY;
        int steps = wholeScrollSteps(scrollAccumulator);
        if (steps == 0) {
            return ScrollResult.CONSUMED_UNCHANGED;
        }
        scrollAccumulator -= steps;

        double adjusted = clamp(activeDivisor + steps, 1.0, settings.maxDivisor());
        if (adjusted == activeDivisor) {
            return ScrollResult.CONSUMED_UNCHANGED;
        }
        activeDivisor = adjusted;
        beginTransition(1.0 / activeDivisor, ZoomState.ENTERING);
        return ScrollResult.CONSUMED_CHANGED;
    }

    public double currentMultiplier() {
        return currentMultiplier;
    }

    public double effectiveSensitivity(double vanilla, ZoomSettings settings) {
        if (!shouldAffectView() || !settings.reduceSensitivity()) {
            return vanilla;
        }
        return vanilla * currentMultiplier * currentMultiplier;
    }

    public boolean effectiveCinematicCamera(boolean vanilla, ZoomSettings settings) {
        return vanilla || shouldAffectView() && settings.smoothCamera();
    }

    public void snapReset(ZoomSettings settings) {
        state = ZoomState.INACTIVE;
        desiredActive = false;
        activeDivisor = settings.divisor();
        currentMultiplier = 1.0;
        targetMultiplier = 1.0;
        scrollAccumulator = 0.0;
    }

    public boolean shouldAffectView() {
        return state != ZoomState.INACTIVE;
    }

    public ZoomState state() {
        return state;
    }

    public boolean desiredActive() {
        return desiredActive;
    }

    public double activeDivisor() {
        return activeDivisor;
    }

    public static double advanceMultiplier(double current, double target, double animationSpeed, double deltaSeconds) {
        if (!Double.isFinite(deltaSeconds) || deltaSeconds < 0.0 || current == target) {
            return current;
        }
        if (animationSpeed >= 10.0) {
            return target;
        }
        if (deltaSeconds == 0.0) {
            return current;
        }
        double retentionPerTick = 1.0 - animationSpeed / 10.0;
        double retention = Math.pow(retentionPerTick, deltaSeconds * TICKS_PER_SECOND);
        return target + (current - target) * retention;
    }

    private void setDesiredActive(boolean requested, ZoomSettings settings) {
        if (requested == desiredActive) {
            return;
        }
        desiredActive = requested;
        scrollAccumulator = 0.0;
        if (requested) {
            if (state == ZoomState.INACTIVE) {
                activeDivisor = settings.divisor();
            }
            beginTransition(1.0 / activeDivisor, ZoomState.ENTERING);
        } else {
            beginTransition(1.0, ZoomState.EXITING);
        }
    }

    private void syncDivisorBounds(ZoomSettings settings) {
        if (state == ZoomState.INACTIVE && !desiredActive) {
            activeDivisor = settings.divisor();
            return;
        }
        double adjusted = clamp(activeDivisor, 1.0, settings.maxDivisor());
        if (adjusted != activeDivisor) {
            activeDivisor = adjusted;
            beginTransition(1.0 / activeDivisor, ZoomState.ENTERING);
        }
    }

    private void beginTransition(double target, ZoomState transitionState) {
        targetMultiplier = target;
        if (Math.abs(currentMultiplier - targetMultiplier) <= SETTLE_EPSILON) {
            currentMultiplier = targetMultiplier;
            state = desiredActive ? ZoomState.ACTIVE : ZoomState.INACTIVE;
            return;
        }
        state = transitionState;
    }

    private static int wholeScrollSteps(double accumulated) {
        if (accumulated >= 1.0 - SCROLL_STEP_EPSILON) {
            return (int) Math.floor(accumulated + SCROLL_STEP_EPSILON);
        }
        if (accumulated <= -1.0 + SCROLL_STEP_EPSILON) {
            return (int) Math.ceil(accumulated - SCROLL_STEP_EPSILON);
        }
        return 0;
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    public enum ScrollResult {
        PASS(false, false),
        CONSUMED_UNCHANGED(true, false),
        CONSUMED_CHANGED(true, true);

        private final boolean consumed;
        private final boolean changed;

        ScrollResult(boolean consumed, boolean changed) {
            this.consumed = consumed;
            this.changed = changed;
        }

        public boolean consumed() {
            return consumed;
        }

        public boolean changed() {
            return changed;
        }
    }
}
