package net.easecation.clientsettings.feature.chat;

import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import static org.junit.jupiter.api.Assertions.*;

class AsyncBlockListTest {
    static class Worker implements Executor {
        final ArrayDeque<Runnable> jobs=new ArrayDeque<>();
        public void execute(Runnable task){jobs.addLast(task);}
        void run(){jobs.removeFirst().run();}
    }
    @Test void refreshAndQueriesAreSerializedAndNoUnknownValueIsAllowed(){
        var worker=new Worker();var calls=new ArrayList<String>();var now=new AtomicLong();
        var list=new AsyncBlockList(()->calls.add("refresh"),id->{calls.add("lookup");return true;},worker,now::get);
        list.start();var answer=list.request(new UUID(0,1),true);
        assertEquals(AsyncBlockList.Status.PENDING,AsyncBlockList.status(answer));assertTrue(calls.isEmpty());assertEquals(1,worker.jobs.size());
        worker.run();list.pump();assertEquals(List.of("refresh"),calls);
        worker.run();list.pump();assertTrue(answer.join());assertEquals(List.of("refresh","lookup"),calls);
    }
    @Test void reconnectNeverConcurrentAndStaleAnswerCannotEnterNewCache(){
        var worker=new Worker();var now=new AtomicLong();var refreshes=new AtomicInteger();
        var list=new AsyncBlockList(refreshes::incrementAndGet,id->true,worker,now::get);
        list.start();worker.run();list.pump();UUID id=new UUID(0,1);var old=list.request(id,true);
        list.stop();list.start();var current=list.request(id,true);
        assertTrue(old.isCancelled());assertEquals(1,worker.jobs.size());
        worker.run();list.pump();assertFalse(current.isDone());
        worker.run();list.pump();worker.run();list.pump();assertTrue(current.join());assertEquals(2,refreshes.get());
    }
    @Test void timeoutFailsClosedWithoutCreatingSecondWorker(){
        var worker=new Worker();var now=new AtomicLong();var list=new AsyncBlockList(()->{},id->false,worker,now::get);
        var refresh=list.start();now.set(11_000_000_000L);list.pump();assertTrue(refresh.isCompletedExceptionally());
        for(int i=0;i<1000;i++)list.request(new UUID(0,i),true);
        assertEquals(1,worker.jobs.size());assertTrue(list.cachedIdentities()<=512);assertTrue(list.pendingQueries()<=257);
    }
    @Test void refreshFailureNeverRunsLookupAndBacksOff(){
        var worker=new Worker();var now=new AtomicLong();var called=new AtomicInteger();
        var list=new AsyncBlockList(()->{throw new IllegalStateException();},id->{called.incrementAndGet();return false;},worker,now::get);
        list.start();var result=list.request(new UUID(0,1),true);worker.run();list.pump();
        assertEquals(AsyncBlockList.Status.FAILED,AsyncBlockList.status(result));assertEquals(0,called.get());
        for(int i=0;i<1000;i++)list.request(new UUID(0,i),true);
        assertTrue(worker.jobs.isEmpty());
    }
    @Test void freshMessagesDoNotUseStaleUiDecision(){
        var worker=new Worker();var now=new AtomicLong();var blocked=new AtomicBoolean();UUID id=new UUID(0,1);
        var list=new AsyncBlockList(()->{},p->blocked.get(),worker,now::get);list.start();worker.run();list.pump();
        var first=list.request(id,true);worker.run();list.pump();assertFalse(first.join());
        blocked.set(true);assertEquals(AsyncBlockList.Status.ALLOWED,list.status(id));
        var second=list.request(id,true);assertNotSame(first,second);worker.run();list.pump();assertTrue(second.join());
    }
    @Test void slowServiceNeverRunsOnCallerAndConsumerCanContinue()throws Exception {
        var caller=Thread.currentThread();var entered=new CountDownLatch(1);var release=new CountDownLatch(1);
        try(var worker=Executors.newSingleThreadExecutor()){
            var list=new AsyncBlockList(()->{},id->{assertNotSame(caller,Thread.currentThread());entered.countDown();try{release.await(5,TimeUnit.SECONDS);}catch(InterruptedException e){throw new RuntimeException(e);}return true;},worker,System::nanoTime);
            var ready=list.start();long readyDeadline=System.nanoTime()+5_000_000_000L;
            while(!ready.isDone()&&System.nanoTime()<readyDeadline){list.pump();Thread.yield();}assertTrue(ready.isDone());
            var result=list.request(new UUID(0,1),true);
            try{assertTrue(entered.await(5,TimeUnit.SECONDS));for(int i=0;i<10000;i++)list.pump();assertFalse(result.isDone());}
            finally{release.countDown();}
            long end=System.nanoTime()+5_000_000_000L;while(!result.isDone()&&System.nanoTime()<end){list.pump();Thread.yield();}assertTrue(result.isDone());assertTrue(result.join());
        }
    }
}
