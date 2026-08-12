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
        return ObsOverlayHook.install(targetWindowHandle, compositor, failureHandler);
    }
}
