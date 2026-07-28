package net.easecation.clientsettings.feature.gui;

public final class ComfortableGuiScale {
    private static final int STANDARD_RESOLUTION_SHORT_EDGE = 1440;
    private static final int STANDARD_SCALE = 2;
    private static final int HIGH_RESOLUTION_SCALE = 4;

    private ComfortableGuiScale() {
    }

    public static int resolve(
            int configuredScale,
            int framebufferWidth,
            int framebufferHeight,
            int vanillaScale
    ) {
        if (configuredScale != 0) {
            return vanillaScale;
        }

        int preferredScale = Math.min(framebufferWidth, framebufferHeight) > STANDARD_RESOLUTION_SHORT_EDGE
                ? HIGH_RESOLUTION_SCALE
                : STANDARD_SCALE;
        return Math.min(preferredScale, vanillaScale);
    }
}
