package net.easecation.clientsettings.mixin.obsoverlay;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.easecation.clientsettings.feature.obsoverlay.ObsOverlayRuntime;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.neoforged.neoforge.client.gui.PictureInPictureRendererRegistration;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(GameRenderer.class)
abstract class GameRendererMixin {

    @Shadow
    @Final
    private GuiRenderState guiRenderState;

    @Shadow
    @Final
    private FogRenderer fogRenderer;

    @ModifyExpressionValue(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/neoforged/neoforge/client/ClientHooks;gatherPictureInPictureRenderers(Ljava/util/List;)Ljava/util/List;"
            )
    )
    private List<PictureInPictureRendererRegistration<?>> ecclientsettings$capturePictureInPictureRenderers(
            List<PictureInPictureRendererRegistration<?>> registrations
    ) {
        ObsOverlayRuntime.capturePictureInPictureRenderers(registrations);
        return registrations;
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void ecclientsettings$beginObsFrame(DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo callback) {
        ObsOverlayRuntime.beginFrame(guiRenderState);
    }

    @Inject(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/systems/CommandEncoder;clearDepthTexture(Lcom/mojang/blaze3d/textures/GpuTexture;D)V",
                    shift = At.Shift.BEFORE
            )
    )
    private void ecclientsettings$backupSceneDepth(
            DeltaTracker deltaTracker,
            boolean renderLevel,
            CallbackInfo callback
    ) {
        ObsOverlayRuntime.backupSceneDepth();
    }

    @WrapOperation(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/neoforged/neoforge/client/ClientHooks;drawScreen(Lnet/minecraft/client/gui/screens/Screen;Lnet/minecraft/client/gui/GuiGraphics;IIF)V"
            )
    )
    private void ecclientsettings$protectObsScreen(
            Screen screen,
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick,
            Operation<Void> original
    ) {
        ObsOverlayRuntime.beginScreen(screen);
        try {
            original.call(screen, graphics, mouseX, mouseY, partialTick);
        } finally {
            ObsOverlayRuntime.endScreen();
        }
    }

    @Inject(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/render/GuiRenderer;incrementFrameNumber()V",
                    shift = At.Shift.BEFORE
            )
    )
    private void ecclientsettings$renderObsGui(DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo callback) {
        ObsOverlayRuntime.renderGuiOverlay(fogRenderer.getBuffer(FogRenderer.FogMode.NONE));
    }
}
