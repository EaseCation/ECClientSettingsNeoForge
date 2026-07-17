package net.easecation.clientsettings.mixin.client;

import net.easecation.clientsettings.feature.timechanger.ClientTimeValues;
import net.easecation.clientsettings.feature.timechanger.TimeChangerRuntime;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(ClientPacketListener.class)
abstract class ClientPacketListenerMixin {

    @Shadow
    private ClientLevel level;

    @ModifyArgs(
            method = "handleSetTime(Lnet/minecraft/network/protocol/game/ClientboundSetTimePacket;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/ClientLevel;setTimeFromServer(JJZ)V"
            ),
            require = 1
    )
    private void ecclientsettings$changeVisibleTime(Args arguments) {
        ClientTimeValues values = TimeChangerRuntime.onServerTime(
                level,
                arguments.get(0),
                arguments.get(1),
                arguments.get(2)
        );
        arguments.set(0, values.gameTime());
        arguments.set(1, values.dayTime());
        arguments.set(2, values.tickDayTime());
    }
}
