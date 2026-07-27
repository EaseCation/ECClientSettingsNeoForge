package net.easecation.clientsettings.client;

import org.oryxel.viabedrockutility.config.HardwareProfile;
import org.oryxel.viabedrockutility.config.LodConfig;
import org.oryxel.viabedrockutility.config.LodDetailedSettings;

final class ViaBedrockOptimizationDraft {
    private final LodConfig config;
    private LodConfig.OptimizationMode mode;
    private LodConfig.Preset manualPreset;
    private double tier1Distance;
    private int tier1FrameInterval;
    private double tier2Distance;
    private int tier2FrameInterval;
    private double tier3Distance;
    private int tier3FrameInterval;
    private double renderCullDistance;
    private double textDisplayCullDistance;
    private int maxAnimatedEntitiesPerFrame;
    private int maxAnimatedPlayersPerFrame;
    private int animationThrottleInterval;
    private boolean frozenMeshEnabled;
    private double frozenMeshEnterDistance;
    private double frozenMeshExitDistance;
    private int frozenMeshMaxGpuMiB;
    private boolean particleTickLodEnabled;
    private int particleTickLodNearDistance;
    private int particleTickLodFarDistance;

    private ViaBedrockOptimizationDraft(LodConfig config) {
        this.config = config;
        this.mode = config.getOptimizationMode();
        this.manualPreset = config.getManualPreset();
        LodDetailedSettings details = config.getCustomSettings();
        tier1Distance = details.tier1Distance();
        tier1FrameInterval = details.tier1FrameInterval();
        tier2Distance = details.tier2Distance();
        tier2FrameInterval = details.tier2FrameInterval();
        tier3Distance = details.tier3Distance();
        tier3FrameInterval = details.tier3FrameInterval();
        renderCullDistance = details.renderCullDistance();
        textDisplayCullDistance = details.textDisplayCullDistance();
        maxAnimatedEntitiesPerFrame = details.maxAnimatedEntitiesPerFrame();
        maxAnimatedPlayersPerFrame = details.maxAnimatedPlayersPerFrame();
        animationThrottleInterval = details.animationThrottleInterval();
        frozenMeshEnabled = details.frozenMeshEnabled();
        frozenMeshEnterDistance = details.frozenMeshEnterDistance();
        frozenMeshExitDistance = details.frozenMeshExitDistance();
        frozenMeshMaxGpuMiB = (int) (details.frozenMeshMaxGpuBytes() / LodDetailedSettings.MIB);
        particleTickLodEnabled = details.particleTickLodEnabled();
        particleTickLodNearDistance = details.particleTickLodNearDistance();
        particleTickLodFarDistance = details.particleTickLodFarDistance();
    }

    static ViaBedrockOptimizationDraft current() {
        return new ViaBedrockOptimizationDraft(LodConfig.getInstance());
    }

    LodConfig.OptimizationMode mode() {
        return mode;
    }

    void setMode(LodConfig.OptimizationMode mode) {
        this.mode = mode;
    }

    LodConfig.Preset manualPreset() {
        return manualPreset;
    }

    void setManualPreset(LodConfig.Preset manualPreset) {
        this.manualPreset = manualPreset;
    }

    LodConfig.Preset effectivePreset() {
        return config.getPreset();
    }

    LodConfig.Preset automaticPreset() {
        return config.getAutomaticPreset();
    }

    HardwareProfile hardwareProfile() {
        return config.getHardwareProfile();
    }

    double tier1Distance() { return tier1Distance; }
    void setTier1Distance(double value) { tier1Distance = value; }
    int tier1FrameInterval() { return tier1FrameInterval; }
    void setTier1FrameInterval(int value) { tier1FrameInterval = value; }
    double tier2Distance() { return tier2Distance; }
    void setTier2Distance(double value) { tier2Distance = value; }
    int tier2FrameInterval() { return tier2FrameInterval; }
    void setTier2FrameInterval(int value) { tier2FrameInterval = value; }
    double tier3Distance() { return tier3Distance; }
    void setTier3Distance(double value) { tier3Distance = value; }
    int tier3FrameInterval() { return tier3FrameInterval; }
    void setTier3FrameInterval(int value) { tier3FrameInterval = value; }
    double renderCullDistance() { return renderCullDistance; }
    void setRenderCullDistance(double value) { renderCullDistance = value; }
    double textDisplayCullDistance() { return textDisplayCullDistance; }
    void setTextDisplayCullDistance(double value) { textDisplayCullDistance = value; }
    int maxAnimatedEntitiesPerFrame() { return maxAnimatedEntitiesPerFrame; }
    void setMaxAnimatedEntitiesPerFrame(int value) { maxAnimatedEntitiesPerFrame = value; }
    int maxAnimatedPlayersPerFrame() { return maxAnimatedPlayersPerFrame; }
    void setMaxAnimatedPlayersPerFrame(int value) { maxAnimatedPlayersPerFrame = value; }
    int animationThrottleInterval() { return animationThrottleInterval; }
    void setAnimationThrottleInterval(int value) { animationThrottleInterval = value; }
    boolean frozenMeshEnabled() { return frozenMeshEnabled; }
    void setFrozenMeshEnabled(boolean value) { frozenMeshEnabled = value; }
    double frozenMeshEnterDistance() { return frozenMeshEnterDistance; }
    void setFrozenMeshEnterDistance(double value) { frozenMeshEnterDistance = value; }
    double frozenMeshExitDistance() { return frozenMeshExitDistance; }
    void setFrozenMeshExitDistance(double value) { frozenMeshExitDistance = value; }
    int frozenMeshMaxGpuMiB() { return frozenMeshMaxGpuMiB; }
    void setFrozenMeshMaxGpuMiB(int value) { frozenMeshMaxGpuMiB = value; }
    boolean particleTickLodEnabled() { return particleTickLodEnabled; }
    void setParticleTickLodEnabled(boolean value) { particleTickLodEnabled = value; }
    int particleTickLodNearDistance() { return particleTickLodNearDistance; }
    void setParticleTickLodNearDistance(int value) { particleTickLodNearDistance = value; }
    int particleTickLodFarDistance() { return particleTickLodFarDistance; }
    void setParticleTickLodFarDistance(int value) { particleTickLodFarDistance = value; }

    void save() {
        LodDetailedSettings details = detailedSettings();
        if (mode == config.getOptimizationMode()
                && manualPreset == config.getManualPreset()
                && details.normalized().equals(config.getCustomSettings())) {
            return;
        }
        config.applySelectionAndSave(mode, manualPreset, details);
    }

    private LodDetailedSettings detailedSettings() {
        return new LodDetailedSettings(
                tier1Distance,
                tier1FrameInterval,
                tier2Distance,
                tier2FrameInterval,
                tier3Distance,
                tier3FrameInterval,
                renderCullDistance,
                textDisplayCullDistance,
                maxAnimatedEntitiesPerFrame,
                maxAnimatedPlayersPerFrame,
                animationThrottleInterval,
                frozenMeshEnabled,
                frozenMeshEnterDistance,
                frozenMeshExitDistance,
                frozenMeshMaxGpuMiB * LodDetailedSettings.MIB,
                particleTickLodEnabled,
                particleTickLodNearDistance,
                particleTickLodFarDistance
        );
    }
}
