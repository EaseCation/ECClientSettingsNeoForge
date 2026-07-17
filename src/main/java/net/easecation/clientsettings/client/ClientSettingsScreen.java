package net.easecation.clientsettings.client;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.easecation.clientsettings.ECClientSettings;
import net.easecation.clientsettings.config.ClientSettingsConfig;
import net.easecation.clientsettings.profile.model.BlockOutlineSettings;
import net.easecation.clientsettings.profile.runtime.ProfileManager;
import net.easecation.clientsettings.profile.runtime.ProfileServices;
import net.easecation.clientsettings.window.WindowAppearanceController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.IOException;

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
    }
}
