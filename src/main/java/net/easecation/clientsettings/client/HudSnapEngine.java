package net.easecation.clientsettings.client;

import net.easecation.clientsettings.feature.hud.HudLayout;

import java.util.List;

final class HudSnapEngine {

    static final int DEFAULT_THRESHOLD = 4;

    private HudSnapEngine() {
    }

    static SnapResult snap(
            int proposedX,
            int proposedY,
            int width,
            int height,
            int viewportWidth,
            int viewportHeight,
            List<Rect> otherWidgets
    ) {
        return snap(
                proposedX,
                proposedY,
                width,
                height,
                viewportWidth,
                viewportHeight,
                otherWidgets,
                DEFAULT_THRESHOLD
        );
    }

    static SnapResult snap(
            int proposedX,
            int proposedY,
            int width,
            int height,
            int viewportWidth,
            int viewportHeight,
            List<Rect> otherWidgets,
            int threshold
    ) {
        if (width < 0 || height < 0 || viewportWidth < 0 || viewportHeight < 0 || threshold < 0) {
            throw new IllegalArgumentException("HUD geometry values must be non-negative");
        }

        AxisSnap horizontal = snapAxis(
                proposedX,
                width,
                viewportWidth,
                HudLayout.edgeInset(viewportWidth),
                otherWidgets.stream().flatMapToInt(rect -> java.util.stream.IntStream.of(
                        rect.x(), rect.right()
                )).boxed().toList(),
                threshold
        );
        AxisSnap vertical = snapAxis(
                proposedY,
                height,
                viewportHeight,
                HudLayout.edgeInset(viewportHeight),
                otherWidgets.stream().flatMapToInt(rect -> java.util.stream.IntStream.of(
                        rect.y(), rect.bottom()
                )).boxed().toList(),
                threshold
        );

        return new SnapResult(
                horizontal.position(),
                vertical.position(),
                horizontal.guide(),
                vertical.guide()
        );
    }

    private static AxisSnap snapAxis(
            int proposedPosition,
            int elementSize,
            int viewportSize,
            int edgeInset,
            List<Integer> otherEdges,
            int threshold
    ) {
        Candidate best = null;
        best = nearer(best, new Candidate(edgeInset, edgeInset), proposedPosition, threshold);
        best = nearer(best, new Candidate(
                viewportSize - edgeInset - elementSize,
                viewportSize - edgeInset
        ), proposedPosition, threshold);
        best = nearer(
                best,
                new Candidate((viewportSize - elementSize) / 2, viewportSize / 2),
                proposedPosition,
                threshold
        );
        for (int edge : otherEdges) {
            best = nearer(best, new Candidate(edge, edge), proposedPosition, threshold);
            best = nearer(best, new Candidate(edge - elementSize, edge), proposedPosition, threshold);
        }

        int minimum = edgeInset;
        int maximum = Math.max(minimum, viewportSize - edgeInset - elementSize);
        int position = best == null ? proposedPosition : best.position();
        int clampedPosition = Math.clamp(position, minimum, maximum);
        Integer guide = best != null && clampedPosition == position ? best.guide() : null;
        return new AxisSnap(clampedPosition, guide);
    }

    private static Candidate nearer(Candidate current, Candidate candidate, int proposedPosition, int threshold) {
        int distance = Math.abs(candidate.position() - proposedPosition);
        if (distance > threshold) {
            return current;
        }
        if (current == null || distance < Math.abs(current.position() - proposedPosition)) {
            return candidate;
        }
        return current;
    }

    record Rect(int x, int y, int width, int height) {

        Rect {
            if (width < 0 || height < 0) {
                throw new IllegalArgumentException("HUD rectangle dimensions must be non-negative");
            }
        }

        int right() {
            return x + width;
        }

        int bottom() {
            return y + height;
        }
    }

    record SnapResult(int x, int y, Integer verticalGuide, Integer horizontalGuide) {
    }

    private record AxisSnap(int position, Integer guide) {
    }

    private record Candidate(int position, int guide) {
    }
}
