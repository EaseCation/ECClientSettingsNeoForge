package net.easecation.clientsettings.mixin.obsoverlay;

import com.mojang.blaze3d.vertex.PoseStack;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.easecation.clientsettings.feature.obsoverlay.ObsOverlayComponent;
import net.easecation.clientsettings.feature.obsoverlay.ObsOverlayRuntime;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.ChestRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ChestRenderer.class)
abstract class ChestRendererMixin {

    @WrapMethod(
            method = "render(Lnet/minecraft/world/level/block/entity/BlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/world/phys/Vec3;)V"
    )
    private void ecclientsettings$protectChest(
            BlockEntity chest,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            int packedOverlay,
            Vec3 cameraPosition,
            Operation<Void> original
    ) {
        if (!ObsOverlayRuntime.beginWorldComponent(ObsOverlayComponent.CHESTS, buffers)) {
            return;
        }
        try {
            original.call(chest, partialTick, poseStack, buffers, packedLight, packedOverlay, cameraPosition);
        } finally {
            ObsOverlayRuntime.endWorldComponent(buffers);
        }
    }
}
