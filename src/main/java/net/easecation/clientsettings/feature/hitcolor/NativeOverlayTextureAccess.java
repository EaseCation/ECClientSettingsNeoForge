package net.easecation.clientsettings.feature.hitcolor;

import com.mojang.blaze3d.platform.NativeImage;
import net.easecation.clientsettings.mixin.render.OverlayTextureAccessor;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;

import java.util.Objects;

final class NativeOverlayTextureAccess implements OverlayTextureAccess {

    private static final int TEXTURE_HEIGHT = 16;

    private final OverlayTexture overlayTexture;
    private final DynamicTexture dynamicTexture;

    NativeOverlayTextureAccess(OverlayTexture overlayTexture) {
        this.overlayTexture = Objects.requireNonNull(overlayTexture, "overlayTexture");
        this.dynamicTexture = Objects.requireNonNull(
                ((OverlayTextureAccessor) overlayTexture).ecclientsettings$getTexture(),
                "overlay dynamic texture"
        );
    }

    @Override
    public Object identity() {
        return overlayTexture;
    }

    @Override
    public int[] captureHurtPixels() {
        NativeImage image = requireImage();
        int[] captured = new int[OverlayPixelGenerator.HURT_PIXEL_COUNT];
        for (int y = 0; y < OverlayPixelGenerator.HURT_ROWS; y++) {
            for (int x = 0; x < OverlayPixelGenerator.WIDTH; x++) {
                captured[index(x, y)] = image.getPixel(x, y);
            }
        }
        return captured;
    }

    @Override
    public void replaceHurtPixels(int[] pixels) {
        requireHurtPixels(pixels);
        NativeImage image = requireImage();
        for (int y = 0; y < OverlayPixelGenerator.HURT_ROWS; y++) {
            for (int x = 0; x < OverlayPixelGenerator.WIDTH; x++) {
                image.setPixel(x, y, pixels[index(x, y)]);
            }
        }
    }

    @Override
    public void upload() {
        dynamicTexture.upload();
    }

    private NativeImage requireImage() {
        NativeImage image = dynamicTexture.getPixels();
        if (image == null) {
            throw new IllegalStateException("Overlay texture is closed");
        }
        if (image.getWidth() != OverlayPixelGenerator.WIDTH || image.getHeight() != TEXTURE_HEIGHT) {
            throw new IllegalStateException(
                    "Unexpected overlay texture size: " + image.getWidth() + "x" + image.getHeight()
            );
        }
        return image;
    }

    private static void requireHurtPixels(int[] pixels) {
        if (pixels == null || pixels.length != OverlayPixelGenerator.HURT_PIXEL_COUNT) {
            throw new IllegalArgumentException(
                    "Hurt overlay requires exactly " + OverlayPixelGenerator.HURT_PIXEL_COUNT + " pixels"
            );
        }
    }

    private static int index(int x, int y) {
        return y * OverlayPixelGenerator.WIDTH + x;
    }
}
