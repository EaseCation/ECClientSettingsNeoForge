package net.easecation.clientsettings.mixin.obsoverlay;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.systems.RenderSystem;
import net.easecation.clientsettings.feature.obsoverlay.ObsOverlayRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(RenderSystem.class)
abstract class RenderSystemMixin {

    @WrapOperation(
            method = "flipFrame",
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/lwjgl/glfw/GLFW;glfwSwapBuffers(J)V",
                    remap = false
            )
    )
    private static void ecclientsettings$preparePublicObsFrame(
            long window,
            Operation<Void> original
    ) {
        if (ObsOverlayRuntime.preparePublicFrameForCapture()) {
            original.call(window);
        }
    }
}
