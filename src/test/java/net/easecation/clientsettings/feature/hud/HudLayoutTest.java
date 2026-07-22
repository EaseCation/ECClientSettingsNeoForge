package net.easecation.clientsettings.feature.hud;

import net.easecation.clientsettings.profile.model.HudWidgetSettings;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HudLayoutTest {

    private static final HudSize SIZE = new HudSize(20, 10);

    @Test
    void normalizedCornersUseTheAvailableTravelArea() {
        HudBounds topLeft = HudLayout.bounds(settings(0.0, 0.0, 1.0), 100, 80, SIZE);
        HudBounds bottomRight = HudLayout.bounds(settings(1.0, 1.0, 1.0), 100, 80, SIZE);

        assertEquals(new HudBounds(4, 4, 20, 10), topLeft);
        assertEquals(new HudBounds(76, 66, 20, 10), bottomRight);
    }

    @Test
    void scalingKeepsTheNormalizedCenterStable() {
        HudBounds bounds = HudLayout.bounds(settings(0.5, 0.5, 2.0), 100, 80, SIZE);

        assertEquals(new HudBounds(30, 30, 40, 20), bounds);
    }

    @Test
    void normalizePositionClampsDraggedWidgetsToTheViewport() {
        HudPosition position = HudLayout.normalizePosition(-50, 500, 100, 80, SIZE, 1.0);

        assertEquals(0.0, position.normalizedX());
        assertEquals(1.0, position.normalizedY());
    }

    @Test
    void oversizedWidgetsAreFittedAndRemainFullyVisible() {
        HudSize oversized = new HudSize(400, 200);
        HudWidgetSettings settings = settings(1.0, 1.0, 3.0);

        HudBounds bounds = HudLayout.bounds(settings, 100, 50, oversized);

        assertEquals(0.21, HudLayout.fittedScale(settings.scale(), 100, 50, oversized));
        assertEquals(new HudBounds(12, 4, 84, 42), bounds);
        assertTrue(bounds.right() <= 100);
        assertTrue(bounds.bottom() <= 50);
    }

    @Test
    void zeroSizedViewportProducesEmptyBounds() {
        assertEquals(new HudBounds(0, 0, 0, 0), HudLayout.bounds(settings(0.5, 0.5, 1.0), 0, 0, SIZE));
    }

    @Test
    void boundsUseRightAndBottomExclusiveHitTesting() {
        HudBounds bounds = new HudBounds(10, 20, 30, 40);

        assertTrue(bounds.contains(10, 20));
        assertTrue(bounds.contains(39.99, 59.99));
        assertFalse(bounds.contains(40, 60));
    }

    @Test
    void invalidRuntimeLayoutArgumentsFailFast() {
        assertThrows(IllegalArgumentException.class, () -> HudLayout.bounds(settings(0.5, 0.5, 1.0), -1, 80, SIZE));
        assertThrows(IllegalArgumentException.class, () -> HudLayout.fittedScale(0.0, 100, 80, SIZE));
    }

    private static HudWidgetSettings settings(double x, double y, double scale) {
        return new HudWidgetSettings(true, x, y, scale);
    }
}
