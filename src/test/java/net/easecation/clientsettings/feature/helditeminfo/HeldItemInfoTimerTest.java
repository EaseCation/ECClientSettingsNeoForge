package net.easecation.clientsettings.feature.helditeminfo;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HeldItemInfoTimerTest {
    @Test void extraRowsExtendLifetimeAndFadeBeforeExpiring() {
        var timer = new HeldItemInfoTimer();
        timer.restart(2, 0.5, 3);
        for (int i = 0; i < 50; i++) timer.tick();
        assertEquals(1, timer.opacity(0));
        for (int i = 0; i < 5; i++) timer.tick();
        assertEquals(0.5, timer.opacity(0));
        assertEquals(0.45, timer.opacity(0.5F), 0.0001);
        for (int i = 0; i < 5; i++) timer.tick();
        assertEquals(0, timer.opacity(0));
        timer.restart(2, 0, 1);
        assertEquals(1, timer.opacity(0));
        timer.clear();
        assertEquals(0, timer.opacity(0));
    }

    @Test void hiddenContentHasNoLifetimeAndMerelyRenderingDoesNotAdvanceTime() {
        var timer = new HeldItemInfoTimer();
        timer.restart(2, 0, 0);
        assertEquals(0, timer.opacity(0));
        timer.restart(2, 0, 1);
        for (int i = 0; i < 35; i++) timer.tick();
        float alpha = timer.opacity(0);
        for (int i = 0; i < 100; i++) assertEquals(alpha, timer.opacity(0));
    }
}
