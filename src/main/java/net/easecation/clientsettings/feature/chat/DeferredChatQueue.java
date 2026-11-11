package net.easecation.clientsettings.feature.chat;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.*;

/** Owner-thread queue; removed messages keep ordered not-displayed acknowledgements. */
public final class DeferredChatQueue {
    private static final int CAPACITY=128, MAX_PER_TICK=16;
    private static final long MAX_WAIT=10_000_000_000L, BUDGET=2_000_000L;
    private static final class Entry {
        final Object key;final Runnable discard;final boolean delayable;
        long queryStarted;Supplier<CompletableFuture<Boolean>> query;
        CompletableFuture<Boolean> answer;Predicate<Boolean> deliver;
        boolean deleted;
        Entry(Object key,boolean delayable,Supplier<CompletableFuture<Boolean>> query,Predicate<Boolean> deliver,Runnable discard){
            this.key=key;this.delayable=delayable;this.query=query;this.deliver=deliver;this.discard=discard;
        }
        void delete(){deleted=true;answer=null;query=null;deliver=null;}
    }
    private final ArrayDeque<Entry> queue=new ArrayDeque<>();
    private final LongSupplier clock;
    private final Runnable failureNotice;
    private Object connection;
    private boolean draining,flush;
    private int manuallyAccepted;
    private long noticeAt=Long.MIN_VALUE;
    public DeferredChatQueue(LongSupplier clock,Runnable failureNotice){this.clock=clock;this.failureNotice=failureNotice;}
    public void connection(Object current){if(current!=connection){queue.clear();connection=current;flush=false;manuallyAccepted=0;noticeAt=Long.MIN_VALUE;}}
    public void offer(Object key,boolean delayable,CompletableFuture<Boolean> answer,Predicate<Boolean> deliver,Runnable discard){
        offer(key,delayable,()->answer,deliver,discard);
    }
    public void offer(Object key,boolean delayable,Supplier<CompletableFuture<Boolean>> query,Predicate<Boolean> deliver,Runnable discard){
        if(queue.size()>=CAPACITY){queue.removeFirst().discard.run();notice();}
        queue.addLast(new Entry(key,delayable,query,deliver,discard));
    }
    public boolean delete(Object key){
        if(key==null)return false;boolean found=false;
        for(Entry entry:queue)if(key.equals(entry.key)){entry.delete();found=true;}
        return found;
    }
    public void acceptNext(){if(!queue.isEmpty())manuallyAccepted=Math.min(CAPACITY,manuallyAccepted+1);}
    public void flush(){flush=true;}
    public void drain(BooleanSupplier delayActive){
        if(draining)return;draining=true;long start=clock.getAsLong();
        try{
            for(int i=0;i<MAX_PER_TICK&&!queue.isEmpty()&&(i==0||clock.getAsLong()-start<BUDGET);i++){
                Entry next=queue.peekFirst();
                if(next.deleted){queue.removeFirst();next.discard.run();continue;}
                if(next.answer==null){
                    if(next.delayable&&!flush&&manuallyAccepted==0&&delayActive.getAsBoolean())return;
                    next.queryStarted=clock.getAsLong();
                    try{next.answer=next.query.get();}catch(RuntimeException failure){next.answer=CompletableFuture.failedFuture(failure);}
                    next.query=null;
                }
                var status=AsyncBlockList.status(next.answer);
                if(status==AsyncBlockList.Status.PENDING&&clock.getAsLong()-next.queryStarted<MAX_WAIT)return;
                boolean failed=status==AsyncBlockList.Status.PENDING||status==AsyncBlockList.Status.FAILED;
                if(!failed&&next.delayable&&!flush&&manuallyAccepted==0&&delayActive.getAsBoolean())return;
                boolean manual=manuallyAccepted>0;queue.removeFirst();if(manual)manuallyAccepted--;
                if(!failed)next.deliver.test(status==AsyncBlockList.Status.BLOCKED);
                else{next.discard.run();notice();}
                if(manual&&!flush&&manuallyAccepted==0)return;
            }
        }finally{if(queue.isEmpty()){flush=false;manuallyAccepted=0;}draining=false;}
    }
    private void notice(){long now=clock.getAsLong();if(noticeAt==Long.MIN_VALUE||now-noticeAt>=30_000_000_000L){noticeAt=now;failureNotice.run();}}
    public int size(){return queue.size();}
}
