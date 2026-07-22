package net.easecation.clientsettings.client;

import me.shedaniel.clothconfig2.gui.entries.TextListEntry;
import me.shedaniel.clothconfig2.api.AbstractConfigEntry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Objects;

@SuppressWarnings("deprecation")
final class HudEditorEntry extends TextListEntry {

    private final ProfileSettingsDraft draft;
    private final List<AbstractConfigEntry<?>> hudControls;
    private int entryX;
    private int entryY;
    private int entryWidth;
    private int entryHeight;
    private boolean hasRenderedBounds;

    HudEditorEntry(ProfileSettingsDraft draft, List<AbstractConfigEntry<?>> hudControls) {
        super(
                Component.translatable("option.ecclientsettings.hud.edit_layout"),
                Component.translatable("option.ecclientsettings.hud.edit_layout.action")
                        .withStyle(ChatFormatting.YELLOW, ChatFormatting.UNDERLINE)
        );
        this.draft = draft;
        this.hudControls = Objects.requireNonNull(hudControls, "HUD controls");
    }

    @Override
    public boolean isEdited() {
        return isDraftEdited(draft) || super.isEdited();
    }

    static boolean isDraftEdited(ProfileSettingsDraft draft) {
        return draft.edited();
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
                hudControls.forEach(AbstractConfigEntry::save);
                minecraft.setScreen(new HudEditorScreen(clothScreen, draft));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    static boolean contains(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}
