package net.easecation.clientsettings.movement;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SneakCameraTransitionTest {

    @Test
    void snapsOnlyWhenAnimationIsDisabledForTheLocalPlayerCamera() {
        assertTrue(SneakCameraTransition.shouldSnap(false, true));
        assertFalse(SneakCameraTransition.shouldSnap(true, true));
        assertFalse(SneakCameraTransition.shouldSnap(false, false));
        assertFalse(SneakCameraTransition.shouldSnap(true, false));
    }
}
