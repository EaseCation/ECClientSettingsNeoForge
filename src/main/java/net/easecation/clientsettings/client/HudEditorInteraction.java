package net.easecation.clientsettings.client;

final class HudEditorInteraction {

    static final double MIN_SCALE = 0.5;
    static final double MAX_SCALE = 3.0;
    static final double SCALE_STEP = 0.1;
    static final long DOUBLE_CLICK_INTERVAL_MILLIS = 400L;
    static final double DOUBLE_CLICK_MAX_DISTANCE = 5.0;

    private HudEditorInteraction() {
    }

    static double scaleAfterScroll(double currentScale, double scrollY) {
        if (!Double.isFinite(currentScale) || currentScale < MIN_SCALE || currentScale > MAX_SCALE) {
            throw new IllegalArgumentException("currentScale must be within the HUD scale range");
        }
        if (!Double.isFinite(scrollY)) {
            throw new IllegalArgumentException("scrollY must be finite");
        }
        if (scrollY == 0.0) {
            return currentScale;
        }
        return Math.clamp(
                currentScale + Math.copySign(SCALE_STEP, scrollY),
                MIN_SCALE,
                MAX_SCALE
        );
    }

    static Point centeredOrigin(
            int oldX,
            int oldY,
            int oldWidth,
            int oldHeight,
            int newWidth,
            int newHeight
    ) {
        if (oldWidth < 0 || oldHeight < 0 || newWidth < 0 || newHeight < 0) {
            throw new IllegalArgumentException("HUD dimensions must be non-negative");
        }
        return new Point(
                oldX + (oldWidth - newWidth) / 2,
                oldY + (oldHeight - newHeight) / 2
        );
    }

    static boolean isDoubleClick(
            long previousClickMillis,
            long currentClickMillis,
            double deltaX,
            double deltaY
    ) {
        long interval = currentClickMillis - previousClickMillis;
        return previousClickMillis >= 0L
                && interval >= 0L
                && interval <= DOUBLE_CLICK_INTERVAL_MILLIS
                && Double.isFinite(deltaX)
                && Double.isFinite(deltaY)
                && deltaX * deltaX + deltaY * deltaY
                <= DOUBLE_CLICK_MAX_DISTANCE * DOUBLE_CLICK_MAX_DISTANCE;
    }

    record Point(int x, int y) {
    }
}
