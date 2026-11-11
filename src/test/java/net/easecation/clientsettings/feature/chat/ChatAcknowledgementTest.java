package net.easecation.clientsettings.feature.chat;

import net.minecraft.network.chat.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class ChatAcknowledgementTest {
    private MessageSignature signature(int value){byte[] b=new byte[256];b[0]=(byte)value;b[1]=(byte)(value>>8);return new MessageSignature(b);}
    @Test void deletedPendingMessageIsAcknowledgedNotDisplayedInOriginalOrder()throws Exception {
        var tracker=new LastSeenMessagesTracker(20);var server=new LastSeenMessagesValidator(20);
        var q=new DeferredChatQueue(()->0,()->fail());var pending=new CompletableFuture<Boolean>();
        var first=signature(1);var second=signature(2);server.addPending(first);server.addPending(second);
        q.offer(first,true,pending,blocked->{tracker.addPending(first,!blocked);return !blocked;},()->tracker.addPending(first,false));
        q.offer(second,true,CompletableFuture.completedFuture(false),blocked->{fail("Deleted message displayed");return false;},()->tracker.addPending(second,false));
        q.delete(second);q.drain(()->false);assertEquals(0,tracker.offset());
        pending.complete(false);q.drain(()->false);
        assertEquals(List.of(first),server.applyUpdate(tracker.generateAndApplyUpdate().update()).entries());
    }
    @Test void overloadMaintainsAckOrderAndNeverClaimsUnknownMessagesSeen()throws Exception {
        var tracker=new LastSeenMessagesTracker(20);var server=new LastSeenMessagesValidator(20);
        var q=new DeferredChatQueue(()->0,()->{});
        for(int i=0;i<150;i++){
            var signature=signature(i);server.addPending(signature);
            q.offer(signature,true,new CompletableFuture<>(),blocked->{fail();return false;},()->tracker.addPending(signature,false));
        }
        assertEquals(22,tracker.offset());assertTrue(server.applyUpdate(tracker.generateAndApplyUpdate().update()).entries().isEmpty());
    }
}
