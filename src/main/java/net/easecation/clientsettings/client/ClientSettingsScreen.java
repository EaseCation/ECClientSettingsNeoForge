package net.easecation.clientsettings.client;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.easecation.clientsettings.ECClientSettings;
import net.easecation.clientsettings.config.ClientSettingsConfig;
import net.easecation.clientsettings.profile.model.BlockOutlineSettings;
import net.easecation.clientsettings.profile.model.LowFireSettings;
import net.easecation.clientsettings.profile.model.FullbrightMode;
import net.easecation.clientsettings.profile.model.FullbrightSettings;
import net.easecation.clientsettings.profile.model.HitColorSettings;
import net.easecation.clientsettings.profile.model.TimeChangerMode;
import net.easecation.clientsettings.profile.model.TimeChangerSettings;
import net.easecation.clientsettings.profile.model.ZoomActivation;
import net.easecation.clientsettings.profile.model.ZoomSettings;
import net.easecation.clientsettings.profile.runtime.ProfileManager;
import net.easecation.clientsettings.profile.runtime.ProfileServices;
import net.easecation.clientsettings.window.WindowAppearanceController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.util.Locale;

public final class ClientSettingsScreen {

    private ClientSettingsScreen() {
    }

    public static Screen create(Screen parent) {
        ProfileManager profiles = ProfileServices.manager();
        return create(
                parent,
                ProfileSettingsDraft.active(profiles),
                ClientSettingsConfig.allowServerWindowTitle(),
                ClientSettingsConfig.allowServerWindowFrame(),
                null
        );
    }

    private static Screen create(
            Screen parent,
            ProfileSettingsDraft draft,
            boolean initialAllowTitle,
            boolean initialAllowFrame,
            Component saveError
    ) {
        ProfileManager profiles = ProfileServices.manager();
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("screen.ecclientsettings.title"));
        ConfigEntryBuilder entries = builder.entryBuilder();
        boolean[] allowTitle = {initialAllowTitle};
        boolean[] allowFrame = {initialAllowFrame};

        addProfileCategory(builder, entries, parent, profiles, draft, saveError);
        addMovementCategory(builder, entries, draft);
        addCombatCategory(builder, entries);
        addRenderingCategory(builder, entries, draft);
        addServerPermissionsCategory(builder, entries, allowTitle, allowFrame);

        builder.setSavingRunnable(() -> {
            try {
                draft.save(profiles);
                ClientSettingsConfig.setServerWindowPermissions(allowTitle[0], allowFrame[0]);
                WindowAppearanceController.getInstance().reconcilePermissions();
            } catch (IOException | RuntimeException exception) {
                ECClientSettings.LOGGER.error("Could not save client settings", exception);
                Component error = Component.translatable("message.ecclientsettings.settings.save_failed");
                Minecraft minecraft = Minecraft.getInstance();
                minecraft.schedule(() -> minecraft.setScreen(create(
                        parent,
                        draft,
                        allowTitle[0],
                        allowFrame[0],
                        error
                )));
            }
        });
        return builder.build();
    }

    private static void addProfileCategory(
            ConfigBuilder builder,
            ConfigEntryBuilder entries,
            Screen settingsParent,
            ProfileManager profiles,
            ProfileSettingsDraft draft,
            Component saveError
    ) {
        ConfigCategory category = builder.getOrCreateCategory(
                Component.translatable("category.ecclientsettings.profile")
        );
        category.addEntry(entries.startTextDescription(Component.translatable(
                "option.ecclientsettings.profile.current",
                profiles.activeSnapshot().name()
        )).build());
        category.addEntry(new ProfileManagementEntry(settingsParent));
        if (!profiles.warnings().isEmpty()) {
            category.addEntry(entries.startTextDescription(Component.translatable(
                    "message.ecclientsettings.profile.recovery_warning",
                    profiles.warnings().size()
            )).build());
        }
        if (saveError != null) {
            category.addEntry(entries.startTextDescription(saveError).build());
        }
        if (!profiles.activeSnapshot().id().equals(draft.profileId())) {
            category.addEntry(entries.startTextDescription(Component.translatable(
                    "message.ecclientsettings.profile.stale_draft"
            )).build());
        }
    }

    private static void addMovementCategory(
            ConfigBuilder builder,
            ConfigEntryBuilder entries,
            ProfileSettingsDraft draft
    ) {
        ConfigCategory category = builder.getOrCreateCategory(
                Component.translatable("category.ecclientsettings.movement")
        );
        category.addEntry(entries.startBooleanToggle(
                        Component.translatable("option.ecclientsettings.force_sprint"),
                        draft.features().forceSprint().enabled()
                )
                .setDefaultValue(ClientSettingsConfig.DEFAULT_FORCE_SPRINT)
                .setTooltip(Component.translatable("option.ecclientsettings.force_sprint.tooltip"))
                .setSaveConsumer(draft::setForceSprint)
                .build());
    }

    private static void addCombatCategory(ConfigBuilder builder, ConfigEntryBuilder entries) {
        ConfigCategory category = builder.getOrCreateCategory(
                Component.translatable("category.ecclientsettings.combat")
        );
        category.addEntry(entries.startBooleanToggle(
                        Component.translatable("option.ecclientsettings.sword_blocking_animation"),
                        ClientSettingsConfig.swordBlockingAnimation()
                )
                .setDefaultValue(ClientSettingsConfig.DEFAULT_SWORD_BLOCKING_ANIMATION)
                .setTooltip(Component.translatable("option.ecclientsettings.sword_blocking_animation.tooltip"))
                .setSaveConsumer(ClientSettingsConfig.SWORD_BLOCKING_ANIMATION::set)
                .build());
    }

    private static void addServerPermissionsCategory(
            ConfigBuilder builder,
            ConfigEntryBuilder entries,
            boolean[] allowTitle,
            boolean[] allowFrame
    ) {
        ConfigCategory category = builder.getOrCreateCategory(
                Component.translatable("category.ecclientsettings.server_window")
        );
        category.addEntry(entries.startBooleanToggle(
                        Component.translatable("option.ecclientsettings.server_window_title"),
                        allowTitle[0]
                )
                .setDefaultValue(ClientSettingsConfig.DEFAULT_ALLOW_SERVER_WINDOW_TITLE)
                .setTooltip(Component.translatable("option.ecclientsettings.server_window_title.tooltip"))
                .setSaveConsumer(value -> allowTitle[0] = value)
                .build());
        category.addEntry(entries.startBooleanToggle(
                        Component.translatable("option.ecclientsettings.server_window_frame"),
                        allowFrame[0]
                )
                .setDefaultValue(ClientSettingsConfig.DEFAULT_ALLOW_SERVER_WINDOW_FRAME)
                .setTooltip(Component.translatable("option.ecclientsettings.server_window_frame.tooltip"))
                .setSaveConsumer(value -> allowFrame[0] = value)
                .build());
    }

    private static void addRenderingCategory(
            ConfigBuilder builder,
            ConfigEntryBuilder entries,
            ProfileSettingsDraft draft
    ) {
        ConfigCategory category = builder.getOrCreateCategory(
                Component.translatable("category.ecclientsettings.rendering")
        );
        category.addEntry(entries.startBooleanToggle(
                        Component.translatable("option.ecclientsettings.block_outline.enabled"),
                        draft.features().blockOutline().enabled()
                )
                .setDefaultValue(false)
                .setTooltip(Component.translatable("option.ecclientsettings.block_outline.enabled.tooltip"))
                .setSaveConsumer(draft::setBlockOutlineEnabled)
                .build());
        category.addEntry(entries.startAlphaColorField(
                        Component.translatable("option.ecclientsettings.block_outline.color"),
                        draft.features().blockOutline().color().value()
                )
                .setDefaultValue(BlockOutlineSettings.DEFAULT.color().value())
                .setTooltip(Component.translatable("option.ecclientsettings.block_outline.color.tooltip"))
                .setSaveConsumer(draft::setBlockOutlineColor)
                .build());
        category.addEntry(entries.startBooleanToggle(
                        Component.translatable("option.ecclientsettings.low_fire.enabled"),
                        draft.features().lowFire().enabled()
                )
                .setDefaultValue(false)
                .setTooltip(Component.translatable("option.ecclientsettings.low_fire.enabled.tooltip"))
                .setSaveConsumer(draft::setLowFireEnabled)
                .build());
        category.addEntry(entries.startDoubleField(
                        Component.translatable("option.ecclientsettings.low_fire.offset"),
                        draft.features().lowFire().verticalOffset()
                )
                .setDefaultValue(LowFireSettings.DEFAULT.verticalOffset())
                .setMin(0.0)
                .setMax(0.5)
                .setTooltip(Component.translatable("option.ecclientsettings.low_fire.offset.tooltip"))
                .setSaveConsumer(draft::setLowFireOffset)
                .build());
        category.addEntry(entries.startEnumSelector(
                        Component.translatable("option.ecclientsettings.fullbright.mode"),
                        FullbrightMode.class,
                        draft.features().fullbright().mode()
                )
                .setDefaultValue(FullbrightMode.OFF)
                .setEnumNameProvider(mode -> Component.translatable(
                        "option.ecclientsettings.fullbright.mode." + mode.name().toLowerCase(Locale.ROOT)
                ))
                .setTooltip(Component.translatable("option.ecclientsettings.fullbright.mode.tooltip"))
                .setSaveConsumer(draft::setFullbrightMode)
                .build());
        category.addEntry(entries.startDoubleField(
                        Component.translatable("option.ecclientsettings.fullbright.strength"),
                        draft.features().fullbright().strength()
                )
                .setDefaultValue(FullbrightSettings.DEFAULT.strength())
                .setMin(0.0)
                .setMax(1.0)
                .setTooltip(Component.translatable("option.ecclientsettings.fullbright.strength.tooltip"))
                .setSaveConsumer(draft::setFullbrightStrength)
                .build());
        category.addEntry(entries.startEnumSelector(
                        Component.translatable("option.ecclientsettings.time_changer.mode"),
                        TimeChangerMode.class,
                        draft.features().timeChanger().mode()
                )
                .setDefaultValue(TimeChangerMode.FOLLOW_SERVER)
                .setEnumNameProvider(mode -> Component.translatable(
                        "option.ecclientsettings.time_changer.mode." + mode.name().toLowerCase(Locale.ROOT)
                ))
                .setTooltip(Component.translatable("option.ecclientsettings.time_changer.mode.tooltip"))
                .setSaveConsumer(draft::setTimeChangerMode)
                .build());
        category.addEntry(entries.startIntField(
                        Component.translatable("option.ecclientsettings.time_changer.custom_time"),
                        draft.features().timeChanger().customTime()
                )
                .setDefaultValue(TimeChangerSettings.DEFAULT.customTime())
                .setMin(0)
                .setMax(23_999)
                .setTooltip(Component.translatable("option.ecclientsettings.time_changer.custom_time.tooltip"))
                .setSaveConsumer(draft::setTimeChangerCustomTime)
                .build());
        category.addEntry(entries.startBooleanToggle(
                        Component.translatable("option.ecclientsettings.zoom.enabled"),
                        draft.zoomEnabled()
                )
                .setDefaultValue(ZoomSettings.DEFAULT.enabled())
                .setTooltip(Component.translatable("option.ecclientsettings.zoom.enabled.tooltip"))
                .setSaveConsumer(draft::setZoomEnabled)
                .build());
        category.addEntry(entries.startEnumSelector(
                        Component.translatable("option.ecclientsettings.zoom.activation"),
                        ZoomActivation.class,
                        draft.zoomActivation()
                )
                .setDefaultValue(ZoomSettings.DEFAULT.activation())
                .setEnumNameProvider(activation -> Component.translatable(
                        "option.ecclientsettings.zoom.activation." + activation.name().toLowerCase(Locale.ROOT)
                ))
                .setSaveConsumer(draft::setZoomActivation)
                .build());
        category.addEntry(entries.startDoubleField(
                        Component.translatable("option.ecclientsettings.zoom.divisor"),
                        draft.zoomDivisor()
                )
                .setDefaultValue(ZoomSettings.DEFAULT.divisor())
                .setMin(1.0)
                .setMax(16.0)
                .setTooltip(Component.translatable("option.ecclientsettings.zoom.divisor.tooltip"))
                .setSaveConsumer(draft::setZoomDivisor)
                .build());
        category.addEntry(entries.startDoubleField(
                        Component.translatable("option.ecclientsettings.zoom.max_divisor"),
                        draft.zoomMaxDivisor()
                )
                .setDefaultValue(ZoomSettings.DEFAULT.maxDivisor())
                .setMin(1.0)
                .setMax(32.0)
                .setTooltip(Component.translatable("option.ecclientsettings.zoom.max_divisor.tooltip"))
                .setSaveConsumer(draft::setZoomMaxDivisor)
                .build());
        category.addEntry(entries.startDoubleField(
                        Component.translatable("option.ecclientsettings.zoom.animation_speed"),
                        draft.zoomAnimationSpeed()
                )
                .setDefaultValue(ZoomSettings.DEFAULT.animationSpeed())
                .setMin(1.0)
                .setMax(10.0)
                .setTooltip(Component.translatable("option.ecclientsettings.zoom.animation_speed.tooltip"))
                .setSaveConsumer(draft::setZoomAnimationSpeed)
                .build());
        category.addEntry(entries.startBooleanToggle(
                        Component.translatable("option.ecclientsettings.zoom.scroll_adjustment"),
                        draft.zoomScrollAdjustment()
                )
                .setDefaultValue(ZoomSettings.DEFAULT.scrollAdjustment())
                .setTooltip(Component.translatable("option.ecclientsettings.zoom.scroll_adjustment.tooltip"))
                .setSaveConsumer(draft::setZoomScrollAdjustment)
                .build());
        category.addEntry(entries.startBooleanToggle(
                        Component.translatable("option.ecclientsettings.zoom.reduce_sensitivity"),
                        draft.zoomReduceSensitivity()
                )
                .setDefaultValue(ZoomSettings.DEFAULT.reduceSensitivity())
                .setTooltip(Component.translatable("option.ecclientsettings.zoom.reduce_sensitivity.tooltip"))
                .setSaveConsumer(draft::setZoomReduceSensitivity)
                .build());
        category.addEntry(entries.startBooleanToggle(
                        Component.translatable("option.ecclientsettings.zoom.smooth_camera"),
                        draft.zoomSmoothCamera()
                )
                .setDefaultValue(ZoomSettings.DEFAULT.smoothCamera())
                .setTooltip(Component.translatable("option.ecclientsettings.zoom.smooth_camera.tooltip"))
                .setSaveConsumer(draft::setZoomSmoothCamera)
                .build());
        category.addEntry(entries.startBooleanToggle(
                        Component.translatable("option.ecclientsettings.hit_color.enabled"),
                        draft.features().hitColor().enabled()
                )
                .setDefaultValue(HitColorSettings.DEFAULT.enabled())
                .setTooltip(Component.translatable("option.ecclientsettings.hit_color.enabled.tooltip"))
                .setSaveConsumer(draft::setHitColorEnabled)
                .build());
        category.addEntry(entries.startAlphaColorField(
                        Component.translatable("option.ecclientsettings.hit_color.color"),
                        draft.features().hitColor().color().value()
                )
                .setDefaultValue(HitColorSettings.DEFAULT.color().value())
                .setTooltip(Component.translatable("option.ecclientsettings.hit_color.color.tooltip"))
                .setSaveConsumer(draft::setHitColor)
                .build());
    }
}
