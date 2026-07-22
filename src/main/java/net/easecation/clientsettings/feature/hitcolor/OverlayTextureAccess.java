package net.easecation.clientsettings.feature.hitcolor;

public interface OverlayTextureAccess {

    Object identity();

    int[] captureHurtPixels();

    void replaceHurtPixels(int[] pixels);

    void upload();
}
