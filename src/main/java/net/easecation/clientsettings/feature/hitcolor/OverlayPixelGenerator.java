package net.easecation.clientsettings.feature.hitcolor;

import java.util.Arrays;

public final class OverlayPixelGenerator {

    public static final int WIDTH = 16;
    public static final int HURT_ROWS = 8;
    public static final int HURT_PIXEL_COUNT = WIDTH * HURT_ROWS;

    private OverlayPixelGenerator() {
    }

    public static int[] solidHurtPixels(int argb) {
        int[] pixels = new int[HURT_PIXEL_COUNT];
        Arrays.fill(pixels, argb);
        return pixels;
    }
}
