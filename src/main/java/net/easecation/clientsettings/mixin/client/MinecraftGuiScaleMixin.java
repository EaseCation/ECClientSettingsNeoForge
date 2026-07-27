package net.easecation.clientsettings.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.easecation.clientsettings.feature.gui.ComfortableGuiScale;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Minecraft.class)
public abstract class MinecraftGuiScaleMixin {
    @ModifyExpressionValue(
            method = "resizeDisplay",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/platform/Window;calculateScale(IZ)I"
            )
    )
    private int ecclientsettings$useComfortableAutomaticScale(int vanillaScale) {
        Minecraft minecraft = (Minecraft) (Object) this;
        return ComfortableGuiScale.resolve(
                minecraft.options.guiScale().get(),
                minecraft.getWindow().getWidth(),
                minecraft.getWindow().getHeight(),
                vanillaScale
        );
    }
}
