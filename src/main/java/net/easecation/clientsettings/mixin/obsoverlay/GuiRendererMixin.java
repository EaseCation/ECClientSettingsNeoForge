package net.easecation.clientsettings.mixin.obsoverlay;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.easecation.clientsettings.feature.obsoverlay.ObsOverlayRuntime;
import net.minecraft.client.gui.render.GuiRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GuiRenderer.class)
abstract class GuiRendererMixin {

    @ModifyExpressionValue(
            method = "draw",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/Minecraft;getMainRenderTarget()Lcom/mojang/blaze3d/pipeline/RenderTarget;"
            )
    )
    private RenderTarget ecclientsettings$selectObsTarget(RenderTarget original) {
        return ObsOverlayRuntime.guiRenderTarget((GuiRenderer) (Object) this, original);
    }
}
