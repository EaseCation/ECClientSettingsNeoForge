package net.easecation.clientsettings.client;

import me.shedaniel.clothconfig2.gui.entries.TextListEntry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

@SuppressWarnings("deprecation")
final class ProfileManagementEntry extends TextListEntry {

    private final Screen settingsParent;
    private int entryX;
    private int entryY;
    private int entryWidth;
    private int entryHeight;
    private boolean hasRenderedBounds;

    ProfileManagementEntry(Screen settingsParent) {
        super(
                Component.translatable("option.ecclientsettings.profile.manage"),
                Component.translatable("option.ecclientsettings.profile.manage.action")
                        .withStyle(ChatFormatting.YELLOW, ChatFormatting.UNDERLINE)
        );
        this.settingsParent = settingsParent;
    }

    @Override
    public void render(
            GuiGraphics graphics,
            int index,
            int y,
            int x,
            int entryWidth,
            int entryHeight,
            int mouseX,
            int mouseY,
            boolean hovered,
            float partialTick
    ) {
        this.entryX = x;
        this.entryY = y;
        this.entryWidth = entryWidth;
        this.entryHeight = entryHeight;
        this.hasRenderedBounds = true;
        super.render(graphics, index, y, x, entryWidth, entryHeight, mouseX, mouseY, hovered, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && hasRenderedBounds
                && contains(mouseX, mouseY, entryX, entryY, entryWidth, entryHeight)) {
            Minecraft.getInstance().setScreen(new ProfileManagementScreen(settingsParent));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    static boolean contains(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}
