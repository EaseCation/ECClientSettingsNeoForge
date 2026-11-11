package net.easecation.clientsettings.feature.chat;

import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import static org.junit.jupiter.api.Assertions.*;

class DeferredChatQueueTest {
    @Test void unknownHeadPreservesOrderAndDeletionUsesOrderedDiscard(){
        var now=new AtomicLong();var calls=new ArrayList<String>();var q=new DeferredChatQueue(now::get,()->{});q.connection(new Object());
        var pending=new CompletableFuture<Boolean>();
        q.offer("first",true,pending,b->{calls.add("first:"+b);return !b;},()->calls.add("first:discard"));
        q.offer("second",true,CompletableFuture.completedFuture(false),b->{fail();return false;},()->calls.add("second:discard"));
        assertTrue(q.delete("second"));q.drain(()->false);assertTrue(calls.isEmpty());
        pending.complete(true);q.drain(()->false);assertEquals(List.of("first:true","second:discard"),calls);
    }
    @Test void networkPendingCannotBeBypassedByFlushOrManualAcceptance(){
        var q=new DeferredChatQueue(()->0,()->{});var calls=new AtomicInteger();var answer=new CompletableFuture<Boolean>();
        q.offer(null,true,answer,b->{calls.incrementAndGet();return true;},()->fail());q.flush();q.acceptNext();q.drain(()->true);assertEquals(0,calls.get());
        answer.complete(false);q.drain(()->true);assertEquals(1,calls.get());
    }
    @Test void delayIsRecheckedAfterEachShownMessageAndSystemFollowsFifo(){
        var waiting=new AtomicBoolean(true);var calls=new ArrayList<String>();var q=new DeferredChatQueue(()->0,()->{});
        q.offer(null,true,CompletableFuture.completedFuture(false),b->{calls.add("player");waiting.set(true);return true;},()->fail());
        q.offer(null,false,CompletableFuture.completedFuture(false),b->{calls.add("system");return true;},()->fail());
        q.drain(waiting::get);assertTrue(calls.isEmpty());waiting.set(false);q.drain(waiting::get);assertEquals(List.of("player","system"),calls);
    }
    @Test void timeoutAndOverflowDoNotFailOpenOrLoseAcknowledgementOrder(){
        var now=new AtomicLong();var notice=new AtomicInteger();var discarded=new ArrayList<Integer>();var q=new DeferredChatQueue(now::get,notice::incrementAndGet);
        for(int i=0;i<130;i++){int id=i;q.offer(id,true,new CompletableFuture<>(),b->{fail();return false;},()->discarded.add(id));}
        assertEquals(128,q.size());assertEquals(List.of(0,1),discarded);assertEquals(1,notice.get());
        q.flush();q.drain(()->false);
        for(int i=0;i<128;i++){now.addAndGet(11_000_000_000L);q.drain(()->false);}
        assertEquals(0,q.size());assertEquals(130,discarded.size());for(int i=0;i<130;i++)assertEquals(i,discarded.get(i));assertTrue(notice.get()<130);
    }
    @Test void connectionReplacementDiscardsOldCallbacksWithoutAcknowledgingNewConnection(){
        var q=new DeferredChatQueue(()->0,()->fail());q.connection(new Object());
        q.offer("old",true,CompletableFuture.completedFuture(false),b->{fail();return false;},()->fail());q.connection(new Object());q.drain(()->false);assertEquals(0,q.size());
    }
    @Test void largeReadyBatchIsBoundedPerDrain(){
        var count=new AtomicInteger();var q=new DeferredChatQueue(()->0,()->{});
        for(int i=0;i<128;i++)q.offer(i,true,CompletableFuture.completedFuture(false),b->{count.incrementAndGet();return true;},()->{});
        q.drain(()->false);assertEquals(16,count.get());assertEquals(112,q.size());
    }
    @Test void intentionalChatDelayDoesNotSnapshotBlockDecisionBeforeItIsDue(){
        var now=new AtomicLong();var waiting=new AtomicBoolean(true);var queries=new AtomicInteger();var answer=new AtomicBoolean();
        var q=new DeferredChatQueue(now::get,()->fail());
        q.offer(null,true,()->{queries.incrementAndGet();return CompletableFuture.completedFuture(answer.get());},blocked->{assertTrue(blocked);return false;},()->fail());
        q.drain(waiting::get);now.set(60_000_000_000L);answer.set(true);q.drain(waiting::get);assertEquals(0,queries.get());
        waiting.set(false);q.drain(waiting::get);assertEquals(1,queries.get());assertEquals(0,q.size());
    }
    @Test void manualNextConsumesExactlyOneEvenWhenItIsFiltered(){
        var count=new AtomicInteger();var q=new DeferredChatQueue(()->0,()->fail());
        for(int i=0;i<2;i++)q.offer(i,true,CompletableFuture.completedFuture(true),b->{count.incrementAndGet();return false;},()->fail());
        q.acceptNext();q.drain(()->false);assertEquals(1,count.get());assertEquals(1,q.size());
    }
}
