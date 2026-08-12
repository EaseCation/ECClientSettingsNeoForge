package net.easecation.clientsettings.feature.obsoverlay.nativehook;

import com.sun.jna.Function;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.PointerByReference;
import net.easecation.clientsettings.feature.obsoverlay.ObsOverlayInstallException;
import org.junit.jupiter.api.Test;
import org.lwjgl.system.CallbackI;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.lwjgl.system.APIUtil.apiStdcall;

class ObsOverlayHookTest {

    @Test
    void minHookReceivesRawDetourPointerInsteadOfJnaCallback() throws Exception {
        Method createHook = MinHook.class.getDeclaredMethod(
                "MH_CreateHook",
                Pointer.class,
                Pointer.class,
                PointerByReference.class
        );

        assertEquals(Pointer.class, createHook.getParameterTypes()[1]);
        assertEquals(0, MinHook.class.getDeclaredClasses().length);
        assertTrue(CallbackI.class.isAssignableFrom(SwapBuffersCallbackI.class));
        assertFalse(com.sun.jna.Callback.class.isAssignableFrom(SwapBuffersCallbackI.class));
        assertEquals(apiStdcall(), SwapBuffersCallbackI.CIF.abi());
        assertEquals(1, SwapBuffersCallbackI.CIF.nargs());
        assertEquals(
                Function.ALT_CONVENTION,
                ObsOverlayHook.stdcallFunction(new Pointer(0x2000L)).getCallingConvention()
        );
    }

    @Test
    void callbackUsesRealStdcallTrampolineResult() throws Exception {
        FakePlatform platform = new FakePlatform();
        FakeClosure closure = new FakeClosure(platform.operations);
        AtomicInteger composites = new AtomicInteger();
        ObsOverlayHook hook = install(platform, closure, composites);

        int result = closure.callback.invoke(77L);

        assertEquals(1234, result);
        assertEquals(1, composites.get());
        assertEquals(1, platform.originalCalls.get());
        hook.uninstall();
        assertEquals(
                List.of("initialize", "create", "enable", "disable", "remove", "freeCallback", "uninitialize"),
                platform.operations
        );
    }

    @Test
    void createFailureFreesCallbackAndOwnedMinHookInitialization() {
        FakePlatform platform = new FakePlatform();
        platform.createStatus = 8;
        FakeClosure closure = new FakeClosure(platform.operations);

        ObsOverlayInstallException failure = assertThrows(
                ObsOverlayInstallException.class,
                () -> install(platform, closure, new AtomicInteger())
        );

        assertNull(failure.retainedInstallation());
        assertTrue(closure.freed);
        assertEquals(
                List.of("initialize", "create", "freeCallback", "uninitialize"),
                platform.operations
        );
    }

    @Test
    void enableFailureRemovesCreatedHookBeforeFreeingCallback() {
        FakePlatform platform = new FakePlatform();
        platform.enableStatus = 9;
        FakeClosure closure = new FakeClosure(platform.operations);

        ObsOverlayInstallException failure = assertThrows(
                ObsOverlayInstallException.class,
                () -> install(platform, closure, new AtomicInteger())
        );

        assertNull(failure.retainedInstallation());
        assertTrue(closure.freed);
        assertEquals(
                List.of("initialize", "create", "enable", "remove", "freeCallback", "uninitialize"),
                platform.operations
        );
    }

    @Test
    void removeFailureDoesNotFreeCallbackAndCanBeRetried() throws Exception {
        FakePlatform platform = new FakePlatform();
        FakeClosure closure = new FakeClosure(platform.operations);
        ObsOverlayHook hook = install(platform, closure, new AtomicInteger());
        platform.removeStatus = 10;

        assertThrows(IOException.class, hook::uninstall);
        assertFalse(closure.freed);
        assertFalse(hook.callbackFreed());

        platform.removeStatus = ObsOverlayHook.MH_OK;
        hook.uninstall();
        assertTrue(closure.freed);
        assertTrue(hook.callbackFreed());
        assertEquals(
                List.of(
                        "initialize", "create", "enable", "disable", "remove",
                        "remove", "freeCallback", "uninitialize"
                ),
                platform.operations
        );
    }

    @Test
    void uninstallWaitsForInFlightCallbackBeforeRemovingItsClosure() throws Exception {
        FakePlatform platform = new FakePlatform();
        FakeClosure closure = new FakeClosure(platform.operations);
        CountDownLatch callbackEntered = new CountDownLatch(1);
        CountDownLatch releaseCallback = new CountDownLatch(1);
        ObsOverlayHook hook = ObsOverlayHook.install(
                42L,
                () -> {
                    callbackEntered.countDown();
                    try {
                        releaseCallback.await();
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException(exception);
                    }
                },
                throwable -> { },
                platform,
                callback -> {
                    closure.callback = callback;
                    return closure;
                }
        );
        AtomicReference<Throwable> callbackFailure = new AtomicReference<>();
        AtomicReference<Throwable> uninstallFailure = new AtomicReference<>();
        Thread callbackThread = new Thread(() -> {
            try {
                closure.callback.invoke(77L);
            } catch (Throwable throwable) {
                callbackFailure.set(throwable);
            }
        });
        callbackThread.start();
        assertTrue(callbackEntered.await(2, TimeUnit.SECONDS));

        Thread uninstallThread = new Thread(() -> {
            try {
                hook.uninstall();
            } catch (Throwable throwable) {
                uninstallFailure.set(throwable);
            }
        });
        uninstallThread.start();
        assertTrue(platform.disabled.await(2, TimeUnit.SECONDS));
        assertFalse(closure.freed);

        releaseCallback.countDown();
        callbackThread.join(2_000L);
        uninstallThread.join(2_000L);

        assertFalse(callbackThread.isAlive());
        assertFalse(uninstallThread.isAlive());
        assertNull(callbackFailure.get());
        assertNull(uninstallFailure.get());
        assertTrue(closure.freed);
    }

    @Test
    void sharedMinHookInitializationIsNeverUninitializedByThisFeature() throws Exception {
        FakePlatform platform = new FakePlatform();
        platform.initializeStatus = ObsOverlayHook.MH_ERROR_ALREADY_INITIALIZED;
        FakeClosure closure = new FakeClosure(platform.operations);
        ObsOverlayHook hook = install(platform, closure, new AtomicInteger());

        hook.uninstall();

        assertFalse(platform.operations.contains("uninitialize"));
        assertEquals(
                List.of("initialize", "create", "enable", "disable", "remove", "freeCallback"),
                platform.operations
        );
    }

    @Test
    void missingObsModuleDoesNotPreventSuccessfulInstallation() throws Exception {
        FakePlatform platform = new FakePlatform();
        platform.obsLoaded = false;
        FakeClosure closure = new FakeClosure(platform.operations);

        ObsOverlayHook hook = install(platform, closure, new AtomicInteger());

        assertFalse(hook.unsafeCaptureOrder());
        assertFalse(hook.isObsCaptureLoaded());
        hook.uninstall();
    }

    private static ObsOverlayHook install(
            FakePlatform platform,
            FakeClosure closure,
            AtomicInteger composites
    ) throws Exception {
        return ObsOverlayHook.install(
                42L,
                composites::incrementAndGet,
                throwable -> { },
                platform,
                callback -> {
                    closure.callback = callback;
                    return closure;
                }
        );
    }

    private static final class FakeClosure implements ObsOverlayHook.CallbackClosure {
        private final List<String> operations;
        private ObsOverlayHook.SwapBuffersCallback callback;
        private boolean freed;

        private FakeClosure(List<String> operations) {
            this.operations = operations;
        }

        @Override
        public Pointer pointer() {
            return new Pointer(0x3000L);
        }

        @Override
        public void free() {
            operations.add("freeCallback");
            freed = true;
        }
    }

    private static final class FakePlatform implements ObsOverlayHook.NativePlatform {
        private final List<String> operations = new ArrayList<>();
        private final AtomicInteger originalCalls = new AtomicInteger();
        private final CountDownLatch disabled = new CountDownLatch(1);
        private int initializeStatus = ObsOverlayHook.MH_OK;
        private int createStatus = ObsOverlayHook.MH_OK;
        private int enableStatus = ObsOverlayHook.MH_OK;
        private int disableStatus = ObsOverlayHook.MH_OK;
        private int removeStatus = ObsOverlayHook.MH_OK;
        private int uninitializeStatus = ObsOverlayHook.MH_OK;
        private boolean obsLoaded;

        @Override
        public boolean isObsCaptureLoaded() {
            return obsLoaded;
        }

        @Override
        public Pointer swapBuffers() {
            return new Pointer(0x1000L);
        }

        @Override
        public int initialize() {
            operations.add("initialize");
            return initializeStatus;
        }

        @Override
        public int createHook(Pointer target, Pointer detour, PointerByReference original) {
            operations.add("create");
            if (createStatus == ObsOverlayHook.MH_OK) {
                original.setValue(new Pointer(0x2000L));
            }
            return createStatus;
        }

        @Override
        public int enableHook(Pointer target) {
            operations.add("enable");
            return enableStatus;
        }

        @Override
        public int disableHook(Pointer target) {
            operations.add("disable");
            disabled.countDown();
            return disableStatus;
        }

        @Override
        public int removeHook(Pointer target) {
            operations.add("remove");
            return removeStatus;
        }

        @Override
        public int uninitialize() {
            operations.add("uninitialize");
            return uninitializeStatus;
        }

        @Override
        public long windowFromDeviceContext(long deviceContext) {
            return 42L;
        }

        @Override
        public ObsOverlayHook.OriginalSwapBuffers original(Pointer pointer) {
            return deviceContext -> {
                originalCalls.incrementAndGet();
                return 1234;
            };
        }
    }
}
