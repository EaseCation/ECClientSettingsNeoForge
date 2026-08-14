package net.easecation.clientsettings.feature.obsoverlay.nativehook;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.PointerByReference;
import net.easecation.clientsettings.ECClientSettings;
import net.easecation.clientsettings.feature.obsoverlay.ObsOverlayInstallException;
import net.easecation.clientsettings.feature.obsoverlay.ObsOverlayInstallation;
import org.lwjgl.system.Callback;
import org.lwjgl.system.JNI;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Native hook adapted from OBS Overlay by Artem Dzhemesiuk (MIT).
 * The hook intentionally runs after OBS game capture when OBS attaches after Minecraft.
 */
public final class ObsOverlayHook implements ObsOverlayInstallation {

    static final int MH_OK = 0;
    static final int MH_ERROR_ALREADY_INITIALIZED = 1;
    private static final String[] OBS_HOOK_MODULES = {
            "graphics-hook64.dll", "graphics-hook32.dll", "graphics-hook.dll"
    };

    private final NativePlatform platform;
    private final Pointer swapBuffers;
    private final CallbackClosure callbackClosure;
    private final CallbackState callbackState;
    private final boolean ownsInitialization;
    private boolean unsafeCaptureOrder;
    private boolean initialized;
    private boolean created;
    private boolean enabled;
    private boolean callbackFreed;
    private boolean closed;

    private ObsOverlayHook(
            NativePlatform platform,
            Pointer swapBuffers,
            CallbackClosure callbackClosure,
            CallbackState callbackState,
            boolean ownsInitialization,
            boolean unsafeCaptureOrder
    ) {
        this.platform = platform;
        this.swapBuffers = swapBuffers;
        this.callbackClosure = callbackClosure;
        this.callbackState = callbackState;
        this.ownsInitialization = ownsInitialization;
        this.unsafeCaptureOrder = unsafeCaptureOrder;
        this.initialized = true;
    }

    static ObsOverlayHook install(
            Runnable compositor,
            Consumer<Throwable> failureHandler
    ) throws Exception {
        return install(
                compositor,
                failureHandler,
                JnaNativePlatform.create(),
                LwjglCallbackClosure::create
        );
    }

    static ObsOverlayHook install(
            Runnable compositor,
            Consumer<Throwable> failureHandler,
            NativePlatform platform,
            CallbackFactory callbackFactory
    ) throws Exception {
        Objects.requireNonNull(compositor, "compositor");
        Objects.requireNonNull(failureHandler, "failureHandler");
        Objects.requireNonNull(platform, "platform");
        Objects.requireNonNull(callbackFactory, "callbackFactory");
        boolean obsAlreadyLoaded = platform.isObsCaptureLoaded();
        Pointer swapBuffers = platform.swapBuffers();
        if (isNull(swapBuffers)) {
            throw new IOException("wglSwapBuffers was not found");
        }

        int initializeStatus = platform.initialize();
        if (initializeStatus != MH_OK && initializeStatus != MH_ERROR_ALREADY_INITIALIZED) {
            throw new IOException("MinHook initialization failed with status " + initializeStatus);
        }
        boolean ownsInitialization = initializeStatus == MH_OK;
        CallbackState callbackState = new CallbackState(
                compositor,
                failureHandler
        );
        CallbackClosure callbackClosure;
        try {
            callbackClosure = callbackFactory.create(callbackState::invoke);
        } catch (RuntimeException | LinkageError failure) {
            if (ownsInitialization) {
                int uninitializeStatus = platform.uninitialize();
                if (uninitializeStatus != MH_OK) {
                    failure.addSuppressed(statusFailure("uninitialize", uninitializeStatus));
                }
            }
            throw failure;
        }

        ObsOverlayHook hook = new ObsOverlayHook(
                platform,
                swapBuffers,
                callbackClosure,
                callbackState,
                ownsInitialization,
                obsAlreadyLoaded
        );
        PointerByReference originalReference = new PointerByReference();
        int createStatus;
        try {
            createStatus = platform.createHook(swapBuffers, callbackClosure.pointer(), originalReference);
        } catch (RuntimeException | LinkageError failure) {
            throw hook.rollbackInstall(safeMessage(failure), failure);
        }
        if (createStatus != MH_OK) {
            throw hook.rollbackInstall(
                    "MinHook could not create wglSwapBuffers hook (status " + createStatus + ")"
            );
        }
        hook.created = true;
        Pointer originalPointer = originalReference.getValue();
        if (isNull(originalPointer)) {
            throw hook.rollbackInstall("MinHook created wglSwapBuffers hook without a trampoline");
        }
        try {
            callbackState.setOriginal(platform.original(originalPointer));
            int enableStatus = platform.enableHook(swapBuffers);
            if (enableStatus != MH_OK) {
                throw new IOException(
                        "MinHook could not enable wglSwapBuffers hook (status " + enableStatus + ")"
                );
            }
            hook.enabled = true;
            hook.unsafeCaptureOrder = obsAlreadyLoaded || platform.isObsCaptureLoaded();
        } catch (Exception | LinkageError failure) {
            throw hook.rollbackInstall(safeMessage(failure), failure);
        }
        return hook;
    }

    @Override
    public boolean unsafeCaptureOrder() {
        return unsafeCaptureOrder;
    }

    @Override
    public boolean isObsCaptureLoaded() {
        return platform.isObsCaptureLoaded();
    }

    @Override
    public ObsOverlayInstallation armTargetSwap() {
        callbackState.armTargetSwap();
        return this;
    }

    @Override
    public void disarmTargetSwap() {
        callbackState.disarmTargetSwap();
    }

    @Override
    public synchronized void uninstall() throws Exception {
        if (closed) {
            return;
        }
        if (enabled) {
            int status = platform.disableHook(swapBuffers);
            if (status != MH_OK) {
                throw statusFailure("disable", status);
            }
            enabled = false;
        }
        if (created) {
            callbackState.awaitQuiescence();
            int status = platform.removeHook(swapBuffers);
            if (status != MH_OK) {
                throw statusFailure("remove", status);
            }
            created = false;
        }
        if (!callbackFreed) {
            callbackClosure.free();
            callbackFreed = true;
        }
        if (initialized && ownsInitialization) {
            int status = platform.uninitialize();
            if (status != MH_OK) {
                throw statusFailure("uninitialize", status);
            }
        }
        initialized = false;
        closed = true;
    }

    boolean callbackFreed() {
        return callbackFreed;
    }

    private ObsOverlayInstallException rollbackInstall(String message) {
        return rollbackInstall(message, new IOException(message));
    }

    private ObsOverlayInstallException rollbackInstall(String message, Throwable failure) {
        try {
            uninstall();
            return new ObsOverlayInstallException(message, failure);
        } catch (Exception | LinkageError rollbackFailure) {
            failure.addSuppressed(rollbackFailure);
            return new ObsOverlayInstallException(message, failure, this);
        }
    }

    private static String safeMessage(Throwable throwable) {
        String message = throwable.getMessage();
        return message == null || message.isBlank() ? throwable.getClass().getSimpleName() : message;
    }

    static boolean supportedPlatform() {
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

    static void requireSupportedPlatform() throws IOException {
        if (!supportedPlatform()) {
            throw new IOException("OBS overlay supports Windows x64/x86 only");
        }
    }

    private static IOException statusFailure(String operation, int status) {
        return new IOException("MinHook " + operation + " failed with status " + status);
    }

    private static boolean isNull(Pointer pointer) {
        return pointer == null || Pointer.nativeValue(pointer) == 0L;
    }

    @FunctionalInterface
    interface CallbackFactory {
        CallbackClosure create(SwapBuffersCallback callback);
    }

    interface CallbackClosure {
        Pointer pointer();

        void free();
    }

    @FunctionalInterface
    interface SwapBuffersCallback {
        int invoke(long deviceContext);
    }

    @FunctionalInterface
    interface OriginalSwapBuffers {
        int invoke(long deviceContext);
    }

    interface NativePlatform {
        boolean isObsCaptureLoaded();

        Pointer swapBuffers() throws IOException;

        int initialize();

        int createHook(Pointer target, Pointer detour, PointerByReference original);

        int enableHook(Pointer target);

        int disableHook(Pointer target);

        int removeHook(Pointer target);

        int uninitialize();

        OriginalSwapBuffers original(Pointer pointer);
    }

    private static final class CallbackState {
        private final Runnable compositor;
        private final Consumer<Throwable> failureHandler;
        // The detour runs synchronously on the Java thread that called glfwSwapBuffers.
        private final ThreadLocal<Boolean> targetSwapArmed = new ThreadLocal<>();
        private volatile OriginalSwapBuffers original;
        private int activeCallbacks;

        private CallbackState(
                Runnable compositor,
                Consumer<Throwable> failureHandler
        ) {
            this.compositor = compositor;
            this.failureHandler = failureHandler;
        }

        private void setOriginal(OriginalSwapBuffers original) {
            this.original = Objects.requireNonNull(original, "original");
        }

        private void armTargetSwap() {
            if (Boolean.TRUE.equals(targetSwapArmed.get())) {
                throw new IllegalStateException("OBS target SwapBuffers scope is already armed on this thread");
            }
            targetSwapArmed.set(true);
        }

        private void disarmTargetSwap() {
            targetSwapArmed.remove();
        }

        private int invoke(long deviceContext) {
            enterCallback();
            try {
                return invokeInstalled(deviceContext);
            } finally {
                exitCallback();
            }
        }

        private int invokeInstalled(long deviceContext) {
            OriginalSwapBuffers currentOriginal = original;
            if (currentOriginal == null) {
                throw new IllegalStateException("wglSwapBuffers callback invoked before its trampoline was available");
            }
            boolean targetSwap = Boolean.TRUE.equals(targetSwapArmed.get());
            targetSwapArmed.remove();
            try {
                if (targetSwap) {
                    compositor.run();
                }
            } catch (Throwable throwable) {
                try {
                    failureHandler.accept(throwable);
                } catch (Throwable reportingFailure) {
                    ECClientSettings.LOGGER.error("OBS overlay failure handler failed", reportingFailure);
                }
            }
            return currentOriginal.invoke(deviceContext);
        }

        private synchronized void enterCallback() {
            activeCallbacks++;
        }

        private synchronized void exitCallback() {
            activeCallbacks--;
            if (activeCallbacks == 0) {
                notifyAll();
            }
        }

        private synchronized void awaitQuiescence() throws InterruptedException {
            while (activeCallbacks != 0) {
                wait();
            }
        }
    }

    private record LwjglCallbackClosure(SwapBuffersCallbackI callback, long address) implements CallbackClosure {

        private static LwjglCallbackClosure create(SwapBuffersCallback callback) {
            SwapBuffersCallbackI callbackInterface = callback::invoke;
            return new LwjglCallbackClosure(callbackInterface, callbackInterface.address());
        }

        @Override
        public Pointer pointer() {
            return new Pointer(address);
        }

        @Override
        public void free() {
            Callback.free(address);
        }
    }

    private static final class JnaNativePlatform implements NativePlatform {
        private final Kernel32 kernel32;
        private final MinHook minHook;
        private final Pointer swapBuffers;

        private JnaNativePlatform(
                Kernel32 kernel32,
                MinHook minHook,
                Pointer swapBuffers
        ) {
            this.kernel32 = kernel32;
            this.minHook = minHook;
            this.swapBuffers = swapBuffers;
        }

        private static JnaNativePlatform create() throws IOException {
            Kernel32 kernel32 = Native.load("Kernel32", Kernel32.class);
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
            return new JnaNativePlatform(
                    kernel32,
                    minHook,
                    swapBuffers
            );
        }

        @Override
        public boolean isObsCaptureLoaded() {
            for (String module : OBS_HOOK_MODULES) {
                if (!isNull(kernel32.GetModuleHandleA(module))) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public Pointer swapBuffers() {
            return swapBuffers;
        }

        @Override
        public int initialize() {
            return minHook.MH_Initialize();
        }

        @Override
        public int createHook(Pointer target, Pointer detour, PointerByReference original) {
            return minHook.MH_CreateHook(target, detour, original);
        }

        @Override
        public int enableHook(Pointer target) {
            return minHook.MH_EnableHook(target);
        }

        @Override
        public int disableHook(Pointer target) {
            return minHook.MH_DisableHook(target);
        }

        @Override
        public int removeHook(Pointer target) {
            return minHook.MH_RemoveHook(target);
        }

        @Override
        public int uninitialize() {
            return minHook.MH_Uninitialize();
        }

        @Override
        public OriginalSwapBuffers original(Pointer pointer) {
            long address = Pointer.nativeValue(pointer);
            return deviceContext -> JNI.callPI(deviceContext, address);
        }
    }
}
