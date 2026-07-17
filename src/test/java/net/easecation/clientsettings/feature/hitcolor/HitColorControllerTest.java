package net.easecation.clientsettings.feature.hitcolor;

import net.easecation.clientsettings.profile.model.ArgbColor;
import net.easecation.clientsettings.profile.model.HitColorSettings;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HitColorControllerTest {

    @Test
    void captureApplyAndRestorePreserveEveryOriginalPixel() {
        HitColorController controller = new HitColorController();
        FakeTexture texture = FakeTexture.patterned(100);
        int[] original = texture.copyPixels();

        assertTrue(controller.reconcile(texture, enabled(0x80402010)));
        assertArrayEquals(filled(0x80402010), texture.hurtPixels());
        assertArrayEquals(Arrays.copyOfRange(original, 128, 256), texture.lowerPixels());

        assertTrue(controller.restore());
        assertArrayEquals(original, texture.copyPixels());
        assertEquals(2, texture.uploadAttempts);
        assertEquals(1, texture.captureCount);
    }

    @Test
    void sameColorOnSameTextureIsAZeroUploadNoOp() {
        HitColorController controller = new HitColorController();
        FakeTexture texture = FakeTexture.patterned(200);

        assertTrue(controller.reconcile(texture, enabled(0x80ABCDEF)));
        assertFalse(controller.reconcile(texture, enabled(0x80ABCDEF)));

        assertEquals(1, texture.uploadAttempts);
        assertEquals(1, texture.captureCount);
    }

    @Test
    void colorChangeUsesOnePreparedReplacementAndOneUpload() {
        HitColorController controller = new HitColorController();
        FakeTexture texture = FakeTexture.patterned(300);
        controller.reconcile(texture, enabled(0x80112233));

        assertTrue(controller.reconcile(texture, enabled(0x40445566)));

        assertArrayEquals(filled(0x40445566), texture.hurtPixels());
        assertEquals(2, texture.replaceCount);
        assertEquals(2, texture.uploadAttempts);
    }

    @Test
    void disabledSettingsRestoreTheCapturedOriginal() {
        HitColorController controller = new HitColorController();
        FakeTexture texture = FakeTexture.patterned(400);
        int[] original = texture.copyPixels();
        controller.reconcile(texture, enabled(0x80FFAA00));

        assertTrue(controller.reconcile(texture, HitColorSettings.DEFAULT));
        assertFalse(controller.reconcile(texture, HitColorSettings.DEFAULT));

        assertArrayEquals(original, texture.copyPixels());
        assertEquals(2, texture.uploadAttempts);
    }

    @Test
    void newTextureGetsItsOwnCaptureAfterTheOldOneIsRestored() {
        HitColorController controller = new HitColorController();
        FakeTexture oldTexture = FakeTexture.patterned(500);
        FakeTexture newTexture = FakeTexture.patterned(900);
        int[] oldOriginal = oldTexture.copyPixels();
        int[] newOriginalLower = newTexture.lowerPixels();
        controller.reconcile(oldTexture, enabled(0x80112233));

        assertTrue(controller.reconcile(newTexture, enabled(0x80445566)));

        assertArrayEquals(oldOriginal, oldTexture.copyPixels());
        assertArrayEquals(filled(0x80445566), newTexture.hurtPixels());
        assertArrayEquals(newOriginalLower, newTexture.lowerPixels());
        assertEquals(1, oldTexture.captureCount);
        assertEquals(1, newTexture.captureCount);
        assertEquals(2, oldTexture.uploadAttempts);
        assertEquals(1, newTexture.uploadAttempts);
    }

    @Test
    void failedUploadRestoresPixelsAndAbandonsOwnership() {
        HitColorController controller = new HitColorController();
        FakeTexture texture = FakeTexture.patterned(700);
        int[] original = texture.copyPixels();
        texture.failedUploadsRemaining = 1;

        assertThrows(IllegalStateException.class, () -> controller.reconcile(texture, enabled(0x80AABBCC)));

        assertArrayEquals(original, texture.copyPixels());
        assertEquals(2, texture.uploadAttempts);
        assertEquals(1, texture.captureCount);

        assertTrue(controller.reconcile(texture, enabled(0x80AABBCC)));
        assertEquals(2, texture.captureCount);
        assertEquals(3, texture.uploadAttempts);
    }

    private static HitColorSettings enabled(int color) {
        return new HitColorSettings(true, new ArgbColor(color));
    }

    private static int[] filled(int color) {
        int[] pixels = new int[128];
        Arrays.fill(pixels, color);
        return pixels;
    }

    private static final class FakeTexture implements OverlayTextureAccess {

        private final Object identity = new Object();
        private final int[] pixels;
        private int captureCount;
        private int replaceCount;
        private int uploadAttempts;
        private int failedUploadsRemaining;

        private FakeTexture(int[] pixels) {
            this.pixels = pixels;
        }

        static FakeTexture patterned(int seed) {
            int[] pixels = new int[256];
            for (int index = 0; index < pixels.length; index++) {
                pixels[index] = seed * 31 + index;
            }
            return new FakeTexture(pixels);
        }

        @Override
        public Object identity() {
            return identity;
        }

        @Override
        public int[] captureHurtPixels() {
            captureCount++;
            return hurtPixels();
        }

        @Override
        public void replaceHurtPixels(int[] replacement) {
            if (replacement.length != 128) {
                throw new IllegalArgumentException("wrong hurt pixel count");
            }
            replaceCount++;
            System.arraycopy(replacement, 0, pixels, 0, replacement.length);
        }

        @Override
        public void upload() {
            uploadAttempts++;
            if (failedUploadsRemaining > 0) {
                failedUploadsRemaining--;
                throw new IllegalStateException("simulated upload failure");
            }
        }

        int[] hurtPixels() {
            return Arrays.copyOfRange(pixels, 0, 128);
        }

        int[] lowerPixels() {
            return Arrays.copyOfRange(pixels, 128, 256);
        }

        int[] copyPixels() {
            return pixels.clone();
        }
    }
}
