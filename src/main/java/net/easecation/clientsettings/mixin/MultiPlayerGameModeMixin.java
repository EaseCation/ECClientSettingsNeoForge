package net.easecation.clientsettings.mixin;

import net.easecation.clientsettings.combat.SwordBlockingComponents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeMixin {

    @Inject(method = "useItem", at = @At("HEAD"), cancellable = true)
    private void ecclientsettings$prioritizeOffhandBlock(Player player,
                                                         InteractionHand hand,
                                                         CallbackInfoReturnable<InteractionResult> callback) {
        if (hand != InteractionHand.MAIN_HAND) {
            return;
        }

        ItemStack mainHand = player.getMainHandItem();
        boolean hasBlockingComponent = SwordBlockingComponents.hasBlockingComponent(mainHand);
        boolean blockTarget = Minecraft.getInstance().hitResult instanceof BlockHitResult;
        boolean offhandBlock = player.getOffhandItem().getItem() instanceof BlockItem;
        if (SwordBlockingComponents.shouldPrioritizeOffhandBlock(
                hasBlockingComponent,
                blockTarget,
                offhandBlock
        )) {
            callback.setReturnValue(InteractionResult.PASS);
        }
    }
}
