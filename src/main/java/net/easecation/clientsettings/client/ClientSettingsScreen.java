package net.easecation.clientsettings.client;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.api.AbstractConfigEntry;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import net.easecation.clientsettings.ECClientSettings;
import net.easecation.clientsettings.config.ClientSettingsConfig;
import net.easecation.clientsettings.feature.obsoverlay.ObsOverlayComponent;
import net.easecation.clientsettings.feature.obsoverlay.ObsOverlayRuntime;
import net.easecation.clientsettings.feature.obsoverlay.ObsOverlayScreen;
import net.easecation.clientsettings.feature.obsoverlay.ObsOverlaySettings;
import net.easecation.clientsettings.profile.model.BlockOutlineSettings;
import net.easecation.clientsettings.profile.model.LowFireSettings;
import net.easecation.clientsettings.profile.model.FullbrightMode;
import net.easecation.clientsettings.profile.model.FullbrightSettings;
import net.easecation.clientsettings.profile.model.HitColorSettings;
import net.easecation.clientsettings.profile.model.HudSettings;
import net.easecation.clientsettings.profile.model.HudWidgetId;
import net.easecation.clientsettings.profile.model.TimeChangerMode;
import net.easecation.clientsettings.profile.model.TimeChangerSettings;
import net.easecation.clientsettings.profile.model.ZoomActivation;
import net.easecation.clientsettings.profile.model.ZoomSettings;
import net.easecation.clientsettings.profile.runtime.ProfileManager;
import net.easecation.clientsettings.profile.runtime.ProfileServices;
import net.easecation.clientsettings.window.WindowAppearanceController;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class ClientSettingsScreen {

    private ClientSettingsScreen() {
    }

    public static Screen create(Screen parent) {
        ProfileManager profiles = ProfileServices.manager();
        return create(parent, ProfileSettingsDraft.active(profiles), ObsOverlaySettingsDraft.current());
    }

    static Screen create(Screen parent, ProfileSettingsDraft draft) {
        return create(parent, draft, ObsOverlaySettingsDraft.current());
    }

    private static Screen create(
            Screen parent,
            ProfileSettingsDraft draft,
            ObsOverlaySettingsDraft obsOverlayDraft
    ) {
        return create(
                parent,
                draft,
                obsOverlayDraft,
                ClientSettingsConfig.allowServerWindowTitle(),
                ClientSettingsConfig.allowServerWindowFrame(),
                null
        );
    }

    private static Screen create(
            Screen parent,
            ProfileSettingsDraft draft,
            ObsOverlaySettingsDraft obsOverlayDraft,
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
        addHudCategory(builder, entries, draft);
        addObsOverlayCategory(builder, entries, obsOverlayDraft, saveError);
        addRenderingCategory(builder, entries, draft);
        addServerPermissionsCategory(builder, entries, allowTitle, allowFrame);

        builder.setSavingRunnable(() -> {
            try {
                // Persist privacy controls first so an unrelated Profile write cannot prevent protection changes.
                obsOverlayDraft.save();
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
                        obsOverlayDraft,
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

    private static void addObsOverlayCategory(
            ConfigBuilder builder,
            ConfigEntryBuilder entries,
            ObsOverlaySettingsDraft draft,
            Component saveError
    ) {
        ConfigCategory category = builder.getOrCreateCategory(
                Component.translatable("category.ecclientsettings.obs_overlay")
        );
        category.addEntry(entries.startTextDescription(
                Component.translatable("option.ecclientsettings.obs_overlay.description")
        ).build());
        category.addEntry(entries.startTextDescription(
                Component.translatable("option.ecclientsettings.obs_overlay.safety_warning")
                        .withStyle(ChatFormatting.RED, ChatFormatting.BOLD)
        ).build());
        if (saveError != null) {
            category.addEntry(entries.startTextDescription(saveError.copy().withStyle(ChatFormatting.RED)).build());
        }

        category.addEntry(new ObsOverlayStatusEntry());
        category.addEntry(entries.startTextDescription(
                Component.translatable("option.ecclientsettings.obs_overlay.compatibility_warning")
                        .withStyle(ChatFormatting.GOLD)
        ).build());

        category.addEntry(entries.startTextDescription(
                Component.translatable("option.ecclientsettings.obs_overlay.setup_heading")
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
        ).build());
        for (int step = 1; step <= 4; step++) {
            category.addEntry(entries.startTextDescription(Component.translatable(
                    "option.ecclientsettings.obs_overlay.setup_step_" + step
            )).build());
        }

        var general = entries.startSubCategory(
                Component.translatable("option.ecclientsettings.obs_overlay.group.general")
        ).setExpanded(true);
        general.add(entries.startBooleanToggle(
                        Component.translatable("option.ecclientsettings.obs_overlay.enabled"),
                        draft.enabled()
                )
                .setDefaultValue(ObsOverlaySettings.DEFAULT.enabled())
                .setTooltip(Component.translatable("option.ecclientsettings.obs_overlay.enabled.tooltip"))
                .setSaveConsumer(draft::setEnabled)
                .build());
        general.add(entries.startBooleanToggle(
                        Component.translatable("option.ecclientsettings.obs_overlay.show_test_marker"),
                        draft.showTestMarker()
                )
                .setDefaultValue(ObsOverlaySettings.DEFAULT.showTestMarker())
                .setTooltip(Component.translatable("option.ecclientsettings.obs_overlay.show_test_marker.tooltip"))
                .setSaveConsumer(draft::setShowTestMarker)
                .build());
        general.add(entries.startBooleanToggle(
                        Component.translatable("option.ecclientsettings.obs_overlay.fail_closed"),
                        draft.failClosed()
                )
                .setDefaultValue(ObsOverlaySettings.DEFAULT.failClosed())
                .setTooltip(Component.translatable("option.ecclientsettings.obs_overlay.fail_closed.tooltip"))
                .setSaveConsumer(draft::setFailClosed)
                .build());
        category.addEntry(general.build());

        var hud = entries.startSubCategory(
                Component.translatable("option.ecclientsettings.obs_overlay.group.hud")
        );
        for (ObsOverlayComponent component : ObsOverlayComponent.values()) {
            if (component.group() == ObsOverlayComponent.Group.HUD) {
                hud.add(componentToggle(entries, draft, component));
            }
        }
        category.addEntry(hud.build());

        var world = entries.startSubCategory(
                Component.translatable("option.ecclientsettings.obs_overlay.group.world")
        );
        world.add(entries.startTextDescription(
                Component.translatable("option.ecclientsettings.obs_overlay.world.experimental")
                        .withStyle(ChatFormatting.GOLD)
        ).build());
        for (ObsOverlayComponent component : ObsOverlayComponent.values()) {
            if (component.group() == ObsOverlayComponent.Group.WORLD) {
                world.add(componentToggle(entries, draft, component));
            }
        }
        category.addEntry(world.build());

        var screens = entries.startSubCategory(
                Component.translatable("option.ecclientsettings.obs_overlay.group.screens")
        );
        screens.add(entries.startTextDescription(
                Component.translatable("option.ecclientsettings.obs_overlay.screens.description")
        ).build());
        screens.add(entries.startBooleanToggle(
                        Component.translatable("option.ecclientsettings.obs_overlay.screen.all_in_game"),
                        draft.hideAllInGameScreens()
                )
                .setDefaultValue(ObsOverlaySettings.DEFAULT.hideAllInGameScreens())
                .setTooltip(Component.translatable("option.ecclientsettings.obs_overlay.screen.all_in_game.tooltip"))
                .setSaveConsumer(draft::setHideAllInGameScreens)
                .build());
        for (ObsOverlayScreen screen : ObsOverlayScreen.values()) {
            screens.add(entries.startBooleanToggle(
                            Component.translatable(
                                    "option.ecclientsettings.obs_overlay.screen." + screen.serializedName()
                            ),
                            draft.screen(screen)
                    )
                    .setDefaultValue(ObsOverlaySettings.DEFAULT.screens().get(screen))
                    .setTooltip(Component.translatable(
                            "option.ecclientsettings.obs_overlay.screen." + screen.serializedName() + ".tooltip"
                    ))
                    .setSaveConsumer(enabled -> draft.setScreen(screen, enabled))
                    .build());
        }
        screens.add(entries.startBooleanToggle(
                        Component.translatable("option.ecclientsettings.obs_overlay.custom_screens_enabled"),
                        draft.customHandledScreensEnabled()
                )
                .setDefaultValue(ObsOverlaySettings.DEFAULT.customHandledScreensEnabled())
                .setTooltip(Component.translatable(
                        "option.ecclientsettings.obs_overlay.custom_screens_enabled.tooltip"
                ))
                .setSaveConsumer(draft::setCustomHandledScreensEnabled)
                .build());
        screens.add(entries.startStrList(
                        Component.translatable("option.ecclientsettings.obs_overlay.custom_screen_ids"),
                        draft.customHandledScreenIds()
                )
                .setDefaultValue(ObsOverlaySettings.DEFAULT.customHandledScreenIds())
                .setExpanded(false)
                .setTooltip(Component.translatable("option.ecclientsettings.obs_overlay.custom_screen_ids.tooltip"))
                .setCellErrorSupplier(ClientSettingsScreen::validateMenuId)
                .setSaveConsumer(draft::setCustomHandledScreenIds)
                .build());
        category.addEntry(screens.build());

        var autoHide = entries.startSubCategory(
                Component.translatable("option.ecclientsettings.obs_overlay.group.auto_hide")
        );
        autoHide.add(entries.startTextDescription(
                Component.translatable("option.ecclientsettings.obs_overlay.auto_hide.description")
        ).build());
        for (ObsOverlayComponent component : ObsOverlayComponent.values()) {
            if (component.autoHideSupported()) {
                autoHide.add(entries.startBooleanToggle(
                                Component.translatable(
                                        "option.ecclientsettings.obs_overlay.auto_hide.component",
                                        Component.translatable(
                                                "option.ecclientsettings.obs_overlay.component."
                                                        + component.serializedName()
                                        )
                                ),
                                draft.autoHide(component)
                        )
                        .setDefaultValue(ObsOverlaySettings.DEFAULT.autoHide().get(component))
                        .setTooltip(Component.translatable(
                                "option.ecclientsettings.obs_overlay.auto_hide.component.tooltip"
                        ))
                        .setSaveConsumer(enabled -> draft.setAutoHide(component, enabled))
                        .build());
            }
        }
        category.addEntry(autoHide.build());

        category.addEntry(entries.startTextDescription(
                Component.translatable("option.ecclientsettings.obs_overlay.attribution")
        ).build());
        category.addEntry(entries.startTextDescription(
                Component.translatable("option.ecclientsettings.obs_overlay.license")
        ).build());
        category.addEntry(new ObsOverlayProjectLinkEntry());
    }

    private static AbstractConfigListEntry<?> componentToggle(
            ConfigEntryBuilder entries,
            ObsOverlaySettingsDraft draft,
            ObsOverlayComponent component
    ) {
        String key = "option.ecclientsettings.obs_overlay.component." + component.serializedName();
        return entries.startBooleanToggle(Component.translatable(key), draft.component(component))
                .setDefaultValue(component.defaultEnabled())
                .setTooltip(Component.translatable(key + ".tooltip"))
                .setSaveConsumer(enabled -> draft.setComponent(component, enabled))
                .build();
    }

    private static Optional<Component> validateMenuId(String value) {
        String candidate = value.trim().toLowerCase(Locale.ROOT);
        if (candidate.isEmpty()) {
            return Optional.of(Component.translatable(
                    "option.ecclientsettings.obs_overlay.custom_screen_ids.error.empty"
            ));
        }
        if (ResourceLocation.tryParse(candidate) != null
                || (!candidate.contains(":") && ResourceLocation.tryParse("minecraft:" + candidate) != null)) {
            return Optional.empty();
        }
        return Optional.of(Component.translatable(
                "option.ecclientsettings.obs_overlay.custom_screen_ids.error.invalid"
        ));
    }

    private static void addHudCategory(
            ConfigBuilder builder,
            ConfigEntryBuilder entries,
            ProfileSettingsDraft draft
    ) {
        ConfigCategory category = builder.getOrCreateCategory(
                Component.translatable("category.ecclientsettings.hud")
        );
        category.addEntry(entries.startTextDescription(
                Component.translatable("option.ecclientsettings.hud.description")
        ).build());
        List<AbstractConfigEntry<?>> hudControls = new ArrayList<>();
        category.addEntry(new HudEditorEntry(draft, hudControls));
        for (HudWidgetId id : HudWidgetId.values()) {
            String name = id.serializedName();
            var enabledControl = entries.startBooleanToggle(
                            Component.translatable("option.ecclientsettings.hud." + name + ".enabled"),
                            draft.hudSettings().widget(id).enabled()
                    )
                    .setDefaultValue(HudSettings.DEFAULT.widget(id).enabled())
                    .setTooltip(Component.translatable(
                            "option.ecclientsettings.hud." + name + ".enabled.tooltip"
                    ))
                    .setSaveConsumer(enabled -> draft.setHudEnabled(id, enabled))
                    .build();
            hudControls.add(enabledControl);
            category.addEntry(enabledControl);
        }
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
