package net.easecation.clientsettings.window;

import net.easecation.clientsettings.window.protocol.FrameAppearance;

public interface WindowFrameBackend {

    void apply(FrameAppearance appearance);

    void restore();
}
