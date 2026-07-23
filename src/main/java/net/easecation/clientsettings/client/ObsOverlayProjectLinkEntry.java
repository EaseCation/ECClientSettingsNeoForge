package net.easecation.clientsettings.client;

import me.shedaniel.clothconfig2.gui.entries.TextListEntry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

@SuppressWarnings("deprecation")
final class ObsOverlayProjectLinkEntry extends TextListEntry {

    private static final String PROJECT_URL = "https://github.com/zziger/obs-overlay";

    private int entryX;
    private int entryY;
    private int entryWidth;
    private int entryHeight;
    private boolean hasRenderedBounds;

    ObsOverlayProjectLinkEntry() {
        super(
                Component.translatable("option.ecclientsettings.obs_overlay.project"),
                Component.literal(PROJECT_URL).withStyle(ChatFormatting.AQUA, ChatFormatting.UNDERLINE)
        );
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
            Minecraft minecraft = Minecraft.getInstance();
            Screen clothScreen = minecraft.screen;
            if (clothScreen != null) {
                ConfirmLinkScreen.confirmLinkNow(clothScreen, PROJECT_URL);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private static boolean contains(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}
