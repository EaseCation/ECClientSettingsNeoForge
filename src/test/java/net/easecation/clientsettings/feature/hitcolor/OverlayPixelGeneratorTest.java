package net.easecation.clientsettings.feature.hitcolor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OverlayPixelGeneratorTest {

    @Test
    void fillsExactlyTheHurtRegionWithUnmodifiedArgb() {
        int color = 0x40123456;
        int[] pixels = OverlayPixelGenerator.solidHurtPixels(color);

        assertEquals(16 * 8, pixels.length);
        for (int pixel : pixels) {
            assertEquals(color, pixel);
        }
    }
}
