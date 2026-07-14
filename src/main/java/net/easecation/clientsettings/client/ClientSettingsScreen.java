package net.easecation.clientsettings.client;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.easecation.clientsettings.config.ClientSettingsConfig;
import net.easecation.clientsettings.window.WindowAppearanceController;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class ClientSettingsScreen {

    private ClientSettingsScreen() {
    }

    public static Screen create(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("screen.ecclientsettings.title"));
        ConfigCategory category = builder.getOrCreateCategory(
                Component.translatable("category.ecclientsettings.default")
        );
        ConfigCategory windowCategory = builder.getOrCreateCategory(
                Component.translatable("category.ecclientsettings.server_window")
        );
        ConfigEntryBuilder entries = builder.entryBuilder();

        category.addEntry(entries.startBooleanToggle(
                        Component.translatable("option.ecclientsettings.force_sprint"),
                        ClientSettingsConfig.forceSprint()
                )
                .setDefaultValue(ClientSettingsConfig.DEFAULT_FORCE_SPRINT)
                .setTooltip(Component.translatable("option.ecclientsettings.force_sprint.tooltip"))
                .setSaveConsumer(ClientSettingsConfig.FORCE_SPRINT::set)
                .build());

        windowCategory.addEntry(entries.startBooleanToggle(
                        Component.translatable("option.ecclientsettings.server_window_title"),
                        ClientSettingsConfig.allowServerWindowTitle()
                )
                .setDefaultValue(ClientSettingsConfig.DEFAULT_ALLOW_SERVER_WINDOW_TITLE)
                .setTooltip(Component.translatable("option.ecclientsettings.server_window_title.tooltip"))
                .setSaveConsumer(ClientSettingsConfig.ALLOW_SERVER_WINDOW_TITLE::set)
                .build());
        windowCategory.addEntry(entries.startBooleanToggle(
                        Component.translatable("option.ecclientsettings.server_window_frame"),
                        ClientSettingsConfig.allowServerWindowFrame()
                )
                .setDefaultValue(ClientSettingsConfig.DEFAULT_ALLOW_SERVER_WINDOW_FRAME)
                .setTooltip(Component.translatable("option.ecclientsettings.server_window_frame.tooltip"))
                .setSaveConsumer(ClientSettingsConfig.ALLOW_SERVER_WINDOW_FRAME::set)
                .build());

        builder.setSavingRunnable(() -> {
            ClientSettingsConfig.SPEC.save();
            WindowAppearanceController.getInstance().reconcilePermissions();
        });
        return builder.build();
    }
}

