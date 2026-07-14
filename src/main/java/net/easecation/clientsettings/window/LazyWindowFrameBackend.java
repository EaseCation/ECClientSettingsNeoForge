package net.easecation.clientsettings.window;

import net.easecation.clientsettings.window.protocol.FrameAppearance;
import net.easecation.clientsettings.window.windows.JnaDwmFacade;
import net.easecation.clientsettings.window.windows.WindowsWindowFrameBackend;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFWNativeWin32;
import org.lwjgl.system.Platform;

final class LazyWindowFrameBackend implements WindowFrameBackend {

    private WindowFrameBackend delegate;

    @Override
    public void apply(FrameAppearance appearance) {
        delegate().apply(appearance);
    }

    @Override
    public void restore() {
        if (delegate != null) {
            delegate.restore();
        }
    }

    private WindowFrameBackend delegate() {
        if (delegate == null) {
            delegate = createBackend();
        }
        return delegate;
    }

    private static WindowFrameBackend createBackend() {
        if (Platform.get() != Platform.WINDOWS) {
            return new NoopWindowFrameBackend();
        }
        return new WindowsWindowFrameBackend(
                new JnaDwmFacade(),
                () -> GLFWNativeWin32.glfwGetWin32Window(Minecraft.getInstance().getWindow().getWindow())
        );
    }
}
