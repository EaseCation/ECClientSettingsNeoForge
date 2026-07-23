package net.easecation.clientsettings.mixin.obsoverlay;

import net.easecation.clientsettings.feature.obsoverlay.ObsOverlayRuntime;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
abstract class ScreenMixin {

    @Inject(method = "renderBlurredBackground", at = @At("HEAD"), cancellable = true)
    private void ecclientsettings$suppressCapturedBlur(GuiGraphics graphics, CallbackInfo callback) {
        if (ObsOverlayRuntime.suppressCapturedScreenBlur()) {
            callback.cancel();
        }
    }
}
