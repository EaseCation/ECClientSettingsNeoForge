package net.easecation.clientsettings.feature.helditeminfo;

import java.util.List;
import java.util.function.Consumer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.easecation.clientsettings.profile.model.HeldItemInfoSettings;
import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.TooltipDisplay;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HeldItemInfoContentTest {
    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private static ItemStack describedItem() {
        var stack = new ItemStack(Items.PAPER);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("Menu"));
        stack.set(DataComponents.LORE, new ItemLore(List.of(Component.literal("Description"), Component.empty(),
                Component.literal("Click to use"))));
        return stack;
    }

    @Test void descriptionSwitchAndHiddenTooltipNeverMutateOrRevealServerComponents() {
        var stack = describedItem();
        var before = stack.copy();
        var settings = HeldItemInfoSettings.DEFAULT.withShowAdditional(false);
        assertEquals(List.of("Menu", "Description", "", "Click to use"),
                HeldItemInfoContent.collect(stack, null, Item.TooltipContext.EMPTY, settings).stream()
                        .map(line -> line.text().getString()).toList());
        assertEquals(List.of("Menu"), HeldItemInfoContent.collect(stack, null, Item.TooltipContext.EMPTY,
                settings.withShowDescription(false)).stream().map(line -> line.text().getString()).toList());
        assertTrue(ItemStack.isSameItemSameComponents(before, stack));
        stack.set(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT.withHidden(DataComponents.LORE, true));
        assertEquals(1, HeldItemInfoContent.collect(stack, null, Item.TooltipContext.EMPTY, settings).size());
        stack.set(DataComponents.TOOLTIP_DISPLAY, new TooltipDisplay(true, TooltipDisplay.DEFAULT.hiddenComponents()));
        assertTrue(HeldItemInfoContent.collect(stack, null, Item.TooltipContext.EMPTY, settings).isEmpty());
    }

    @Test void fullTooltipIncludesLoreOnceAndHonorsContentSwitches() {
        var stack = describedItem();
        var content = HeldItemInfoContent.collect(stack, null, Item.TooltipContext.EMPTY, HeldItemInfoSettings.DEFAULT);
        assertEquals(List.of("Menu", "Description", "", "Click to use"),
                content.stream().map(line -> line.text().getString()).toList());
        assertEquals(3, content.stream().filter(line -> line.kind() == HeldItemInfoContent.Kind.DESCRIPTION).count());
        var hidden = HeldItemInfoContent.collect(stack, null, Item.TooltipContext.EMPTY,
                HeldItemInfoSettings.DEFAULT.withShowName(false).withShowDescription(false));
        assertTrue(hidden.isEmpty());
        assertNotNull(stack.get(DataComponents.LORE));
    }

    @Test void identicalAddonTextDoesNotConsumeNativeDescriptionBudget() {
        var stack = describedItem();
        Consumer<ItemTooltipEvent> listener = event -> {
            if (event.getItemStack().is(Items.PAPER)) {
                event.getToolTip().add(1, stack.get(DataComponents.LORE).styledLines().getFirst().copy());
            }
        };
        NeoForge.EVENT_BUS.addListener(listener);
        try {
            var content = HeldItemInfoContent.collect(stack, null, Item.TooltipContext.EMPTY, HeldItemInfoSettings.DEFAULT);
            assertEquals(HeldItemInfoContent.Kind.OTHER, content.get(1).kind());
            assertEquals(HeldItemInfoContent.Kind.DESCRIPTION, content.get(2).kind());
            assertEquals(content.get(1).text(), content.get(2).text());
        } finally {
            NeoForge.EVENT_BUS.unregister(listener);
        }
    }

    @Test void countChangesDoNotTriggerButInPlaceLoreUpdatesDo() {
        var live = describedItem();
        var snapshot = live.copy();
        live.setCount(16);
        assertTrue(ItemStack.isSameItemSameComponents(snapshot, live));
        live.set(DataComponents.LORE, new ItemLore(List.of(Component.literal("Updated description"))));
        assertFalse(ItemStack.isSameItemSameComponents(snapshot, live));
    }
}
