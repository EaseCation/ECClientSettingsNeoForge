package net.easecation.clientsettings.client;

import java.util.List;
import java.util.Optional;
import me.shedaniel.clothconfig2.gui.entries.TooltipListEntry;
import net.easecation.clientsettings.profile.model.HudWidgetId;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;

final class HudEnabledEntry extends TooltipListEntry<Boolean> {
    private final HudEnabledBinding binding;
    private final Button toggle;
    private final Button reset;

    HudEnabledEntry(ProfileSettingsDraft draft, HudWidgetId id) {
        super(Component.translatable("option.ecclientsettings.hud." + id.serializedName() + ".enabled"),
                () -> Optional.of(new Component[]{Component.translatable(
                        "option.ecclientsettings.hud." + id.serializedName() + ".enabled.tooltip")}));
        binding = new HudEnabledBinding(draft, id);
        toggle = Button.builder(stateMessage(), button -> { binding.toggle(); refresh(); }).bounds(0, 0, 100, 20).build();
        reset = Button.builder(Component.translatable("text.cloth-config.reset_value"),
                button -> { binding.reset(); refresh(); }).bounds(0, 0, 48, 20).build();
        // Cloth binds its screen after constructing entries; isEditable() dereferences it.
        // The first render refreshes editability once the screen is attached.
        reset.active = binding.value() != binding.defaultValue();
    }

    private Component stateMessage() {
        return Component.translatable(binding.value()
                ? "screen.ecclientsettings.hud_editor.enabled" : "screen.ecclientsettings.hud_editor.disabled");
    }

    private void refresh() {
        toggle.setMessage(stateMessage());
        toggle.active = isEditable();
        reset.active = isEditable() && binding.value() != binding.defaultValue();
    }

    @Override public Boolean getValue() { return binding.value(); }
    @Override public Optional<Boolean> getDefaultValue() { return Optional.of(binding.defaultValue()); }
    @Override public boolean isEdited() { return binding.edited(); }
    // No cached widget value to write back: every interaction already updates the draft.
    @Override public void save() {}
    @Override public int getItemHeight() { return 24; }
    @Override public List<? extends GuiEventListener> children() { return List.of(toggle, reset); }
    @Override public List<? extends NarratableEntry> narratables() { return List.of(toggle, reset); }

    @Override public void render(GuiGraphics graphics, int index, int y, int x, int width, int height,
            int mouseX, int mouseY, boolean hovered, float partialTick) {
        super.render(graphics, index, y, x, width, height, mouseX, mouseY, hovered, partialTick);
        refresh();
        reset.setX(x + width - 48); reset.setY(y);
        toggle.setX(x + width - 150); toggle.setY(y);
        graphics.drawString(Minecraft.getInstance().font, getFieldName(), x, y + 6, getPreferredTextColor());
        toggle.render(graphics, mouseX, mouseY, partialTick);
        reset.render(graphics, mouseX, mouseY, partialTick);
    }
}
