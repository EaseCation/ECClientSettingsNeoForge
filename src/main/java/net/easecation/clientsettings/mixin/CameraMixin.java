package net.easecation.clientsettings.mixin;

import net.easecation.clientsettings.config.ClientSettingsConfig;
import net.easecation.clientsettings.movement.SneakCameraTransition;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
abstract class CameraMixin {

    @Shadow
    private float eyeHeight;

    @Shadow
    private float eyeHeightOld;

    @Inject(method = "setup", at = @At("HEAD"))
    private void ecclientsettings$snapSneakCamera(
            BlockGetter level,
            Entity cameraEntity,
            boolean detached,
            boolean mirrored,
            float partialTick,
            CallbackInfo callbackInfo
    ) {
        boolean localPlayerCamera = cameraEntity == Minecraft.getInstance().player;
        if (!SneakCameraTransition.shouldSnap(ClientSettingsConfig.sneakAnimation(), localPlayerCamera)) {
            return;
        }

        float actualEyeHeight = cameraEntity.getEyeHeight();
        eyeHeight = actualEyeHeight;
        eyeHeightOld = actualEyeHeight;
    }
}
