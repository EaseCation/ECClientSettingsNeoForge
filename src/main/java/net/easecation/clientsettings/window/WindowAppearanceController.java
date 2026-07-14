package net.easecation.clientsettings.window;

import net.easecation.clientsettings.ECClientSettings;
import net.easecation.clientsettings.config.ClientSettingsConfig;
import net.easecation.clientsettings.window.protocol.FrameAppearance;
import net.easecation.clientsettings.window.protocol.TitleAppearance;
import net.easecation.clientsettings.window.protocol.WindowAppearanceCommand;
import net.minecraft.client.Minecraft;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.LongSupplier;

public final class WindowAppearanceController {

    public static final int MAX_CHANGES_PER_SECOND = 10;
    private static final long RATE_WINDOW_NANOS = 1_000_000_000L;
    private static final WindowAppearanceController INSTANCE = new WindowAppearanceController(
            new LazyWindowFrameBackend(),
            () -> Minecraft.getInstance().updateTitle(),
            ClientSettingsConfig::allowServerWindowTitle,
            ClientSettingsConfig::allowServerWindowFrame,
            System::nanoTime
    );

    private final WindowFrameBackend frameBackend;
    private final Runnable titleUpdater;
    private final BooleanSupplier titleAllowed;
    private final BooleanSupplier frameAllowed;
    private final LongSupplier nanoTime;
    private final Deque<Long> recentChanges = new ArrayDeque<>();

    private volatile String titleOverride;
    private FrameAppearance frameAppearance;

    public WindowAppearanceController(
            WindowFrameBackend frameBackend,
            Runnable titleUpdater,
            BooleanSupplier titleAllowed,
            BooleanSupplier frameAllowed,
            LongSupplier nanoTime
    ) {
        this.frameBackend = Objects.requireNonNull(frameBackend, "frameBackend");
        this.titleUpdater = Objects.requireNonNull(titleUpdater, "titleUpdater");
        this.titleAllowed = Objects.requireNonNull(titleAllowed, "titleAllowed");
        this.frameAllowed = Objects.requireNonNull(frameAllowed, "frameAllowed");
        this.nanoTime = Objects.requireNonNull(nanoTime, "nanoTime");
    }

    public static WindowAppearanceController getInstance() {
        return INSTANCE;
    }

    public synchronized ApplyResult apply(WindowAppearanceCommand command) {
        Objects.requireNonNull(command, "command");
        String requestedTitle = titleOverride;
        FrameAppearance requestedFrame = frameAppearance;
        boolean hasAllowedField = false;

        if (command.title() != null && titleAllowed.getAsBoolean()) {
            hasAllowedField = true;
            requestedTitle = command.title().mode() == TitleAppearance.Mode.CUSTOM ? command.title().text() : null;
        }
        if (command.frame() != null && frameAllowed.getAsBoolean()) {
            hasAllowedField = true;
            requestedFrame = command.frame().mode() == FrameAppearance.Mode.SYSTEM ? null : command.frame();
        }
        if (!hasAllowedField) {
            return ApplyResult.DENIED;
        }

        boolean titleChanged = !Objects.equals(titleOverride, requestedTitle);
        boolean frameChanged = !Objects.equals(frameAppearance, requestedFrame);
        if (!titleChanged && !frameChanged) {
            return ApplyResult.UNCHANGED;
        }
        if (!acquireRatePermit()) {
            return ApplyResult.RATE_LIMITED;
        }

        if (titleChanged) {
            titleOverride = requestedTitle;
            refreshTitle();
        }
        if (frameChanged) {
            frameAppearance = requestedFrame;
            applyFrame(requestedFrame);
        }
        return ApplyResult.APPLIED;
    }

    public synchronized void reset() {
        boolean titleChanged = titleOverride != null;
        boolean frameChanged = frameAppearance != null;
        titleOverride = null;
        frameAppearance = null;
        if (titleChanged) {
            refreshTitle();
        }
        if (frameChanged) {
            restoreFrame();
        }
    }

    public synchronized void disconnect() {
        reset();
        recentChanges.clear();
    }

    public synchronized void reconcilePermissions() {
        if (!titleAllowed.getAsBoolean() && titleOverride != null) {
            titleOverride = null;
            refreshTitle();
        }
        if (!frameAllowed.getAsBoolean() && frameAppearance != null) {
            frameAppearance = null;
            restoreFrame();
        }
    }

    public String titleOverride() {
        return titleOverride;
    }

    FrameAppearance frameAppearance() {
        return frameAppearance;
    }

    private boolean acquireRatePermit() {
        long now = nanoTime.getAsLong();
        while (!recentChanges.isEmpty() && now - recentChanges.peekFirst() >= RATE_WINDOW_NANOS) {
            recentChanges.removeFirst();
        }
        if (recentChanges.size() >= MAX_CHANGES_PER_SECOND) {
            return false;
        }
        recentChanges.addLast(now);
        return true;
    }

    private void refreshTitle() {
        try {
            titleUpdater.run();
        } catch (RuntimeException exception) {
            ECClientSettings.LOGGER.warn("Failed to update the Minecraft window title", exception);
        }
    }

    private void applyFrame(FrameAppearance appearance) {
        try {
            frameBackend.apply(appearance);
        } catch (Throwable throwable) {
            ECClientSettings.LOGGER.warn("Failed to apply the native window frame appearance", throwable);
        }
    }

    private void restoreFrame() {
        try {
            frameBackend.restore();
        } catch (Throwable throwable) {
            ECClientSettings.LOGGER.warn("Failed to restore the native window frame appearance", throwable);
        }
    }

    public enum ApplyResult {
        APPLIED,
        UNCHANGED,
        DENIED,
        RATE_LIMITED
    }
}
