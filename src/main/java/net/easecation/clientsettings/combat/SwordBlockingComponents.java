package net.easecation.clientsettings.combat;

import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Consumable;

public final class SwordBlockingComponents {

    static final float VIA_BEDROCK_BLOCK_DURATION_SECONDS = 1_000_000F;
    private static final Consumable CLIENT_BLOCKING_COMPONENT = Consumable.builder()
            .consumeSeconds(VIA_BEDROCK_BLOCK_DURATION_SECONDS)
            .animation(ItemUseAnimation.BLOCK)
            .hasConsumeParticles(false)
            .build();

    private SwordBlockingComponents() {
    }

    public static Consumable overrideSwordConsumable(Consumable current, boolean animationEnabled) {
        if (animationEnabled) {
            return current == null || isViaBedrockBlockingComponent(current)
                    ? CLIENT_BLOCKING_COMPONENT
                    : current;
        }
        return isViaBedrockBlockingComponent(current) ? null : current;
    }

    public static boolean shouldPrioritizeOffhandBlock(boolean hasBlockingComponent,
                                                       boolean blockTarget,
                                                       boolean offhandBlock) {
        return hasBlockingComponent && blockTarget && offhandBlock;
    }

    public static boolean isSupportedSword(ItemStack stack) {
        return stack.is(Items.WOODEN_SWORD)
                || stack.is(Items.STONE_SWORD)
                || stack.is(Items.IRON_SWORD)
                || stack.is(Items.GOLDEN_SWORD)
                || stack.is(Items.DIAMOND_SWORD)
                || stack.is(Items.NETHERITE_SWORD);
    }

    public static boolean hasBlockingComponent(ItemStack stack) {
        return isSupportedSword(stack)
                && isViaBedrockBlockingComponent(stack.get(net.minecraft.core.component.DataComponents.CONSUMABLE));
    }

    static boolean isViaBedrockBlockingComponent(Consumable component) {
        return component != null
                && Float.compare(component.consumeSeconds(), VIA_BEDROCK_BLOCK_DURATION_SECONDS) == 0
                && component.animation() == ItemUseAnimation.BLOCK;
    }
}
