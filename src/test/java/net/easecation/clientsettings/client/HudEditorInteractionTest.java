package net.easecation.clientsettings.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HudEditorInteractionTest {

    @Test
    void scrollChangesScaleInPredictableStepsAndClampsAtLimits() {
        assertEquals(1.1, HudEditorInteraction.scaleAfterScroll(1.0, 1.0));
        assertEquals(0.9, HudEditorInteraction.scaleAfterScroll(1.0, -1.0));
        assertEquals(3.0, HudEditorInteraction.scaleAfterScroll(3.0, 1.0));
        assertEquals(0.5, HudEditorInteraction.scaleAfterScroll(0.5, -1.0));
        assertEquals(1.0, HudEditorInteraction.scaleAfterScroll(1.0, 0.0));
    }

    @Test
    void scalingOriginKeepsTheOldCenter() {
        HudEditorInteraction.Point point = HudEditorInteraction.centeredOrigin(40, 30, 20, 10, 40, 30);

        assertEquals(30, point.x());
        assertEquals(20, point.y());
    }

    @Test
    void invalidScaleAndDimensionsAreRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                HudEditorInteraction.scaleAfterScroll(0.49, 1.0)
        );
        assertThrows(IllegalArgumentException.class, () ->
                HudEditorInteraction.centeredOrigin(0, 0, 10, 10, -1, 10)
        );
    }
}
