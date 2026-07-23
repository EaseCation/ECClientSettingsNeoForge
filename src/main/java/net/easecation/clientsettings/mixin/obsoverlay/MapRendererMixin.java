package net.easecation.clientsettings.mixin.obsoverlay;

import com.mojang.blaze3d.vertex.PoseStack;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.easecation.clientsettings.feature.obsoverlay.ObsOverlayComponent;
import net.easecation.clientsettings.feature.obsoverlay.ObsOverlayRuntime;
import net.minecraft.client.renderer.MapRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.state.MapRenderState;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(MapRenderer.class)
abstract class MapRendererMixin {

    @WrapMethod(
            method = "render(Lnet/minecraft/client/renderer/state/MapRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ZI)V"
    )
    private void ecclientsettings$protectItemFrameMap(
            MapRenderState state,
            PoseStack poseStack,
            MultiBufferSource buffers,
            boolean active,
            int packedLight,
            Operation<Void> original
    ) {
        if (!active) {
            original.call(state, poseStack, buffers, false, packedLight);
            return;
        }
        if (!ObsOverlayRuntime.beginWorldComponent(ObsOverlayComponent.ITEM_FRAME_MAPS, buffers)) {
            return;
        }
        try {
            original.call(state, poseStack, buffers, true, packedLight);
        } finally {
            ObsOverlayRuntime.endWorldComponent(buffers);
        }
    }
}
