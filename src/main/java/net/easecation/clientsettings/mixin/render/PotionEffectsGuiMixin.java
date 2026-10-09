package net.easecation.clientsettings.mixin.render;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.easecation.clientsettings.feature.hud.potions.VanillaPotionOverlay;
import net.easecation.clientsettings.profile.runtime.ProfileServices;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.neoforge.client.extensions.common.IClientMobEffectExtensions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
abstract class PotionEffectsGuiMixin {
    @Inject(method = "renderEffects(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V",
            at = @At("HEAD"), cancellable = true)
    private void ecclientsettings$hideVanilla(GuiGraphics graphics, DeltaTracker delta, CallbackInfo ci) {
        if (ProfileServices.active().features().hud().potions().hideVanilla()) ci.cancel();
    }

    @Redirect(method = "renderEffects(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/effect/MobEffectInstance;endsWithin(I)Z"), require = 1)
    private boolean ecclientsettings$replaceVanillaWarning(MobEffectInstance effect, int ignored) {
        // Shared warning below owns opacity, including disabling vanilla's fixed 10-second blink.
        return false;
    }

    @WrapOperation(method = "renderEffects(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V",
            at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforge/client/extensions/common/IClientMobEffectExtensions;renderGuiIcon(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/client/gui/Gui;Lnet/minecraft/client/gui/GuiGraphics;IIFF)Z"), require = 1)
    private boolean ecclientsettings$effectTile(IClientMobEffectExtensions renderer, MobEffectInstance effect,
            Gui gui, GuiGraphics graphics, int x, int y, float z, float vanillaAlpha, Operation<Boolean> original) {
        var settings = ProfileServices.active().features().hud().potions();
        float alpha = VanillaPotionOverlay.opacity(effect, settings);
        if (!original.call(renderer, effect, gui, graphics, x, y, z, alpha)) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, Gui.getMobEffectSprite(effect.getEffect()),
                    x + 3, y + 3, 18, 18, alpha);
        }
        VanillaPotionOverlay.draw(graphics, effect, x, y, alpha, settings);
        // Icon was rendered here (native or extension); skip only the original fallback draw.
        return true;
    }
}
