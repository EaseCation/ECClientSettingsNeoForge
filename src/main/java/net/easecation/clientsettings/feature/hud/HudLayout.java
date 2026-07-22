package net.easecation.clientsettings.feature.hud;

import net.easecation.clientsettings.profile.model.HudWidgetSettings;

import java.util.Objects;

public final class HudLayout {

    private static final int EDGE_INSET = 4;

    private HudLayout() {
    }

    public static HudBounds bounds(
            HudWidgetSettings settings,
            int viewportWidth,
            int viewportHeight,
            HudSize unscaledSize
    ) {
        Objects.requireNonNull(settings, "settings");
        Objects.requireNonNull(unscaledSize, "unscaledSize");
        requireViewport(viewportWidth, viewportHeight);

        double scale = fittedScale(settings.scale(), viewportWidth, viewportHeight, unscaledSize);
        if (scale == 0.0) {
            return new HudBounds(0, 0, 0, 0);
        }
        int insetX = edgeInset(viewportWidth);
        int insetY = edgeInset(viewportHeight);
        int availableWidth = availableLength(viewportWidth);
        int availableHeight = availableLength(viewportHeight);
        int width = scaledLength(unscaledSize.width(), scale, availableWidth);
        int height = scaledLength(unscaledSize.height(), scale, availableHeight);
        int travelX = Math.max(0, availableWidth - width);
        int travelY = Math.max(0, availableHeight - height);
        int x = insetX + (int) Math.round(settings.normalizedX() * travelX);
        int y = insetY + (int) Math.round(settings.normalizedY() * travelY);
        return new HudBounds(x, y, width, height);
    }

    public static HudPosition normalizePosition(
            int x,
            int y,
            int viewportWidth,
            int viewportHeight,
            HudSize unscaledSize,
            double requestedScale
    ) {
        Objects.requireNonNull(unscaledSize, "unscaledSize");
        requireViewport(viewportWidth, viewportHeight);

        double scale = fittedScale(requestedScale, viewportWidth, viewportHeight, unscaledSize);
        int insetX = edgeInset(viewportWidth);
        int insetY = edgeInset(viewportHeight);
        int availableWidth = availableLength(viewportWidth);
        int availableHeight = availableLength(viewportHeight);
        int width = scaledLength(unscaledSize.width(), scale, availableWidth);
        int height = scaledLength(unscaledSize.height(), scale, availableHeight);
        int travelX = Math.max(0, availableWidth - width);
        int travelY = Math.max(0, availableHeight - height);
        double normalizedX = travelX == 0
                ? 0.0
                : clamp(x - insetX, 0, travelX) / (double) travelX;
        double normalizedY = travelY == 0
                ? 0.0
                : clamp(y - insetY, 0, travelY) / (double) travelY;
        return new HudPosition(normalizedX, normalizedY);
    }

    public static double fittedScale(
            double requestedScale,
            int viewportWidth,
            int viewportHeight,
            HudSize unscaledSize
    ) {
        Objects.requireNonNull(unscaledSize, "unscaledSize");
        requireViewport(viewportWidth, viewportHeight);
        if (!Double.isFinite(requestedScale) || requestedScale <= 0.0) {
            throw new IllegalArgumentException("requestedScale must be finite and positive");
        }
        if (viewportWidth == 0 || viewportHeight == 0) {
            return 0.0;
        }
        double widthLimit = availableLength(viewportWidth) / (double) unscaledSize.width();
        double heightLimit = availableLength(viewportHeight) / (double) unscaledSize.height();
        return Math.min(requestedScale, Math.min(widthLimit, heightLimit));
    }

    public static int edgeInset(int viewportLength) {
        if (viewportLength < 0) {
            throw new IllegalArgumentException("viewport length must be non-negative");
        }
        return Math.min(EDGE_INSET, viewportLength / 2);
    }

    private static int availableLength(int viewportLength) {
        return Math.max(0, viewportLength - edgeInset(viewportLength) * 2);
    }

    private static int scaledLength(int unscaledLength, double scale, int viewportLength) {
        if (viewportLength == 0 || scale == 0.0) {
            return 0;
        }
        return Math.min(viewportLength, Math.max(1, (int) Math.ceil(unscaledLength * scale)));
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(value, maximum));
    }

    private static void requireViewport(int width, int height) {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("viewport dimensions must be non-negative");
        }
    }
}
