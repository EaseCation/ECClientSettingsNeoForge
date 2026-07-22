package net.easecation.clientsettings.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProfileManagementEntryTest {

    @Test
    void onlyAcceptsClicksInsideItsRenderedRow() {
        int x = 20;
        int y = 40;
        int width = 200;
        int height = 24;

        assertTrue(ProfileManagementEntry.contains(20, 40, x, y, width, height));
        assertTrue(ProfileManagementEntry.contains(219.9, 63.9, x, y, width, height));
        assertFalse(ProfileManagementEntry.contains(100, 39.9, x, y, width, height));
        assertFalse(ProfileManagementEntry.contains(100, 64, x, y, width, height));
        assertFalse(ProfileManagementEntry.contains(220, 50, x, y, width, height));
    }
}
