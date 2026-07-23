package net.easecation.clientsettings.mixin.obsoverlay;

import com.mojang.blaze3d.vertex.PoseStack;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.easecation.clientsettings.feature.obsoverlay.ObsOverlayComponent;
import net.easecation.clientsettings.feature.obsoverlay.ObsOverlayRuntime;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.AbstractSignRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.SignText;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(AbstractSignRenderer.class)
abstract class AbstractSignRendererMixin {

    @WrapMethod(
            method = "renderSignText(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/SignText;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IIIZ)V"
    )
    private void ecclientsettings$protectSignText(
            BlockPos pos,
            SignText text,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int light,
            int lineHeight,
            int maxLineWidth,
            boolean front,
            Operation<Void> original
    ) {
        boolean restoreImmediatelyFastCache = ObsOverlayRuntime.suspendImmediatelyFastSignTextCache(text);
        try {
            if (!ObsOverlayRuntime.beginWorldComponent(ObsOverlayComponent.SIGN_TEXT, buffers)) {
                return;
            }
            try {
                original.call(pos, text, poseStack, buffers, light, lineHeight, maxLineWidth, front);
            } finally {
                ObsOverlayRuntime.endWorldComponent(buffers);
            }
        } finally {
            ObsOverlayRuntime.restoreImmediatelyFastSignTextCache(text, restoreImmediatelyFastCache);
        }
    }
}
