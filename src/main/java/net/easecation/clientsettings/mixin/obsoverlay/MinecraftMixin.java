package net.easecation.clientsettings.mixin.obsoverlay;

import net.easecation.clientsettings.feature.obsoverlay.ObsOverlayRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.main.GameConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
abstract class MinecraftMixin {

    @Inject(method = "<init>", at = @At("RETURN"))
    private void ecclientsettings$initializeObsOverlay(GameConfig config, CallbackInfo callback) {
        ObsOverlayRuntime.initialize((Minecraft) (Object) this);
    }
}
