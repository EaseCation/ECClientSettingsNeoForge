package net.easecation.clientsettings.mixin;

import net.easecation.clientsettings.combat.SwordBlockingComponents;
import net.easecation.clientsettings.config.ClientSettingsConfig;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Item.class)
public abstract class ItemMixin {

    @Redirect(
            method = {"use", "finishUsingItem", "getUseAnimation", "getUseDuration"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;get(Lnet/minecraft/core/component/DataComponentType;)Ljava/lang/Object;"
            )
    )
    @SuppressWarnings("unchecked")
    private <T> T ecclientsettings$overrideSwordConsumable(ItemStack stack,
                                                            DataComponentType<? extends T> type) {
        T current = stack.get(type);
        if (type != DataComponents.CONSUMABLE || !SwordBlockingComponents.isSupportedSword(stack)) {
            return current;
        }
        return (T) SwordBlockingComponents.overrideSwordConsumable(
                (Consumable) current,
                ClientSettingsConfig.swordBlockingAnimation()
        );
    }
}
