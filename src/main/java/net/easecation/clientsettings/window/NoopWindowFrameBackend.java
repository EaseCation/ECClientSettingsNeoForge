package net.easecation.clientsettings.window;

import net.easecation.clientsettings.window.protocol.FrameAppearance;

final class NoopWindowFrameBackend implements WindowFrameBackend {

    @Override
    public void apply(FrameAppearance appearance) {
    }

    @Override
    public void restore() {
    }
}
