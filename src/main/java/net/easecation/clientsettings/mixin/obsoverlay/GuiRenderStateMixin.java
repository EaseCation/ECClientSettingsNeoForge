package net.easecation.clientsettings.mixin.obsoverlay;

import net.easecation.clientsettings.feature.obsoverlay.ObsOverlayRuntime;
import net.minecraft.client.gui.render.state.GuiElementRenderState;
import net.minecraft.client.gui.render.state.GuiItemRenderState;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.gui.render.state.GuiTextRenderState;
import net.minecraft.client.gui.render.state.pip.PictureInPictureRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiRenderState.class)
abstract class GuiRenderStateMixin {

    @Inject(method = "submitItem", at = @At("HEAD"), cancellable = true)
    private void ecclientsettings$routeItem(GuiItemRenderState state, CallbackInfo callback) {
        if (ObsOverlayRuntime.redirect((GuiRenderState) (Object) this, state)) {
            callback.cancel();
        }
    }

    @Inject(method = "submitText", at = @At("HEAD"), cancellable = true)
    private void ecclientsettings$routeText(GuiTextRenderState state, CallbackInfo callback) {
        if (ObsOverlayRuntime.redirect((GuiRenderState) (Object) this, state)) {
            callback.cancel();
        }
    }

    @Inject(method = "submitPicturesInPictureState", at = @At("HEAD"), cancellable = true)
    private void ecclientsettings$routePicture(PictureInPictureRenderState state, CallbackInfo callback) {
        if (ObsOverlayRuntime.redirect((GuiRenderState) (Object) this, state)) {
            callback.cancel();
        }
    }

    @Inject(method = "submitGuiElement", at = @At("HEAD"), cancellable = true)
    private void ecclientsettings$routeElement(GuiElementRenderState state, CallbackInfo callback) {
        if (ObsOverlayRuntime.redirect((GuiRenderState) (Object) this, state)) {
            callback.cancel();
        }
    }

    @Inject(method = "nextStratum", at = @At("HEAD"))
    private void ecclientsettings$routeStratum(CallbackInfo callback) {
        ObsOverlayRuntime.redirectNextStratum((GuiRenderState) (Object) this);
    }

    @Inject(method = "blurBeforeThisStratum", at = @At("HEAD"), cancellable = true)
    private void ecclientsettings$suppressCapturedBlur(CallbackInfo callback) {
        if (ObsOverlayRuntime.suppressBlur((GuiRenderState) (Object) this)) {
            callback.cancel();
        }
    }
}
