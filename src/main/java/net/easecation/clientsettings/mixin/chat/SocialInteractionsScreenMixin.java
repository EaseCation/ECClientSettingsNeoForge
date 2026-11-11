package net.easecation.clientsettings.mixin.chat;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import net.easecation.clientsettings.feature.chat.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.social.SocialInteractionsScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SocialInteractionsScreen.class)
abstract class SocialInteractionsScreenMixin {
    @Shadow private SocialInteractionsScreen.Page page;
    @Shadow private void showPage(SocialInteractionsScreen.Page page){throw new AssertionError();}
    @Unique private long ecSettings$revision=-1,ecSettings$nextRefresh;
    @Unique private AsyncSocialAccess ecSettings$social(){return (AsyncSocialAccess)Minecraft.getInstance().getPlayerSocialManager();}
    @WrapMethod(method="showPage")
    private void ecSettings$knownBlockedOnly(SocialInteractionsScreen.Page selected,Operation<Void> original){
        ecSettings$social().ecSettings$withSocialView(()->original.call(selected));
    }
    @Inject(method="render",at=@At("HEAD"))
    private void ecSettings$refreshKnownList(CallbackInfo ci){
        var mc=Minecraft.getInstance();if(mc.getConnection()==null||mc.player==null)return;
        long revision=ecSettings$social().ecSettings$blockList().revision(),now=System.nanoTime();
        if(page==SocialInteractionsScreen.Page.BLOCKED&&revision!=ecSettings$revision&&now-ecSettings$nextRefresh>=0){
            ecSettings$revision=revision;ecSettings$nextRefresh=now+250_000_000L;showPage(page);
        }
    }
    @Unique private Component ecSettings$pendingLabel(Component label){
        if(page==SocialInteractionsScreen.Page.BLOCKED&&ecSettings$social().ecSettings$blockList().pendingQueries()>0)
            return Component.translatable("ecclientsettings.chat.filter_pending");
        if(page==SocialInteractionsScreen.Page.BLOCKED&&ecSettings$social().ecSettings$blockList().unavailable())
            return Component.translatable("ecclientsettings.chat.filter_failed");
        return label;
    }
    @WrapOperation(method="render",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/GuiGraphics;drawCenteredString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V"))
    private void ecSettings$emptyStatus(GuiGraphics gui,Font font,Component text,int x,int y,int color,Operation<Void> original){
        original.call(gui,font,ecSettings$pendingLabel(text),x,y,color);
    }
    @WrapOperation(method="showPage",at=@At(value="INVOKE",target="Lnet/minecraft/client/GameNarrator;saySystemNow(Lnet/minecraft/network/chat/Component;)V"))
    private void ecSettings$narrateStatus(GameNarrator narrator,Component text,Operation<Void> original){original.call(narrator,ecSettings$pendingLabel(text));}
}
