package net.easecation.clientsettings.feature.chat;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.LongSupplier;
import java.util.function.Predicate;

/** Owner-thread state; only the service calls run on the supplied serial worker. */
public final class AsyncBlockList {
    public enum Status { ALLOWED, BLOCKED, PENDING, FAILED }
    private static final long TIMEOUT=10_000_000_000L, UI_TTL=30_000_000_000L, RETRY=5_000_000_000L;
    private static final int MAX_KEYS=512, MAX_PENDING=256;
    private record Request(long epoch,UUID player,CompletableFuture<Boolean> answer,long submitted) { }
    private record Cached(CompletableFuture<Boolean> answer,long created) { }
    private final Runnable refresh;
    private final Predicate<UUID> lookup;
    private final Executor worker;
    private final LongSupplier clock;
    private final LinkedHashMap<UUID,Cached> cache=new LinkedHashMap<>();
    private final ArrayDeque<Request> pending=new ArrayDeque<>();
    private Request running;
    private CompletableFuture<Boolean> work;
    private CompletableFuture<Boolean> refreshAnswer=CompletableFuture.completedFuture(false);
    private long epoch,revision,retryAt;
    private boolean online;

    public AsyncBlockList(Runnable refresh,Predicate<UUID> lookup,Executor worker,LongSupplier clock){
        this.refresh=refresh;this.lookup=lookup;this.worker=worker;this.clock=clock;
    }
    public long epoch(){return epoch;}
    public long revision(){pump();return revision;}
    public boolean online(){return online;}
    public CompletableFuture<Void> start(){
        reset();online=true;enqueueRefresh();pump();
        return refreshAnswer.thenApply(ignored->null);
    }
    public void stop(){reset();online=false;}
    private void reset(){
        epoch++;revision++;
        cache.values().forEach(c->c.answer.cancel(false));cache.clear();
        pending.forEach(r->r.answer.cancel(false));pending.clear();refreshAnswer.cancel(false);
        // A running service call keeps its slot until it really exits. Reconnects never
        // create a second concurrent caller or apply its stale answer to the new epoch.
    }
    private void enqueueRefresh(){
        refreshAnswer=new CompletableFuture<>();retryAt=clock.getAsLong()+RETRY;
        pending.addFirst(new Request(epoch,null,refreshAnswer,clock.getAsLong()));
    }
    public CompletableFuture<Boolean> request(UUID player,boolean fresh){
        pump();
        if(!online)return CompletableFuture.completedFuture(false);
        if(work!=null&&!work.isDone()&&running.epoch==epoch&&running.answer.isCompletedExceptionally())
            return CompletableFuture.failedFuture(new TimeoutException("block_list_worker_unavailable"));
        long now=clock.getAsLong();
        Cached known=cache.get(player);
        if(known!=null&&(!known.answer.isDone()||!fresh&&now-known.created<UI_TTL))return known.answer;
        if(refreshAnswer.isCompletedExceptionally()){
            if(now-retryAt<0)return CompletableFuture.failedFuture(new IllegalStateException("block_list_refresh_unavailable"));
            cache.values().forEach(c->c.answer.cancel(false));cache.clear();
            enqueueRefresh();
        }
        if(pending.size()>=MAX_PENDING)return CompletableFuture.failedFuture(new RejectedExecutionException("block_list_query_capacity"));
        if(known==null&&cache.size()>=MAX_KEYS){
            var it=cache.entrySet().iterator();boolean evicted=false;
            while(it.hasNext()){if(it.next().getValue().answer.isDone()){it.remove();evicted=true;break;}}
            if(!evicted)return CompletableFuture.failedFuture(new RejectedExecutionException("block_list_identity_capacity"));
        }
        var answer=new CompletableFuture<Boolean>();cache.put(player,new Cached(answer,now));
        pending.addLast(new Request(epoch,player,answer,now));pump();return answer;
    }
    public Status status(UUID player){return status(request(player,false));}
    public static Status status(CompletableFuture<Boolean> answer){
        if(!answer.isDone())return Status.PENDING;
        try{return answer.getNow(true)?Status.BLOCKED:Status.ALLOWED;}
        catch(CompletionException|CancellationException failure){return Status.FAILED;}
    }
    public void pump(){
        long now=clock.getAsLong();
        if(work!=null){
            if(!work.isDone()){
                if(now-running.submitted>=TIMEOUT&&!running.answer.isDone()){
                    running.answer.completeExceptionally(new TimeoutException("block_list_query_timeout"));revision++;
                }
                return;
            }
            var done=running;var result=work;running=null;work=null;
            if(done.epoch==epoch&&!done.answer.isDone()){
                try{done.answer.complete(result.getNow(true));}
                catch(CompletionException|CancellationException e){done.answer.completeExceptionally(e);}
                revision++;
            }
        }
        for(int i=0;i<16&&!pending.isEmpty();i++){
            Request next=pending.removeFirst();
            if(next.epoch!=epoch||next.answer.isDone())continue;
            if(now-next.submitted>=TIMEOUT){next.answer.completeExceptionally(new TimeoutException("block_list_query_timeout"));revision++;continue;}
            if(next.player!=null&&refreshAnswer.isCompletedExceptionally()){
                next.answer.completeExceptionally(new IllegalStateException("block_list_refresh_unavailable"));revision++;continue;
            }
            running=next;
            try{work=CompletableFuture.supplyAsync(()->{
                if(next.player==null){refresh.run();return false;}
                return lookup.test(next.player);
            },worker);}catch(RejectedExecutionException rejected){next.answer.completeExceptionally(rejected);running=null;revision++;}
            break;
        }
    }
    public boolean unavailable(){
        pump();return refreshAnswer.isCompletedExceptionally()||cache.values().stream().anyMatch(c->c.answer.isCompletedExceptionally());
    }
    public int cachedIdentities(){return cache.size();}
    public int pendingQueries(){return pending.size()+(work==null?0:1);}
}
