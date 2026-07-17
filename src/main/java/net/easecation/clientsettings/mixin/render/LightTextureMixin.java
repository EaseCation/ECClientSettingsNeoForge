package net.easecation.clientsettings.mixin.render;

import net.easecation.clientsettings.feature.fullbright.FullbrightController;
import net.easecation.clientsettings.profile.model.FullbrightSettings;
import net.easecation.clientsettings.profile.runtime.ProfileServices;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Slice;

@Mixin(LightTexture.class)
abstract class LightTextureMixin {

    @ModifyVariable(
            method = "updateLightTexture(F)V",
            at = @At("LOAD"),
            slice = @Slice(
                    from = @At(
                            value = "INVOKE",
                            target = "Lcom/mojang/blaze3d/buffers/Std140Builder;putInt(I)Lcom/mojang/blaze3d/buffers/Std140Builder;"
                    ),
                    to = @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/client/renderer/GameRenderer;getDarkenWorldAmount(F)F"
                    )
            ),
            index = 10,
            require = 1
    )
    private float ecclientsettings$fullbrightNightVision(float vanilla) {
        FullbrightSettings settings = ProfileServices.active().features().fullbright();
        return FullbrightController.effectiveNightVision(vanilla, settings);
    }

    @ModifyVariable(
            method = "updateLightTexture(F)V",
            at = @At("LOAD"),
            slice = @Slice(
                    from = @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/client/renderer/GameRenderer;getDarkenWorldAmount(F)F"
                    ),
                    to = @At(
                            value = "INVOKE",
                            target = "Ljava/lang/Math;max(FF)F"
                    )
            ),
            index = 15,
            require = 1
    )
    private float ecclientsettings$fullbrightGamma(float vanilla) {
        FullbrightSettings settings = ProfileServices.active().features().fullbright();
        return FullbrightController.effectiveGamma(vanilla, settings);
    }
}
