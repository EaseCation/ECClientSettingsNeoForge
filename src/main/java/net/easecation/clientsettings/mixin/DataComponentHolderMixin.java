package net.easecation.clientsettings.mixin;

import net.easecation.clientsettings.combat.SwordBlockingComponents;
import net.easecation.clientsettings.config.ClientSettingsConfig;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DataComponentHolder.class)
public abstract class DataComponentHolderMixin {

    @Inject(method = "get", at = @At("RETURN"), cancellable = true)
    @SuppressWarnings("unchecked")
    private <T> void ecclientsettings$overrideSwordConsumable(DataComponentType<? extends T> type,
                                                               CallbackInfoReturnable<T> callback) {
        if (type != DataComponents.CONSUMABLE
                || !((Object) this instanceof ItemStack stack)
                || !SwordBlockingComponents.isSupportedSword(stack)) {
            return;
        }

        Consumable current = (Consumable) callback.getReturnValue();
        callback.setReturnValue((T) SwordBlockingComponents.overrideSwordConsumable(
                current,
                ClientSettingsConfig.swordBlockingAnimation()
        ));
    }
}
