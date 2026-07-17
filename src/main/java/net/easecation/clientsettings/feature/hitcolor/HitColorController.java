package net.easecation.clientsettings.feature.hitcolor;

import net.easecation.clientsettings.profile.model.HitColorSettings;

import java.util.Objects;

public final class HitColorController {

    private OverlayTextureAccess ownedTexture;
    private Object ownedIdentity;
    private int[] originalPixels;
    private Integer appliedColor;

    public synchronized boolean reconcile(OverlayTextureAccess texture, HitColorSettings settings) {
        Objects.requireNonNull(texture, "texture");
        Objects.requireNonNull(settings, "settings");

        if (!settings.enabled()) {
            return restore();
        }

        Object identity = Objects.requireNonNull(texture.identity(), "texture identity");
        if (ownedTexture != null && ownedIdentity != identity) {
            restore();
        }
        if (ownedTexture == null) {
            capture(texture, identity);
        }

        int color = settings.color().value();
        if (appliedColor != null && appliedColor == color) {
            return false;
        }

        int[] replacement = OverlayPixelGenerator.solidHurtPixels(color);
        try {
            ownedTexture.replaceHurtPixels(replacement);
            ownedTexture.upload();
            appliedColor = color;
            return true;
        } catch (RuntimeException failure) {
            abandonAfterFailedApply(failure);
            throw failure;
        }
    }

    public synchronized boolean restore() {
        if (ownedTexture == null) {
            return false;
        }

        OverlayTextureAccess texture = ownedTexture;
        int[] restorePixels = originalPixels.clone();
        try {
            texture.replaceHurtPixels(restorePixels);
            texture.upload();
            return true;
        } catch (RuntimeException failure) {
            try {
                texture.replaceHurtPixels(restorePixels);
                texture.upload();
            } catch (RuntimeException retryFailure) {
                failure.addSuppressed(retryFailure);
            }
            throw failure;
        } finally {
            clearOwnership();
        }
    }

    private void capture(OverlayTextureAccess texture, Object identity) {
        int[] captured = Objects.requireNonNull(texture.captureHurtPixels(), "captured hurt pixels");
        if (captured.length != OverlayPixelGenerator.HURT_PIXEL_COUNT) {
            throw new IllegalStateException(
                    "Captured " + captured.length + " hurt pixels instead of " + OverlayPixelGenerator.HURT_PIXEL_COUNT
            );
        }
        ownedTexture = texture;
        ownedIdentity = identity;
        originalPixels = captured.clone();
        appliedColor = null;
    }

    private void abandonAfterFailedApply(RuntimeException failure) {
        OverlayTextureAccess texture = ownedTexture;
        int[] restorePixels = originalPixels.clone();
        try {
            texture.replaceHurtPixels(restorePixels);
            texture.upload();
        } catch (RuntimeException restoreFailure) {
            failure.addSuppressed(restoreFailure);
        } finally {
            clearOwnership();
        }
    }

    private void clearOwnership() {
        ownedTexture = null;
        ownedIdentity = null;
        originalPixels = null;
        appliedColor = null;
    }
}
