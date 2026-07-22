package net.easecation.clientsettings.feature.zoom;

import net.easecation.clientsettings.profile.model.ZoomActivation;
import net.easecation.clientsettings.profile.model.ZoomSettings;

public final class ZoomController {

    private ZoomState state = ZoomState.INACTIVE;
    private boolean desiredActive;
    private double activeDivisor = ZoomSettings.DEFAULT.divisor();
    private double previousMultiplier = 1.0;
    private double currentMultiplier = 1.0;
    private double transitionStart = 1.0;
    private double transitionTarget = 1.0;
    private int transitionTick;
    private int transitionDuration = 1;

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

    public void tick() {
        previousMultiplier = currentMultiplier;
        if (state != ZoomState.ENTERING && state != ZoomState.EXITING) {
            return;
        }
        transitionTick++;
        double progress = Math.min(1.0, (double) transitionTick / transitionDuration);
        double eased = smoothstep(progress);
        currentMultiplier = lerp(transitionStart, transitionTarget, eased);
        if (progress >= 1.0) {
            currentMultiplier = transitionTarget;
            previousMultiplier = currentMultiplier;
            state = desiredActive ? ZoomState.ACTIVE : ZoomState.INACTIVE;
        }
    }

    public boolean handleScroll(ZoomSettings settings, double deltaY, boolean validContext) {
        if (!validContext
                || !settings.enabled()
                || !settings.scrollAdjustment()
                || !desiredActive
                || deltaY == 0.0
                || !Double.isFinite(deltaY)) {
            return false;
        }
        double adjusted = clamp(activeDivisor + Math.signum(deltaY), 1.0, settings.maxDivisor());
        if (adjusted == activeDivisor) {
            return false;
        }
        activeDivisor = adjusted;
        beginTransition(1.0 / activeDivisor, ZoomState.ENTERING, settings.animationSpeed());
        return true;
    }

    public double interpolatedMultiplier(double partialTick) {
        double clampedPartialTick = clamp(partialTick, 0.0, 1.0);
        return lerp(previousMultiplier, currentMultiplier, clampedPartialTick);
    }

    public double effectiveSensitivity(double vanilla, ZoomSettings settings) {
        if (!shouldAffectView() || !settings.reduceSensitivity()) {
            return vanilla;
        }
        return vanilla / (activeDivisor * activeDivisor);
    }

    public boolean effectiveCinematicCamera(boolean vanilla, ZoomSettings settings) {
        return vanilla || shouldAffectView() && settings.smoothCamera();
    }

    public void snapReset(ZoomSettings settings) {
        state = ZoomState.INACTIVE;
        desiredActive = false;
        activeDivisor = settings.divisor();
        previousMultiplier = 1.0;
        currentMultiplier = 1.0;
        transitionStart = 1.0;
        transitionTarget = 1.0;
        transitionTick = 0;
        transitionDuration = 1;
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

    public static int transitionDuration(double animationSpeed) {
        return Math.max(1, (int) Math.ceil(11.0 - animationSpeed));
    }

    public static double smoothstep(double value) {
        double clamped = clamp(value, 0.0, 1.0);
        return clamped * clamped * (3.0 - 2.0 * clamped);
    }

    private void setDesiredActive(boolean requested, ZoomSettings settings) {
        if (requested == desiredActive) {
            return;
        }
        desiredActive = requested;
        if (requested) {
            if (state == ZoomState.INACTIVE) {
                activeDivisor = settings.divisor();
            }
            beginTransition(1.0 / activeDivisor, ZoomState.ENTERING, settings.animationSpeed());
        } else {
            beginTransition(1.0, ZoomState.EXITING, settings.animationSpeed());
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
            beginTransition(1.0 / activeDivisor, ZoomState.ENTERING, settings.animationSpeed());
        }
    }

    private void beginTransition(double target, ZoomState transitionState, double animationSpeed) {
        transitionStart = currentMultiplier;
        transitionTarget = target;
        transitionTick = 0;
        transitionDuration = transitionDuration(animationSpeed);
        previousMultiplier = currentMultiplier;
        state = transitionState;
    }

    private static double lerp(double start, double end, double progress) {
        return start + (end - start) * progress;
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
