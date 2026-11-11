package net.easecation.clientsettings.mixin.chat;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.authlib.minecraft.UserApiService;
import net.easecation.clientsettings.feature.chat.*;
import net.minecraft.client.gui.screens.social.PlayerSocialManager;
import org.spongepowered.asm.mixin.*;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BooleanSupplier;

@Mixin(PlayerSocialManager.class)
abstract class PlayerSocialManagerMixin implements AsyncSocialAccess {
    @Shadow @Final private UserApiService service;
    @Shadow private boolean onlineMode;
    @Shadow private CompletableFuture<?> pendingBlockListRefresh;
    @Unique private AsyncBlockList ecSettings$list;
    @Unique private UUID ecSettings$answerPlayer;
    @Unique private boolean ecSettings$answer,ecSettings$socialView;
    @Override public AsyncBlockList ecSettings$blockList(){
        if(ecSettings$list==null)ecSettings$list=new AsyncBlockList(service::refreshBlockList,service::isBlockedPlayer,BlockListWorker.EXECUTOR,System::nanoTime);
        return ecSettings$list;
    }
    @WrapMethod(method="startOnlineMode")
    private void ecSettings$start(Operation<Void> original){
        onlineMode=true;pendingBlockListRefresh=ecSettings$blockList().start();
    }
    @WrapMethod(method="stopOnlineMode")
    private void ecSettings$stop(Operation<Void> original){onlineMode=false;ecSettings$blockList().stop();}
    @WrapMethod(method="isBlocked")
    private boolean ecSettings$blocked(UUID player,Operation<Boolean> original){
        if(!onlineMode)return false;
        if(player.equals(ecSettings$answerPlayer))return ecSettings$answer;
        var status=ecSettings$blockList().status(player);
        // Chat delivery gets a definite answer through withAnswer. Unknown external
        // boolean consumers fail closed; the social UI explicitly presents pending.
        return status==AsyncBlockList.Status.BLOCKED||!ecSettings$socialView&&status!=AsyncBlockList.Status.ALLOWED;
    }
    @Override public boolean ecSettings$withAnswer(UUID player,boolean blocked,BooleanSupplier action){
        UUID previous=ecSettings$answerPlayer;boolean answer=ecSettings$answer;
        ecSettings$answerPlayer=player;ecSettings$answer=blocked;
        try{return action.getAsBoolean();}finally{ecSettings$answerPlayer=previous;ecSettings$answer=answer;}
    }
    @Override public void ecSettings$withSocialView(Runnable action){
        boolean previous=ecSettings$socialView;ecSettings$socialView=true;
        try{action.run();}finally{ecSettings$socialView=previous;}
    }
}
