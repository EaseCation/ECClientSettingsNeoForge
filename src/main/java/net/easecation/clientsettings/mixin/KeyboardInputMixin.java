package net.easecation.clientsettings.mixin;

import net.easecation.clientsettings.movement.SprintInputOverride;
import net.easecation.clientsettings.profile.runtime.ProfileServices;
import net.minecraft.client.player.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
abstract class KeyboardInputMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void ecclientsettings$forceSprint(CallbackInfo callbackInfo) {
        KeyboardInput input = (KeyboardInput) (Object) this;
        boolean forceSprint = ProfileServices.active().features().forceSprint().enabled();
        input.keyPresses = SprintInputOverride.apply(input.keyPresses, forceSprint);
    }
}
