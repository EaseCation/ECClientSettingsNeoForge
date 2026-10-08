package net.easecation.clientsettings.feature.helditeminfo;

import java.util.ArrayList;
import java.util.List;
import java.util.IdentityHashMap;
import java.util.Map;
import net.easecation.clientsettings.profile.model.HeldItemInfoSettings;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.component.ItemLore;

public final class HeldItemInfoContent {
    public enum Kind { NAME, DESCRIPTION, OTHER }
    public record Line(Component text, Kind kind) {}
    private HeldItemInfoContent() {}

    public static List<Line> collect(ItemStack original, Player player, Item.TooltipContext context,
            HeldItemInfoSettings settings) {
        TooltipDisplay display = original.getOrDefault(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT);
        if (original.isEmpty() || display.hideTooltip()) return List.of();
        // Filter native components on a copy, retaining server hidden-component rules.
        ItemStack stack = original.copy();
        if (!settings.showDescription()) display = display.withHidden(DataComponents.LORE, true);
        if (!settings.showEnchantments()) {
            display = display.withHidden(DataComponents.ENCHANTMENTS, true)
                    .withHidden(DataComponents.STORED_ENCHANTMENTS, true);
        }
        stack.set(DataComponents.TOOLTIP_DISPLAY, display);
        ItemLore originalLore = stack.get(DataComponents.LORE);
        if (originalLore != null) {
            // Vanilla styledLines is a lazy transforming list that creates new Components on
            // every iteration. Materialize only on our copy to identify native lore reliably.
            stack.set(DataComponents.LORE, new ItemLore(originalLore.lines(), List.copyOf(originalLore.styledLines())));
        }
        List<Component> lore = new ArrayList<>();
        stack.addToTooltip(DataComponents.LORE, context, display, lore::add, TooltipFlag.NORMAL);
        List<Line> result = new ArrayList<>();
        if (settings.showAdditional()) {
            // Keep NeoForge tooltip hooks and native component ordering; never parse localized text.
            List<Component> tooltip = stack.getTooltipLines(context, player, TooltipFlag.NORMAL);
            Map<Component, Integer> unmatchedLore = new IdentityHashMap<>();
            lore.forEach(text -> unmatchedLore.merge(text, 1, Integer::sum));
            for (int i = 0; i < tooltip.size(); i++) {
                if (i == 0) {
                    if (settings.showName()) result.add(new Line(tooltip.get(i), Kind.NAME));
                } else {
                    // Native lore providers reuse their immutable styled components. Identity avoids
                    // mistaking an unrelated, identically worded enchantment/add-on line for lore.
                    int remaining = unmatchedLore.getOrDefault(tooltip.get(i), 0);
                    boolean description = remaining > 0;
                    if (description) unmatchedLore.put(tooltip.get(i), remaining - 1);
                    result.add(new Line(tooltip.get(i), description ? Kind.DESCRIPTION : Kind.OTHER));
                }
            }
        } else {
            if (settings.showName()) result.add(new Line(stack.getStyledHoverName(), Kind.NAME));
            List<Component> enchantments = new ArrayList<>();
            stack.addToTooltip(DataComponents.STORED_ENCHANTMENTS, context, display, enchantments::add, TooltipFlag.NORMAL);
            stack.addToTooltip(DataComponents.ENCHANTMENTS, context, display, enchantments::add, TooltipFlag.NORMAL);
            enchantments.forEach(text -> result.add(new Line(text, Kind.OTHER)));
            lore.forEach(text -> result.add(new Line(text, Kind.DESCRIPTION)));
        }
        return List.copyOf(result);
    }
}
