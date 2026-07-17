package net.easecation.clientsettings.feature.fullbright;

import net.easecation.clientsettings.profile.model.FullbrightMode;
import net.easecation.clientsettings.profile.model.FullbrightSettings;

public final class FullbrightController {

    // The shader clamps after applying gamma; 16 is enough to saturate dim nonzero light without unbounded input.
    public static final float GAMMA_CEILING = 16.0F;

    private FullbrightController() {
    }

    public static float effectiveGamma(float vanilla, FullbrightSettings settings) {
        if (!Float.isFinite(vanilla)
                || settings.mode() != FullbrightMode.GAMMA
                || settings.strength() == 0.0) {
            return vanilla;
        }
        float ceiling = Math.max(vanilla, GAMMA_CEILING);
        float adjusted = (float) (vanilla + (ceiling - vanilla) * settings.strength());
        return Float.isFinite(adjusted) ? adjusted : vanilla;
    }

    public static float effectiveNightVision(float vanilla, FullbrightSettings settings) {
        if (!Float.isFinite(vanilla)
                || settings.mode() != FullbrightMode.NIGHT_VISION
                || settings.strength() == 0.0) {
            return vanilla;
        }
        return Math.max(vanilla, (float) settings.strength());
    }
}
