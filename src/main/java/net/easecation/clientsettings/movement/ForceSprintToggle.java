package net.easecation.clientsettings.movement;

import java.util.function.Consumer;

/** Applies one force-sprint toggle while keeping client-specific side effects injectable. */
public final class ForceSprintToggle {

    private ForceSprintToggle() {
    }

    public static boolean toggle(boolean current, Consumer<Boolean> save, Runnable stopSprinting) {
        boolean enabled = !current;
        save.accept(enabled);
        if (!enabled) {
            stopSprinting.run();
        }
        return enabled;
    }
}
