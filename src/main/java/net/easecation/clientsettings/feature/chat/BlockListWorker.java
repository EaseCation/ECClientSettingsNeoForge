package net.easecation.clientsettings.feature.chat;

import java.util.concurrent.*;

/** One bounded IO worker shared by manager lifetimes. No render-thread join or new thread per reconnect. */
public final class BlockListWorker {
    public static final Executor EXECUTOR=new ThreadPoolExecutor(1,1,30,TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(8),r->{var t=new Thread(r,"ec-chat-block-list");t.setDaemon(true);return t;},new ThreadPoolExecutor.AbortPolicy());
    private BlockListWorker(){}
}
