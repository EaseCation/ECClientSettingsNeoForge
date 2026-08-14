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
        return windowAware(hook, GLFWNativeWin32::glfwGetWin32Window, targetWindowHandle);
    }

    static ObsOverlayInstallation windowAware(
            ObsOverlayHook hook,
            NativeWindowResolver windowResolver,
            long initialWindowHandle
    ) {
        return new WindowAwareInstallation(hook, windowResolver, initialWindowHandle);
    }

    @FunctionalInterface
    interface NativeWindowResolver {
        long windowHandle(long glfwWindow);
    }

    private static final class WindowAwareInstallation implements ObsOverlayInstallation {
        private final ObsOverlayHook hook;
        private final NativeWindowResolver windowResolver;
        private long windowHandle;

        private WindowAwareInstallation(
                ObsOverlayHook hook,
                NativeWindowResolver windowResolver,
                long initialWindowHandle
        ) {
            this.hook = hook;
            this.windowResolver = windowResolver;
            this.windowHandle = initialWindowHandle;
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
            long currentWindowHandle = windowResolver.windowHandle(currentGlfwWindow);
            if (currentWindowHandle == 0L) {
                throw new IOException("Minecraft native window handle is unavailable");
            }
            if (currentWindowHandle == windowHandle) {
                return;
            }
            hook.refreshTargetWindow(currentWindowHandle);
            windowHandle = currentWindowHandle;
        }

        @Override
        public void uninstall() throws Exception {
            hook.uninstall();
        }
    }
}
