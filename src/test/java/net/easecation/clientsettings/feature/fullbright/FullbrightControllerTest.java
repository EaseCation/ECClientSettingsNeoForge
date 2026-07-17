package net.easecation.clientsettings.feature.fullbright;

import net.easecation.clientsettings.profile.model.FullbrightMode;
import net.easecation.clientsettings.profile.model.FullbrightSettings;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FullbrightControllerTest {

    @Test
    void offAndZeroStrengthAreIdentityFunctions() {
        for (float vanilla : new float[]{-1.0F, 0.0F, 0.42F, 1.0F, 20.0F}) {
            FullbrightSettings off = new FullbrightSettings(FullbrightMode.OFF, 1.0);
            FullbrightSettings zeroGamma = new FullbrightSettings(FullbrightMode.GAMMA, 0.0);
            FullbrightSettings zeroVision = new FullbrightSettings(FullbrightMode.NIGHT_VISION, 0.0);
            assertEquals(vanilla, FullbrightController.effectiveGamma(vanilla, off));
            assertEquals(vanilla, FullbrightController.effectiveNightVision(vanilla, off));
            assertEquals(vanilla, FullbrightController.effectiveGamma(vanilla, zeroGamma));
            assertEquals(vanilla, FullbrightController.effectiveNightVision(vanilla, zeroVision));
        }
    }

    @Test
    void gammaInterpolationIsMonotonicBoundedAndNeverReducesVanilla() {
        float vanilla = 0.4F;
        float low = FullbrightController.effectiveGamma(
                vanilla,
                new FullbrightSettings(FullbrightMode.GAMMA, 0.25)
        );
        float high = FullbrightController.effectiveGamma(
                vanilla,
                new FullbrightSettings(FullbrightMode.GAMMA, 0.75)
        );

        assertTrue(low >= vanilla);
        assertTrue(high >= low);
        assertTrue(high <= FullbrightController.GAMMA_CEILING);
        assertEquals(20.0F, FullbrightController.effectiveGamma(
                20.0F,
                new FullbrightSettings(FullbrightMode.GAMMA, 1.0)
        ));
        float extreme = FullbrightController.effectiveGamma(
                -Float.MAX_VALUE,
                new FullbrightSettings(FullbrightMode.GAMMA, 1.0)
        );
        assertTrue(Float.isFinite(extreme));
        assertTrue(extreme >= -Float.MAX_VALUE);
    }

    @Test
    void nightVisionRaisesButNeverReducesVanillaBlend() {
        FullbrightSettings settings = new FullbrightSettings(FullbrightMode.NIGHT_VISION, 0.6);

        assertEquals(0.6F, FullbrightController.effectiveNightVision(0.2F, settings));
        assertEquals(0.8F, FullbrightController.effectiveNightVision(0.8F, settings));
    }

    @Test
    void modesAffectOnlyTheirOwnLightmapInput() {
        FullbrightSettings gamma = new FullbrightSettings(FullbrightMode.GAMMA, 1.0);
        FullbrightSettings vision = new FullbrightSettings(FullbrightMode.NIGHT_VISION, 1.0);

        assertEquals(0.3F, FullbrightController.effectiveNightVision(0.3F, gamma));
        assertEquals(0.3F, FullbrightController.effectiveGamma(0.3F, vision));
        assertEquals(Float.NaN, FullbrightController.effectiveGamma(Float.NaN, gamma));
        assertEquals(Float.POSITIVE_INFINITY, FullbrightController.effectiveNightVision(
                Float.POSITIVE_INFINITY,
                vision
        ));
    }
}
