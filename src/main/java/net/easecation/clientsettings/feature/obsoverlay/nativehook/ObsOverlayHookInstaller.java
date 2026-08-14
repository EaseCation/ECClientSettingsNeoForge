package net.easecation.clientsettings.feature.obsoverlay.nativehook;

import net.easecation.clientsettings.feature.obsoverlay.ObsOverlayInstallation;
import net.easecation.clientsettings.feature.obsoverlay.ObsOverlayNativeInstaller;
import org.lwjgl.glfw.GLFWNativeWin32;

import java.io.IOException;
import java.util.function.Consumer;

public final class ObsOverlayHookInstaller implements ObsOverlayNativeInstaller {

    @Override
    public ObsOverlayInstallation install(
            long glfwWindow,
            Runnable compositor,
            Consumer<Throwable> failureHandler
    ) throws Exception {
        ObsOverlayHook.requireSupportedPlatform();
        long targetWindowHandle = GLFWNativeWin32.glfwGetWin32Window(glfwWindow);
        if (targetWindowHandle == 0L) {
            throw new IOException("Minecraft native window handle is unavailable");
        }
        ObsOverlayHook hook = ObsOverlayHook.install(targetWindowHandle, compositor, failureHandler);
        return new WindowAwareInstallation(glfwWindow, hook);
    }

    private static final class WindowAwareInstallation implements ObsOverlayInstallation {
        private long glfwWindow;
        private final ObsOverlayHook hook;

        private WindowAwareInstallation(long glfwWindow, ObsOverlayHook hook) {
            this.glfwWindow = glfwWindow;
            this.hook = hook;
        }

        @Override
        public boolean unsafeCaptureOrder() {
            return hook.unsafeCaptureOrder();
        }

        @Override
        public boolean isObsCaptureLoaded() {
            return hook.isObsCaptureLoaded();
        }

        @Override
        public void updateWindow(long currentGlfwWindow) throws IOException {
            if (currentGlfwWindow == glfwWindow) {
                return;
            }
            long windowHandle = GLFWNativeWin32.glfwGetWin32Window(currentGlfwWindow);
            if (windowHandle == 0L) {
                throw new IOException("Minecraft native window handle is unavailable after window recreation");
            }
            hook.updateTargetWindow(windowHandle);
            glfwWindow = currentGlfwWindow;
        }

        @Override
        public void uninstall() throws Exception {
            hook.uninstall();
        }
    }
}
