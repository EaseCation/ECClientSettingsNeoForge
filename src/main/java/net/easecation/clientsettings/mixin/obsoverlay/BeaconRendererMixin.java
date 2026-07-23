package net.easecation.clientsettings.mixin.obsoverlay;

import com.mojang.blaze3d.vertex.PoseStack;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.easecation.clientsettings.feature.obsoverlay.ObsOverlayComponent;
import net.easecation.clientsettings.feature.obsoverlay.ObsOverlayRuntime;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BeaconRenderer.class)
abstract class BeaconRendererMixin {

    @WrapMethod(
            method = "renderBeaconBeam(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/resources/ResourceLocation;FFJIIIFF)V"
    )
    private static void ecclientsettings$protectBeaconBeam(
            PoseStack poseStack,
            MultiBufferSource buffers,
            ResourceLocation texture,
            float partialTick,
            float heightScale,
            long gameTime,
            int yOffset,
            int height,
            int color,
            float innerRadius,
            float outerRadius,
            Operation<Void> original
    ) {
        if (!ObsOverlayRuntime.beginWorldComponent(ObsOverlayComponent.BEACON_BEAMS, buffers)) {
            return;
        }
        try {
            original.call(
                    poseStack, buffers, texture, partialTick, heightScale, gameTime,
                    yOffset, height, color, innerRadius, outerRadius
            );
        } finally {
            ObsOverlayRuntime.endWorldComponent(buffers);
        }
    }
}
