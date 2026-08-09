package net.easecation.clientsettings.client;

import net.easecation.clientsettings.ECClientSettings;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import org.oryxel.viabedrockutility.config.LodConfig;

import java.lang.reflect.Method;
import java.util.Map;

/** Optional reflection integration; ECClientSettings never links against BedrockLoader classes. */
public final class BedrockBlockLodIntegration {
    private static final String BRIDGE_CLASS =
            "net.easecation.bedrockloader.integration.BlockModelLodConfigBridge";

    private BedrockBlockLodIntegration() {
    }

    record Policy(boolean enabled, int highDistanceSections, int lowDistanceSections, int maxRebuildsPerFrame) {
        Policy normalized() {
            if (highDistanceSections >= 0
                    && lowDistanceSections > highDistanceSections
                    && maxRebuildsPerFrame > 0) {
                return this;
            }
            return new Policy(enabled, 4, 6, 1);
        }
    }

    record Snapshot(
            boolean available,
            boolean enabled,
            int highDistanceSections,
            int lowDistanceSections,
            int maxRebuildsPerFrame,
            boolean active,
            String adapterStatus
    ) {
        Policy policy() {
            return new Policy(enabled, highDistanceSections, lowDistanceSections, maxRebuildsPerFrame).normalized();
        }

        static Snapshot unavailable(String status) {
            return new Snapshot(false, false, 4, 6, 1, false, status);
        }
    }

    static Policy policyForPreset(LodConfig.Preset preset, Policy custom) {
        return switch (preset) {
            case HIGH_QUALITY -> new Policy(false, 4, 6, 1);
            case BALANCED -> new Policy(true, 4, 6, 1);
            case PERFORMANCE -> new Policy(true, 2, 4, 1);
            case EXTREME -> new Policy(true, 1, 3, 1);
            case CUSTOM -> custom.normalized();
        };
    }

    static LodConfig.Preset selectedPreset(
            LodConfig.OptimizationMode mode,
            LodConfig.Preset manualPreset,
            LodConfig.Preset automaticPreset
    ) {
        return mode == LodConfig.OptimizationMode.AUTO ? automaticPreset : manualPreset;
    }

    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(BedrockBlockLodIntegration::synchronizeCurrentPreset);
    }

    static void synchronizeCurrentPreset() {
        LodConfig config = LodConfig.getInstance();
        Snapshot current = snapshot();
        if (!current.available()) {
            ECClientSettings.LOGGER.info("Bedrock block-model LOD integration unavailable: {}", current.adapterStatus());
            return;
        }
        LodConfig.Preset preset = selectedPreset(
                config.getOptimizationMode(),
                config.getManualPreset(),
                config.getAutomaticPreset()
        );
        apply(policyForPreset(preset, current.policy()), "startup preset " + preset);
    }

    static Snapshot snapshot() {
        try {
            Method snapshot = Class.forName(BRIDGE_CLASS).getMethod("snapshot");
            Object result = snapshot.invoke(null);
            if (!(result instanceof Map<?, ?> values)) {
                return Snapshot.unavailable("BedrockLoader bridge returned an invalid snapshot");
            }
            return new Snapshot(
                    true,
                    booleanValue(values, "enabled", false),
                    intValue(values, "highDistanceSections", 4),
                    intValue(values, "lowDistanceSections", 6),
                    intValue(values, "maxRebuildsPerFrame", 1),
                    booleanValue(values, "active", false),
                    stringValue(values, "adapterStatus", "Adapter status unavailable")
            );
        } catch (ClassNotFoundException absent) {
            return Snapshot.unavailable("BedrockLoader is not installed");
        } catch (ReflectiveOperationException | LinkageError | RuntimeException failure) {
            ECClientSettings.LOGGER.warn("Could not read Bedrock block-model LOD settings", failure);
            return Snapshot.unavailable("BedrockLoader bridge failed: " + failure.getClass().getSimpleName());
        }
    }

    static boolean apply(Policy rawPolicy, String source) {
        Policy policy = rawPolicy.normalized();
        try {
            Method apply = Class.forName(BRIDGE_CLASS).getMethod(
                    "apply", boolean.class, int.class, int.class, int.class);
            apply.invoke(
                    null,
                    policy.enabled(),
                    policy.highDistanceSections(),
                    policy.lowDistanceSections(),
                    policy.maxRebuildsPerFrame()
            );
            ECClientSettings.LOGGER.info(
                    "Applied Bedrock block-model LOD from {}: enabled={}, HIGH<={}, LOW>={}, rebuilds/frame={}",
                    source,
                    policy.enabled(),
                    policy.highDistanceSections(),
                    policy.lowDistanceSections(),
                    policy.maxRebuildsPerFrame()
            );
            return true;
        } catch (ClassNotFoundException absent) {
            return false;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException failure) {
            ECClientSettings.LOGGER.warn("Could not apply Bedrock block-model LOD settings from " + source, failure);
            return false;
        }
    }

    private static boolean booleanValue(Map<?, ?> values, String key, boolean fallback) {
        Object value = values.get(key);
        return value instanceof Boolean bool ? bool : fallback;
    }

    private static int intValue(Map<?, ?> values, String key, int fallback) {
        Object value = values.get(key);
        return value instanceof Number number ? number.intValue() : fallback;
    }

    private static String stringValue(Map<?, ?> values, String key, String fallback) {
        Object value = values.get(key);
        return value instanceof String string && !string.isBlank() ? string : fallback;
    }
}
