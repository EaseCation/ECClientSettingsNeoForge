package net.easecation.clientsettings.client;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.api.Requirement;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.easecation.clientsettings.config.ClientSettingsConfig;
import org.oryxel.viabedrockutility.config.HardwareProfile;
import org.oryxel.viabedrockutility.config.LodConfig;
import org.oryxel.viabedrockutility.config.LodDetailedSettings;

import java.util.Locale;

final class ViaBedrockOptimizationCategory {
    private ViaBedrockOptimizationCategory() {
    }

    static void add(ConfigBuilder builder, ConfigEntryBuilder entries, ViaBedrockOptimizationDraft draft) {
        ConfigCategory category = builder.getOrCreateCategory(
                Component.translatable("category.ecclientsettings.performance")
        );
        category.addEntry(entries.startTextDescription(Component.translatable(
                "option.ecclientsettings.performance.description"
        )).build());

        HardwareProfile hardware = draft.hardwareProfile();
        category.addEntry(entries.startTextDescription(Component.translatable(
                "option.ecclientsettings.performance.current",
                presetName(draft.effectivePreset())
        ).withStyle(ChatFormatting.GREEN)).build());
        category.addEntry(entries.startTextDescription(Component.translatable(
                "option.ecclientsettings.performance.hardware",
                hardware.cpuName(),
                hardware.physicalCores(),
                hardware.gpuName(),
                hardware.performanceScore()
        ).withStyle(ChatFormatting.GRAY)).build());

        var modeEntry = entries.startEnumSelector(
                        Component.translatable("option.ecclientsettings.performance.mode"),
                        LodConfig.OptimizationMode.class,
                        draft.mode()
                )
                .setDefaultValue(LodConfig.OptimizationMode.AUTO)
                .setEnumNameProvider(mode -> Component.translatable(
                        "option.ecclientsettings.performance.mode." + mode.name().toLowerCase(Locale.ROOT)
                ))
                .setTooltip(Component.translatable("option.ecclientsettings.performance.mode.tooltip"))
                .setSaveConsumer(draft::setMode)
                .build();
        category.addEntry(modeEntry);

        var presetEntry = entries.startEnumSelector(
                        Component.translatable("option.ecclientsettings.performance.preset"),
                        LodConfig.Preset.class,
                        draft.manualPreset()
                )
                .setDefaultValue(LodConfig.Preset.BALANCED)
                .setEnumNameProvider(preset -> presetName((LodConfig.Preset) preset))
                .setTooltip(Component.translatable("option.ecclientsettings.performance.preset.tooltip"))
                .setRequirement(Requirement.isValue(modeEntry, LodConfig.OptimizationMode.MANUAL))
                .setSaveConsumer(draft::setManualPreset)
                .build();
        category.addEntry(presetEntry);

        category.addEntry(entries.startTextDescription(Component.translatable(
                "option.ecclientsettings.performance.auto_result",
                presetName(draft.automaticPreset())
        ).withStyle(ChatFormatting.GRAY)).build());
        MutableComponent renderDistanceStatus = ClientSettingsConfig.renderDistanceAutoPending()
                ? Component.translatable(
                        "option.ecclientsettings.performance.render_distance_pending",
                        hardware.recommendedInitialRenderDistance())
                : Component.translatable("option.ecclientsettings.performance.render_distance_initialized");
        category.addEntry(entries.startTextDescription(
                renderDistanceStatus.withStyle(ChatFormatting.GRAY)).build());

        Requirement custom = Requirement.all(
                Requirement.isValue(modeEntry, LodConfig.OptimizationMode.MANUAL),
                Requirement.isValue(presetEntry, LodConfig.Preset.CUSTOM)
        );
        addAnimationSettings(category, entries, draft, custom);
        addFrozenMeshSettings(category, entries, draft, custom);
        addVisibilitySettings(category, entries, draft, custom);
        addParticleSettings(category, entries, draft, custom);
    }

    private static void addAnimationSettings(
            ConfigCategory category,
            ConfigEntryBuilder entries,
            ViaBedrockOptimizationDraft draft,
            Requirement custom
    ) {
        var group = entries.startSubCategory(Component.translatable(
                "option.ecclientsettings.performance.group.animation"
        )).setExpanded(false);
        group.add(entries.startTextDescription(Component.translatable(
                "option.ecclientsettings.performance.animation.description"
        )).build());
        group.add(distance(entries, "tier1_distance", draft.tier1Distance(), 20.0,
                draft::setTier1Distance, custom));
        group.add(interval(entries, "tier1_interval", draft.tier1FrameInterval(), 2,
                draft::setTier1FrameInterval, custom));
        group.add(distance(entries, "tier2_distance", draft.tier2Distance(), 40.0,
                draft::setTier2Distance, custom));
        group.add(interval(entries, "tier2_interval", draft.tier2FrameInterval(), 4,
                draft::setTier2FrameInterval, custom));
        group.add(distance(entries, "tier3_distance", draft.tier3Distance(), 64.0,
                draft::setTier3Distance, custom));
        group.add(interval(entries, "tier3_interval", draft.tier3FrameInterval(), 6,
                draft::setTier3FrameInterval, custom));
        group.add(entries.startIntField(label("max_entities"), draft.maxAnimatedEntitiesPerFrame())
                .setDefaultValue(32)
                .setMin(0).setMax(512)
                .setTooltip(tooltip("max_entities"))
                .setRequirement(custom)
                .setSaveConsumer(draft::setMaxAnimatedEntitiesPerFrame)
                .build());
        group.add(entries.startIntField(label("max_players"), draft.maxAnimatedPlayersPerFrame())
                .setDefaultValue(32)
                .setMin(0).setMax(512)
                .setTooltip(tooltip("max_players"))
                .setRequirement(custom)
                .setSaveConsumer(draft::setMaxAnimatedPlayersPerFrame)
                .build());
        group.add(entries.startIntField(label("budget_interval"), draft.animationThrottleInterval())
                .setDefaultValue(3)
                .setMin(1).setMax(60)
                .setTooltip(tooltip("budget_interval"))
                .setRequirement(custom)
                .setSaveConsumer(draft::setAnimationThrottleInterval)
                .build());
        category.addEntry(group.build());
    }

    private static void addFrozenMeshSettings(
            ConfigCategory category,
            ConfigEntryBuilder entries,
            ViaBedrockOptimizationDraft draft,
            Requirement custom
    ) {
        var group = entries.startSubCategory(Component.translatable(
                "option.ecclientsettings.performance.group.frozen_mesh"
        )).setExpanded(false);
        group.add(entries.startTextDescription(Component.translatable(
                "option.ecclientsettings.performance.frozen_mesh.description"
        ).withStyle(ChatFormatting.GOLD)).build());
        var enabledEntry = entries.startBooleanToggle(label("frozen_enabled"), draft.frozenMeshEnabled())
                .setDefaultValue(true)
                .setTooltip(tooltip("frozen_enabled"))
                .setRequirement(custom)
                .setSaveConsumer(draft::setFrozenMeshEnabled)
                .build();
        group.add(enabledEntry);
        Requirement frozen = Requirement.all(custom, Requirement.isTrue(enabledEntry));
        group.add(entries.startDoubleField(label("frozen_enter"), draft.frozenMeshEnterDistance())
                .setDefaultValue(24.0)
                .setMin(12.0).setMax(128.0)
                .setTooltip(tooltip("frozen_enter"))
                .setRequirement(frozen)
                .setSaveConsumer(draft::setFrozenMeshEnterDistance)
                .build());
        group.add(entries.startDoubleField(label("frozen_exit"), draft.frozenMeshExitDistance())
                .setDefaultValue(20.0)
                .setMin(6.0).setMax(127.0)
                .setTooltip(tooltip("frozen_exit"))
                .setRequirement(frozen)
                .setSaveConsumer(draft::setFrozenMeshExitDistance)
                .build());
        group.add(entries.startIntField(label("frozen_gpu_mib"), draft.frozenMeshMaxGpuMiB())
                .setDefaultValue(128)
                .setMin(16).setMax(512)
                .setTooltip(tooltip("frozen_gpu_mib"))
                .setRequirement(frozen)
                .setSaveConsumer(draft::setFrozenMeshMaxGpuMiB)
                .build());
        category.addEntry(group.build());
    }

    private static void addVisibilitySettings(
            ConfigCategory category,
            ConfigEntryBuilder entries,
            ViaBedrockOptimizationDraft draft,
            Requirement custom
    ) {
        var group = entries.startSubCategory(Component.translatable(
                "option.ecclientsettings.performance.group.visibility"
        )).setExpanded(false);
        group.add(entries.startDoubleField(label("render_cull"), draft.renderCullDistance())
                .setDefaultValue(64.0)
                .setMin(0.0).setMax(512.0)
                .setTooltip(tooltip("render_cull"))
                .setRequirement(custom)
                .setSaveConsumer(draft::setRenderCullDistance)
                .build());
        group.add(entries.startDoubleField(label("text_cull"), draft.textDisplayCullDistance())
                .setDefaultValue(80.0)
                .setMin(0.0).setMax(512.0)
                .setTooltip(tooltip("text_cull"))
                .setRequirement(custom)
                .setSaveConsumer(draft::setTextDisplayCullDistance)
                .build());
        category.addEntry(group.build());
    }

    private static void addParticleSettings(
            ConfigCategory category,
            ConfigEntryBuilder entries,
            ViaBedrockOptimizationDraft draft,
            Requirement custom
    ) {
        var group = entries.startSubCategory(Component.translatable(
                "option.ecclientsettings.performance.group.particles"
        )).setExpanded(false);
        var enabledEntry = entries.startBooleanToggle(label("particle_enabled"), draft.particleTickLodEnabled())
                .setDefaultValue(true)
                .setTooltip(tooltip("particle_enabled"))
                .setRequirement(custom)
                .setSaveConsumer(draft::setParticleTickLodEnabled)
                .build();
        group.add(enabledEntry);
        Requirement particles = Requirement.all(custom, Requirement.isTrue(enabledEntry));
        group.add(entries.startIntField(label("particle_near"), draft.particleTickLodNearDistance())
                .setDefaultValue(20)
                .setMin(1).setMax(256)
                .setTooltip(tooltip("particle_near"))
                .setRequirement(particles)
                .setSaveConsumer(draft::setParticleTickLodNearDistance)
                .build());
        group.add(entries.startIntField(label("particle_far"), draft.particleTickLodFarDistance())
                .setDefaultValue(40)
                .setMin(1).setMax(256)
                .setTooltip(tooltip("particle_far"))
                .setRequirement(particles)
                .setSaveConsumer(draft::setParticleTickLodFarDistance)
                .build());
        category.addEntry(group.build());
    }

    private static me.shedaniel.clothconfig2.api.AbstractConfigListEntry<?> distance(
            ConfigEntryBuilder entries,
            String key,
            double value,
            double defaultValue,
            java.util.function.Consumer<Double> consumer,
            Requirement requirement
    ) {
        return entries.startDoubleField(label(key), value)
                .setDefaultValue(defaultValue)
                .setMin(0.0).setMax(512.0)
                .setTooltip(tooltip(key))
                .setRequirement(requirement)
                .setSaveConsumer(consumer)
                .build();
    }

    private static me.shedaniel.clothconfig2.api.AbstractConfigListEntry<?> interval(
            ConfigEntryBuilder entries,
            String key,
            int value,
            int defaultValue,
            java.util.function.Consumer<Integer> consumer,
            Requirement requirement
    ) {
        return entries.startIntField(label(key), value)
                .setDefaultValue(defaultValue)
                .setMin(1).setMax(60)
                .setTooltip(tooltip(key))
                .setRequirement(requirement)
                .setSaveConsumer(consumer)
                .build();
    }

    private static Component label(String key) {
        return Component.translatable("option.ecclientsettings.performance." + key);
    }

    private static Component tooltip(String key) {
        return Component.translatable("option.ecclientsettings.performance." + key + ".tooltip");
    }

    private static Component presetName(LodConfig.Preset preset) {
        return Component.translatable(
                "option.ecclientsettings.performance.preset." + preset.name().toLowerCase(Locale.ROOT)
        );
    }
}
