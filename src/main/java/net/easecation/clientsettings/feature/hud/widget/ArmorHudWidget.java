package net.easecation.clientsettings.feature.hud.widget;

import net.easecation.clientsettings.feature.hud.HudRenderContext;
import net.easecation.clientsettings.feature.hud.HudSize;
import net.easecation.clientsettings.feature.hud.HudTextRenderer;
import net.easecation.clientsettings.feature.hud.HudWidget;
import net.easecation.clientsettings.profile.model.HudWidgetId;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

public final class ArmorHudWidget implements HudWidget {

    private static final int ROW_HEIGHT = 21;
    private static final int WIDTH = 54;
    private static final int ICON_X = 36;
    private static final int BAR_WIDTH = 16;
    private static final HudSize PREVIEW_SIZE = new HudSize(WIDTH, ROW_HEIGHT * 4);
    private static final List<EquipmentSlot> SLOTS = List.of(
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
    );
    @Override
    public HudWidgetId id() {
        return HudWidgetId.ARMOR;
    }

    @Override
    public HudSize previewSize() {
        return PREVIEW_SIZE;
    }

    @Override
    public HudSize measure(HudRenderContext context) {
        int rows = Math.max(1, currentArmor(context).size());
        return new HudSize(WIDTH, rows * ROW_HEIGHT);
    }

    @Override
    public boolean shouldRender(HudRenderContext context) {
        return context.preview() || context.player() != null && !currentArmor(context).isEmpty();
    }

    @Override
    public void render(HudRenderContext context, HudSize size) {
        List<ItemStack> armor = context.preview() ? previewArmor() : currentArmor(context);
        for (int index = 0; index < armor.size(); index++) {
            renderSlot(context, armor.get(index), index, size.width());
        }
    }

    private static void renderSlot(HudRenderContext context, ItemStack stack, int index, int width) {
        int rowY = index * ROW_HEIGHT;
        if (index > 0 && context.style().borderEnabled()) {
            context.graphics().fill(0, rowY, width, rowY + 1, context.style().borderColor().value());
        }
        if (stack.isEmpty()) {
            return;
        }

        context.graphics().renderItem(stack, ICON_X, rowY + 1);
        int percent = durabilityPercent(stack);
        int color = durabilityColor(percent);
        String text = percent + "%";
        int textX = 31 - context.font().width(text);
        HudTextRenderer.draw(context, text, Math.max(1, textX), rowY + 6);

        int barY = rowY + 18;
        context.graphics().fill(ICON_X, barY, ICON_X + BAR_WIDTH, barY + 2, 0xFF303030);
        int filled = Math.round(BAR_WIDTH * percent / 100.0F);
        if (filled > 0) {
            context.graphics().fill(ICON_X, barY, ICON_X + filled, barY + 2, color);
        }
    }

    private static List<ItemStack> currentArmor(HudRenderContext context) {
        if (context.player() == null) {
            return List.of();
        }
        return SLOTS.stream()
                .map(context.player()::getItemBySlot)
                .filter(stack -> !stack.isEmpty())
                .toList();
    }

    private static List<ItemStack> previewArmor() {
        return List.of(
                previewStack(new ItemStack(Items.DIAMOND_HELMET), 85),
                previewStack(new ItemStack(Items.DIAMOND_CHESTPLATE), 58),
                previewStack(new ItemStack(Items.DIAMOND_LEGGINGS), 30),
                previewStack(new ItemStack(Items.DIAMOND_BOOTS), 12)
        );
    }

    private static ItemStack previewStack(ItemStack stack, int remainingPercent) {
        int damage = Math.round(stack.getMaxDamage() * (100 - remainingPercent) / 100.0F);
        stack.setDamageValue(Math.min(stack.getMaxDamage() - 1, damage));
        return stack;
    }

    static int durabilityPercent(ItemStack stack) {
        if (!stack.isDamageableItem() || stack.getMaxDamage() <= 0) {
            return 100;
        }
        int remaining = Math.max(0, stack.getMaxDamage() - stack.getDamageValue());
        return Math.round(remaining * 100.0F / stack.getMaxDamage());
    }

    static int durabilityColor(int percent) {
        if (percent >= 60) {
            return 0xFF55FF55;
        }
        return percent >= 25 ? 0xFFFFFF55 : 0xFFFF5555;
    }
}
