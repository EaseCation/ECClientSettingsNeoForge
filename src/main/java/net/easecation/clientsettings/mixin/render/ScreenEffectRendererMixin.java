package net.easecation.clientsettings.mixin.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.easecation.clientsettings.feature.lowfire.LowFireTransform;
import net.easecation.clientsettings.profile.model.LowFireSettings;
import net.easecation.clientsettings.profile.runtime.ProfileServices;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ScreenEffectRenderer.class)
abstract class ScreenEffectRendererMixin {

    @Shadow
    private static void renderFire(PoseStack poseStack, MultiBufferSource bufferSource) {
        throw new AssertionError();
    }

    @Redirect(
            method = "renderScreenEffect",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/ScreenEffectRenderer;renderFire(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;)V"
            ),
            require = 1
    )
    private void ecclientsettings$renderLowFire(PoseStack poseStack, MultiBufferSource bufferSource) {
        LowFireSettings settings = ProfileServices.active().features().lowFire();
        LowFireTransform.render(settings, new LowFireTransform.TransformStack() {
            @Override
            public void push() {
                poseStack.pushPose();
            }

            @Override
            public void translateY(double offset) {
                poseStack.translate(0.0, offset, 0.0);
            }

            @Override
            public void pop() {
                poseStack.popPose();
            }
        }, () -> renderFire(poseStack, bufferSource));
    }
}
