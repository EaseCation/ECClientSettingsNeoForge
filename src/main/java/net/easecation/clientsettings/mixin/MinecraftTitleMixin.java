package net.easecation.clientsettings.mixin;

import net.easecation.clientsettings.window.WindowAppearanceController;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class MinecraftTitleMixin {

    @Inject(method = "createTitle", at = @At("RETURN"), cancellable = true)
    private void ecclientsettings$useServerWindowTitle(CallbackInfoReturnable<String> callback) {
        String title = WindowAppearanceController.getInstance().titleOverride();
        if (title != null) {
            callback.setReturnValue(title);
        }
    }
}
