package net.easecation.clientsettings.mixin;

import net.easecation.clientsettings.config.ClientSettingsConfig;
import net.easecation.clientsettings.movement.SprintInputOverride;
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
        input.keyPresses = SprintInputOverride.apply(input.keyPresses, ClientSettingsConfig.forceSprint());
    }
}

