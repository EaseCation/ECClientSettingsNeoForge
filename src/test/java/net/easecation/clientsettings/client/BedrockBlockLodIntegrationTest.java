package net.easecation.clientsettings.client;

import org.junit.jupiter.api.Test;
import org.oryxel.viabedrockutility.config.LodConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BedrockBlockLodIntegrationTest {
    private static final BedrockBlockLodIntegration.Policy CUSTOM =
            new BedrockBlockLodIntegration.Policy(true, 7, 9, 2);

    @Test
    void standardPresetsUseDistinctBlockModelDistances() {
        var quality = BedrockBlockLodIntegration.policyForPreset(LodConfig.Preset.HIGH_QUALITY, CUSTOM);
        var balanced = BedrockBlockLodIntegration.policyForPreset(LodConfig.Preset.BALANCED, CUSTOM);
        var performance = BedrockBlockLodIntegration.policyForPreset(LodConfig.Preset.PERFORMANCE, CUSTOM);
        var extreme = BedrockBlockLodIntegration.policyForPreset(LodConfig.Preset.EXTREME, CUSTOM);

        assertFalse(quality.enabled());
        assertEquals(new BedrockBlockLodIntegration.Policy(true, 4, 6, 1), balanced);
        assertEquals(new BedrockBlockLodIntegration.Policy(true, 2, 4, 1), performance);
        assertEquals(new BedrockBlockLodIntegration.Policy(true, 1, 3, 1), extreme);
    }

    @Test
    void onlyCustomPresetKeepsAdvancedValuesAndNormalizesInvalidInput() {
        assertEquals(CUSTOM, BedrockBlockLodIntegration.policyForPreset(LodConfig.Preset.CUSTOM, CUSTOM));

        var invalid = new BedrockBlockLodIntegration.Policy(true, 6, 6, 0);
        var normalized = BedrockBlockLodIntegration.policyForPreset(LodConfig.Preset.CUSTOM, invalid);
        assertTrue(normalized.enabled());
        assertEquals(4, normalized.highDistanceSections());
        assertEquals(6, normalized.lowDistanceSections());
        assertEquals(1, normalized.maxRebuildsPerFrame());
    }

    @Test
    void automaticModeUsesHardwareRecommendationWhileManualKeepsSelection() {
        assertEquals(
                LodConfig.Preset.PERFORMANCE,
                BedrockBlockLodIntegration.selectedPreset(
                        LodConfig.OptimizationMode.AUTO,
                        LodConfig.Preset.CUSTOM,
                        LodConfig.Preset.PERFORMANCE
                )
        );
        assertEquals(
                LodConfig.Preset.CUSTOM,
                BedrockBlockLodIntegration.selectedPreset(
                        LodConfig.OptimizationMode.MANUAL,
                        LodConfig.Preset.CUSTOM,
                        LodConfig.Preset.PERFORMANCE
                )
        );
    }
}
