package net.easecation.clientsettings.feature.obsoverlay.nativehook;

import com.sun.jna.Function;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.PointerByReference;
import net.easecation.clientsettings.ECClientSettings;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Native hook adapted from OBS Overlay by Artem Dzhemesiuk (MIT).
 * The hook intentionally runs after OBS game capture when OBS attaches after Minecraft.
 */
public final class ObsOverlayHook implements AutoCloseable {

    private static final int MH_OK = 0;
    private static final int MH_ERROR_ALREADY_INITIALIZED = 1;
    private static final String[] OBS_HOOK_MODULES = {
            "graphics-hook64.dll", "graphics-hook32.dll", "graphics-hook.dll"
    };

    private final Kernel32 kernel32;
    private final MinHook minHook;
    private final Pointer swapBuffers;
    private final MinHook.SwapBuffersCallback callback;
    private final Function original;
    private final Consumer<Throwable> failureHandler;
    private final boolean unsafeCaptureOrder;
    private volatile boolean closed;

    private ObsOverlayHook(
            Kernel32 kernel32,
            MinHook minHook,
            Pointer swapBuffers,
            MinHook.SwapBuffersCallback callback,
            Function original,
            Consumer<Throwable> failureHandler,
            boolean unsafeCaptureOrder
    ) {
        this.kernel32 = kernel32;
        this.minHook = minHook;
        this.swapBuffers = swapBuffers;
        this.callback = callback;
        this.original = original;
        this.failureHandler = failureHandler;
        this.unsafeCaptureOrder = unsafeCaptureOrder;
    }

    public static ObsOverlayHook install(
            long targetWindowHandle,
            Runnable compositor,
            Consumer<Throwable> failureHandler
    ) throws IOException {
        Objects.requireNonNull(compositor, "compositor");
        Objects.requireNonNull(failureHandler, "failureHandler");
        requireSupportedPlatform();
        if (targetWindowHandle == 0L) {
            throw new IOException("Minecraft native window handle is unavailable");
        }

        Kernel32 kernel32 = Native.load("Kernel32", Kernel32.class);
        User32 user32 = Native.load("User32", User32.class);
        boolean obsAlreadyLoaded = isObsCaptureLoaded(kernel32);
        Pointer openGl = kernel32.GetModuleHandleA("opengl32.dll");
        if (isNull(openGl)) {
            throw new IOException("opengl32.dll is not loaded");
        }
        Pointer swapBuffers = kernel32.GetProcAddress(openGl, "wglSwapBuffers");
        if (isNull(swapBuffers)) {
            throw new IOException("wglSwapBuffers was not found");
        }

        Path library = NativeLibraryExtractor.extractMinHook();
        MinHook minHook = Native.load(library.toAbsolutePath().toString(), MinHook.class);
        int initializeStatus = minHook.MH_Initialize();
        if (initializeStatus != MH_OK && initializeStatus != MH_ERROR_ALREADY_INITIALIZED) {
            throw new IOException("MinHook initialization failed with status " + initializeStatus);
        }

        PointerByReference originalReference = new PointerByReference();
        Function[] originalHolder = new Function[1];
        ThreadLocal<Boolean> inCallback = ThreadLocal.withInitial(() -> false);
        MinHook.SwapBuffersCallback callback = deviceContext -> {
            Function original = originalHolder[0];
            if (original == null) {
                return 0;
            }
            if (Boolean.TRUE.equals(inCallback.get())) {
                return original.invokeInt(new Object[]{deviceContext});
            }
            inCallback.set(true);
            try {
                Pointer window = user32.WindowFromDC(deviceContext);
                if (!isNull(window) && Pointer.nativeValue(window) == targetWindowHandle) {
                    compositor.run();
                }
            } catch (Throwable throwable) {
                try {
                    failureHandler.accept(throwable);
                } catch (Throwable reportingFailure) {
                    ECClientSettings.LOGGER.error("OBS overlay failure handler failed", reportingFailure);
                }
            } finally {
                inCallback.remove();
            }
            return original.invokeInt(new Object[]{deviceContext});
        };

        int createStatus = minHook.MH_CreateHook(swapBuffers, callback, originalReference);
        if (createStatus != MH_OK || isNull(originalReference.getValue())) {
            if (createStatus == MH_OK) {
                minHook.MH_RemoveHook(swapBuffers);
            }
            minHook.MH_Uninitialize();
            throw new IOException("MinHook could not create wglSwapBuffers hook (status " + createStatus + ")");
        }
        Function original = Function.getFunction(originalReference.getValue(), Function.ALT_CONVENTION);
        originalHolder[0] = original;
        int enableStatus = minHook.MH_EnableHook(swapBuffers);
        if (enableStatus != MH_OK) {
            minHook.MH_RemoveHook(swapBuffers);
            minHook.MH_Uninitialize();
            throw new IOException("MinHook could not enable wglSwapBuffers hook (status " + enableStatus + ")");
        }

        // Treat the narrow attach/install race as unsafe too. A false positive only asks for a restart;
        // a false negative could put the local compositor before OBS capture.
        boolean unsafeCaptureOrder = obsAlreadyLoaded || isObsCaptureLoaded(kernel32);

        return new ObsOverlayHook(
                kernel32, minHook, swapBuffers, callback, original, failureHandler, unsafeCaptureOrder
        );
    }

    public boolean unsafeCaptureOrder() {
        return unsafeCaptureOrder;
    }

    public boolean isObsCaptureLoaded() {
        return isObsCaptureLoaded(kernel32);
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        logCloseFailure("disable", minHook.MH_DisableHook(swapBuffers));
        logCloseFailure("remove", minHook.MH_RemoveHook(swapBuffers));
        logCloseFailure("uninitialize", minHook.MH_Uninitialize());
        // Keep native callback/function strongly reachable until the hook has been removed.
        callback.hashCode();
        original.hashCode();
        failureHandler.hashCode();
    }

    public static boolean supportedPlatform() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String architecture = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
        boolean x64 = architecture.equals("amd64") || architecture.equals("x86_64") || architecture.equals("x64");
        boolean x86 = architecture.equals("x86")
                || architecture.equals("i386")
                || architecture.equals("i486")
                || architecture.equals("i586")
                || architecture.equals("i686");
        return os.contains("win") && (x64 || x86);
    }

    private static void requireSupportedPlatform() throws IOException {
        if (!supportedPlatform()) {
            throw new IOException("OBS overlay supports Windows x64/x86 only");
        }
    }

    private static boolean isObsCaptureLoaded(Kernel32 kernel32) {
        for (String module : OBS_HOOK_MODULES) {
            if (!isNull(kernel32.GetModuleHandleA(module))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isNull(Pointer pointer) {
        return pointer == null || Pointer.nativeValue(pointer) == 0L;
    }

    private static void logCloseFailure(String operation, int status) {
        if (status != MH_OK) {
            ECClientSettings.LOGGER.warn("MinHook {} returned status {} while stopping OBS overlay", operation, status);
        }
    }
}
