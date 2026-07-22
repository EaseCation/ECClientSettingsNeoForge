package net.easecation.clientsettings.client;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HudSnapEngineTest {

    @Test
    void snapsToScreenEdgesAndReportsGuides() {
        HudSnapEngine.SnapResult topLeft = HudSnapEngine.snap(3, 4, 20, 10, 200, 100, List.of());
        HudSnapEngine.SnapResult bottomRight = HudSnapEngine.snap(177, 87, 20, 10, 200, 100, List.of());

        assertEquals(4, topLeft.x());
        assertEquals(4, topLeft.y());
        assertEquals(4, topLeft.verticalGuide());
        assertEquals(4, topLeft.horizontalGuide());
        assertEquals(176, bottomRight.x());
        assertEquals(86, bottomRight.y());
        assertEquals(196, bottomRight.verticalGuide());
        assertEquals(96, bottomRight.horizontalGuide());
    }

    @Test
    void snapsWidgetCentersToViewportCenterLines() {
        HudSnapEngine.SnapResult result = HudSnapEngine.snap(87, 43, 20, 10, 200, 100, List.of());

        assertEquals(90, result.x());
        assertEquals(45, result.y());
        assertEquals(100, result.verticalGuide());
        assertEquals(50, result.horizontalGuide());
    }

    @Test
    void snapsEitherMovingEdgeToOtherWidgetEdges() {
        HudSnapEngine.Rect other = new HudSnapEngine.Rect(60, 30, 40, 20);
        HudSnapEngine.SnapResult adjacent = HudSnapEngine.snap(37, 52, 20, 10, 200, 100, List.of(other));

        assertEquals(40, adjacent.x());
        assertEquals(50, adjacent.y());
        assertEquals(60, adjacent.verticalGuide());
        assertEquals(50, adjacent.horizontalGuide());
    }

    @Test
    void ignoresTargetsBeyondThreshold() {
        HudSnapEngine.SnapResult result = HudSnapEngine.snap(10, 12, 20, 10, 200, 100, List.of());

        assertEquals(10, result.x());
        assertEquals(12, result.y());
        assertNull(result.verticalGuide());
        assertNull(result.horizontalGuide());
    }

    @Test
    void clampsUnsnappedWidgetsInsideViewport() {
        HudSnapEngine.SnapResult result = HudSnapEngine.snap(500, -20, 20, 10, 200, 100, List.of());

        assertEquals(176, result.x());
        assertEquals(4, result.y());
    }

    @Test
    void clearsAGuideWhenItsSnapCandidateMustBeClamped() {
        HudSnapEngine.Rect other = new HudSnapEngine.Rect(20, 20, 20, 20);

        HudSnapEngine.SnapResult result = HudSnapEngine.snap(-57, 50, 80, 10, 200, 100, List.of(other));

        assertEquals(4, result.x());
        assertNull(result.verticalGuide());
    }

    @Test
    void rejectsNegativeGeometry() {
        assertThrows(IllegalArgumentException.class, () ->
                HudSnapEngine.snap(0, 0, -1, 10, 200, 100, List.of())
        );
    }
}
