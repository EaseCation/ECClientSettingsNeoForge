package net.easecation.clientsettings.feature.chat;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BooleanSupplier;

public interface AsyncSocialAccess {
    AsyncBlockList ecSettings$blockList();
    boolean ecSettings$withAnswer(UUID player,boolean blocked,BooleanSupplier action);
    void ecSettings$withSocialView(Runnable action);
}
