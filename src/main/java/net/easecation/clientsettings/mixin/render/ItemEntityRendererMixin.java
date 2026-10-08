package net.easecation.clientsettings.mixin.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.easecation.clientsettings.feature.droppeditems.DroppedItemPose;
import net.easecation.clientsettings.feature.droppeditems.DroppedItemRenderState;
import net.easecation.clientsettings.profile.model.DroppedItemSettings;
import net.easecation.clientsettings.profile.runtime.ProfileServices;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntityRenderer.class)
abstract class ItemEntityRendererMixin {
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/item/ItemEntity;Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;F)V", at = @At("TAIL"))
    private void ecclientsettings$captureGround(ItemEntity entity, ItemEntityRenderState state,
            float partialTick, CallbackInfo ci) {
        ((DroppedItemRenderState) state).ecclientsettings$setOnGround(entity.onGround());
    }

    @Redirect(method = "render(Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V"), require = 1)
    private void ecclientsettings$height(PoseStack stack, float x, float vanillaY, float z,
            ItemEntityRenderState state, PoseStack renderStack, MultiBufferSource bufferSource, int packedLight) {
        DroppedItemSettings settings = ProfileServices.active().features().droppedItems();
        if (settings.equals(DroppedItemSettings.DEFAULT)) {
            stack.translate(x, vanillaY, z);
            return;
        }
        AABB bounds = state.item.getModelBoundingBox();
        DroppedItemPose pose = DroppedItemPose.resolve(settings,
                ((DroppedItemRenderState) state).ecclientsettings$isOnGround(), state.ageInTicks, state.bobOffset);
        double halfDepth = bounds.getZsize() <= 0.0625 ? bounds.getZsize() * 0.75 * Math.max(0, state.count - 1) : 0;
        double y = pose.lift(bounds.minY, bounds.maxY, bounds.minZ, bounds.maxZ, halfDepth);
        if (settings.physics() && state.count > 1 && state.shouldSpread) {
            // Preserve vanilla bundle offsets while keeping every copy above the surface.
            y += bounds.getZsize() > 0.0625
                    ? 0.15 * (Math.abs(Math.cos(pose.pitch())) + Math.abs(Math.sin(pose.pitch())))
                    : 0.075 * Math.abs(Math.cos(pose.pitch()));
        }
        if (settings.floating() && state.shouldBob) {
            y += net.minecraft.util.Mth.sin(state.ageInTicks / 10.0F + state.bobOffset) * 0.1F + 0.1F;
        }
        stack.translate((double) x, y, (double) z);
    }

    @Redirect(method = "render(Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;mulPose(Lorg/joml/Quaternionfc;)V"), require = 1)
    private void ecclientsettings$orientation(PoseStack stack, Quaternionfc vanilla, ItemEntityRenderState state,
            PoseStack renderStack, MultiBufferSource bufferSource, int packedLight) {
        DroppedItemSettings settings = ProfileServices.active().features().droppedItems();
        if (!settings.physics() && settings.rotation()) {
            stack.mulPose(vanilla);
            return;
        }
        DroppedItemPose pose = DroppedItemPose.resolve(settings,
                ((DroppedItemRenderState) state).ecclientsettings$isOnGround(), state.ageInTicks, state.bobOffset);
        stack.mulPose(new Quaternionf().rotationY((float) pose.yaw()).rotateX((float) pose.pitch()));
    }
}
