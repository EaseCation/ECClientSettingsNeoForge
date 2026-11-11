package net.easecation.clientsettings.mixin.chat;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.easecation.clientsettings.feature.chat.AsyncSocialAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.gui.screens.social.SocialInteractionsPlayerList;
import net.minecraft.client.gui.screens.social.SocialInteractionsScreen;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(SocialInteractionsPlayerList.class)
abstract class SocialInteractionsPlayerListMixin {
    @WrapMethod(method="addPlayer")
    private void ecSettings$knownPlayers(PlayerInfo player,SocialInteractionsScreen.Page page,Operation<Void> original){
        ((AsyncSocialAccess)Minecraft.getInstance().getPlayerSocialManager()).ecSettings$withSocialView(()->original.call(player,page));
    }
}
