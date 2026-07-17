package net.easecation.clientsettings.client;

import me.shedaniel.clothconfig2.gui.entries.TextListEntry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

@SuppressWarnings("deprecation")
final class ProfileManagementEntry extends TextListEntry {

    private final Screen settingsParent;

    ProfileManagementEntry(Screen settingsParent) {
        super(
                Component.translatable("option.ecclientsettings.profile.manage"),
                Component.translatable("option.ecclientsettings.profile.manage.action")
                        .withStyle(ChatFormatting.YELLOW, ChatFormatting.UNDERLINE)
        );
        this.settingsParent = settingsParent;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            Minecraft.getInstance().setScreen(new ProfileManagementScreen(settingsParent));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
