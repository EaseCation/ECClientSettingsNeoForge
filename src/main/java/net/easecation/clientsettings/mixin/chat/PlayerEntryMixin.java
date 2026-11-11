package net.easecation.clientsettings.mixin.chat;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import net.easecation.clientsettings.feature.chat.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.social.*;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.List;
import java.util.UUID;

@Mixin(PlayerEntry.class)
abstract class PlayerEntryMixin {
    @Shadow @Final private Minecraft minecraft;
    @Shadow @Final private UUID id;
    @Shadow @Final private boolean reportingEnabled;
    @Shadow private Button hideButton;
    @Shadow private Button showButton;
    @Shadow private Button reportButton;
    @Shadow private void updateHideAndShowButton(boolean hidden){throw new AssertionError();}
    @Unique private AsyncBlockList.Status ecSettings$status(){
        return ((AsyncSocialAccess)minecraft.getPlayerSocialManager()).ecSettings$blockList().status(id);
    }
    // Build controls once, then gate both visibility and input using the authoritative state.
    @WrapOperation(method="<init>",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/screens/social/PlayerSocialManager;isBlocked(Ljava/util/UUID;)Z"))
    private boolean ecSettings$constructControls(PlayerSocialManager manager,UUID player,Operation<Boolean> original){return false;}
    @Inject(method="<init>",at=@At("RETURN"))
    private void ecSettings$initialState(CallbackInfo ci){ecSettings$buttons();}
    @Unique private void ecSettings$buttons(){
        var state=ecSettings$status();boolean allowed=state==AsyncBlockList.Status.ALLOWED;
        if(hideButton!=null&&showButton!=null&&reportButton!=null){
            updateHideAndShowButton(minecraft.getPlayerSocialManager().isHidden(id));
            hideButton.visible&=allowed;showButton.visible&=allowed;
            hideButton.active=showButton.active=allowed;reportButton.visible=allowed;reportButton.active=allowed&&reportingEnabled;
        }
    }
    @Inject(method="render",at=@At("HEAD"))
    private void ecSettings$refreshButtons(CallbackInfo ci){ecSettings$buttons();}
    @WrapMethod(method="getStatusComponent")
    private Component ecSettings$statusText(Operation<Component> original){
        var status=ecSettings$status();
        if(status==AsyncBlockList.Status.PENDING)return Component.translatable("ecclientsettings.chat.filter_pending");
        if(status==AsyncBlockList.Status.FAILED)return Component.translatable("ecclientsettings.chat.filter_failed");
        return original.call();
    }
    @WrapMethod(method={"children","narratables"})
    private List<?> ecSettings$activeChildren(Operation<List<?>> original){
        return ecSettings$status()==AsyncBlockList.Status.ALLOWED?original.call():List.of();
    }
}
