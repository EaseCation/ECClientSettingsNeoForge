package net.easecation.clientsettings.mixin;

import net.easecation.clientsettings.feature.performance.RenderDistancePreferenceTracker;
import net.minecraft.client.OptionInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(OptionInstance.class)
public abstract class OptionInstanceMixin<T> {
    @Inject(method = "set", at = @At("HEAD"))
    private void ecclientsettings$trackRenderDistanceChange(T value, CallbackInfo ci) {
        RenderDistancePreferenceTracker.onOptionSet((OptionInstance<?>) (Object) this, value);
    }
}
