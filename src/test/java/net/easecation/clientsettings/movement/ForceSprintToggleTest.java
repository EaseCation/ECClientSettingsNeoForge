package net.easecation.clientsettings.movement;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForceSprintToggleTest {

    @Test
    void disablingPersistsFalseAndStopsCurrentSprint() {
        AtomicReference<Boolean> saved = new AtomicReference<>();
        AtomicInteger stopCalls = new AtomicInteger();

        boolean result = ForceSprintToggle.toggle(true, saved::set, stopCalls::incrementAndGet);

        assertFalse(result);
        assertEquals(false, saved.get());
        assertEquals(1, stopCalls.get());
    }

    @Test
    void enablingPersistsTrueWithoutForcingSprintState() {
        AtomicReference<Boolean> saved = new AtomicReference<>();
        AtomicInteger stopCalls = new AtomicInteger();

        boolean result = ForceSprintToggle.toggle(false, saved::set, stopCalls::incrementAndGet);

        assertTrue(result);
        assertEquals(true, saved.get());
        assertEquals(0, stopCalls.get());
    }
}
