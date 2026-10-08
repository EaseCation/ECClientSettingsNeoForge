package net.easecation.clientsettings.mixin.render;

import net.easecation.clientsettings.feature.helditeminfo.HeldItemInfoRuntime;
import net.easecation.clientsettings.profile.runtime.ProfileServices;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
abstract class HeldItemInfoGuiMixin {
    @Inject(method = "tick()V", at = @At("TAIL"))
    private void ecclientsettings$tickHeldItemInfo(CallbackInfo ci) {
        HeldItemInfoRuntime.tick();
    }

    @Inject(method = "renderSelectedItemName(Lnet/minecraft/client/gui/GuiGraphics;I)V", at = @At("HEAD"), cancellable = true)
    private void ecclientsettings$heldItemInfo(GuiGraphics graphics, int yShift, CallbackInfo ci) {
        if (ProfileServices.active().features().heldItemInfo().enabled()) {
            HeldItemInfoRuntime.render(graphics, yShift);
            ci.cancel();
        }
    }
}
