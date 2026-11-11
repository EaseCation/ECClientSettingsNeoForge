package net.easecation.clientsettings.mixin.chat;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.authlib.GameProfile;
import net.easecation.clientsettings.feature.chat.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.chat.ChatListener;
import net.minecraft.network.chat.*;
import net.minecraft.Util;
import org.spongepowered.asm.mixin.*;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BooleanSupplier;

@Mixin(ChatListener.class)
abstract class ChatListenerMixin {
    @Shadow @Final private Minecraft minecraft;
    @Shadow private long messageDelay;
    @Shadow private long previousMessageTime;
    @Shadow private UUID guessChatUUID(Component message){throw new AssertionError();}
    @Unique private DeferredChatQueue ecSettings$queue;
    @Unique private UUID ecSettings$sender;
    @Unique private ClientPacketListener ecSettings$connection;
    @Unique private MessageSignature ecSettings$signature;
    @Unique private AsyncSocialAccess ecSettings$social(){return (AsyncSocialAccess)minecraft.getPlayerSocialManager();}
    @Unique private ClientPacketListener ecSettings$source(){
        ClientPacketListener current=minecraft.getConnection();
        if(current!=null)ecSettings$connection=current;
        else if(ecSettings$connection!=null&&!ecSettings$connection.getConnection().isConnected())ecSettings$connection=null;
        return ecSettings$connection;
    }
    @Unique private DeferredChatQueue ecSettings$queue(){
        if(ecSettings$queue==null)ecSettings$queue=new DeferredChatQueue(System::nanoTime,()->minecraft.gui.getChat().addMessage(
                Component.translatable("ecclientsettings.chat.filter_unavailable")));
        ClientPacketListener listener=ecSettings$source();
        ecSettings$queue.connection(listener!=null&&listener.getConnection().isConnected()?listener:null);
        return ecSettings$queue;
    }
    @Unique private void ecSettings$drain(){
        var list=ecSettings$social().ecSettings$blockList();list.pump();
        var queue=ecSettings$queue();
        if(minecraft.getConnection()==null)return;
        queue.drain(()->messageDelay>0&&Util.getMillis()<previousMessageTime+messageDelay);
    }
    @WrapMethod(method="handlePlayerChatMessage")
    private void ecSettings$player(PlayerChatMessage message,GameProfile profile,ChatType.Bound type,Operation<Void> original){
        UUID before=ecSettings$sender;MessageSignature signature=ecSettings$signature;
        ecSettings$sender=message.sender();ecSettings$signature=message.signature();
        try{original.call(message,profile,type);}finally{ecSettings$sender=before;ecSettings$signature=signature;}
    }
    @WrapMethod(method="handleChatMessageError")
    private void ecSettings$error(UUID sender,MessageSignature signature,ChatType.Bound type,Operation<Void> original){
        UUID before=ecSettings$sender;MessageSignature previous=ecSettings$signature;
        ecSettings$sender=sender;ecSettings$signature=signature;
        try{original.call(sender,signature,type);}finally{ecSettings$sender=before;ecSettings$signature=previous;}
    }
    @WrapMethod(method="handleDisguisedChatMessage")
    private void ecSettings$disguised(Component message,ChatType.Bound type,Operation<Void> original){
        UUID before=ecSettings$sender;MessageSignature signature=ecSettings$signature;
        ecSettings$sender=null;ecSettings$signature=null;
        try{original.call(message,type);}finally{ecSettings$sender=before;ecSettings$signature=signature;}
    }
    @WrapMethod(method="handleMessage")
    private void ecSettings$message(MessageSignature ignored,BooleanSupplier action,Operation<Void> original){
        ecSettings$enqueue(ecSettings$sender,ecSettings$signature,true,action);ecSettings$drain();
    }
    @Unique private void ecSettings$enqueue(UUID sender,MessageSignature signature,boolean delayable,BooleanSupplier action){
        var social=ecSettings$social();var source=ecSettings$source();
        ecSettings$queue().offer(signature,delayable,
                ()->sender==null?CompletableFuture.completedFuture(false):social.ecSettings$blockList().request(sender,true),blocked->{
            if(source!=minecraft.getConnection()||source!=null&&!source.getConnection().isConnected())return false;
            return sender==null?action.getAsBoolean():social.ecSettings$withAnswer(sender,blocked,action);
        },()->{
            if(signature!=null&&source!=null&&source==minecraft.getConnection()&&source.getConnection().isConnected())source.markMessageAsProcessed(signature,false);
        });
    }
    @WrapMethod(method="handleSystemMessage")
    private void ecSettings$system(Component message,boolean overlay,Operation<Void> original){
        // Resolve even when name filtering is currently off: the option can change
        // while an earlier message is waiting, and delivery must still have a result.
        UUID sender=guessChatUUID(message);
        ecSettings$enqueue(sender,null,false,()->{original.call(message,overlay);return true;});ecSettings$drain();
    }
    @WrapMethod(method="tick")
    private void ecSettings$tick(Operation<Void> original){ecSettings$drain();}
    @WrapMethod(method="queueSize")
    private long ecSettings$size(Operation<Long> original){return ecSettings$queue().size();}
    @WrapMethod(method="removeFromDelayedMessageQueue")
    private boolean ecSettings$delete(MessageSignature signature,Operation<Boolean> original){
        boolean removed=ecSettings$queue().delete(signature);ecSettings$drain();return removed;
    }
    @WrapMethod(method="acceptNextDelayedMessage")
    private void ecSettings$next(Operation<Void> original){ecSettings$queue().acceptNext();ecSettings$drain();}
    @WrapMethod(method="clearQueue")
    private void ecSettings$clear(Operation<Void> original){ecSettings$queue().flush();ecSettings$drain();}
    @WrapMethod(method="setMessageDelay")
    private void ecSettings$delay(double seconds,Operation<Void> original){
        long value=(long)(seconds*1000);if(value==0&&messageDelay>0)ecSettings$queue().flush();
        messageDelay=value;ecSettings$drain();
    }
}
