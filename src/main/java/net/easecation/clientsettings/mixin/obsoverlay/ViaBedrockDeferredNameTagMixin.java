package net.easecation.clientsettings.mixin.obsoverlay;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.easecation.clientsettings.feature.obsoverlay.ObsOverlayRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Splits VBU's deferred player names from its ordinary entity-name replay. */
@Pseudo
@Mixin(targets = "org.oryxel.viabedrockutility.renderer.DeferredNameTag", remap = false)
abstract class ViaBedrockDeferredNameTagMixin {

    @Inject(
            method = "enqueue(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;FFILorg/joml/Matrix4f;Lnet/minecraft/client/gui/Font$DisplayMode;II)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false,
            require = 1
    )
    private static void ecclientsettings$capturePlayerName(
            Font font,
            Component text,
            float x,
            float y,
            int color,
            Matrix4f pose,
            Font.DisplayMode displayMode,
            int backgroundColor,
            int packedLight,
            CallbackInfo callback
    ) {
        if (ObsOverlayRuntime.captureDeferredNameTag(
                font,
                text,
                x,
                y,
                color,
                pose,
                displayMode,
                backgroundColor,
                packedLight
        )) {
            callback.cancel();
        }
    }

    @WrapMethod(method = "flush()V", remap = false)
    private static void ecclientsettings$replayPlayerNamesAfterVbu(Operation<Void> original) {
        original.call();
        MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        ObsOverlayRuntime.replayDeferredPlayerNameTags(buffers);
    }
}
