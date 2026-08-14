package net.easecation.clientsettings.feature.obsoverlay;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObsOverlayLifecycleTest {

    @Test
    void configMayLoadBeforeClientStarts() {
        FakeInstallation installed = new FakeInstallation(false, false);
        AtomicInteger installs = new AtomicInteger();
        ObsOverlayLifecycle lifecycle = new ObsOverlayLifecycle(() -> {
            installs.incrementAndGet();
            return installed;
        });

        lifecycle.onConfigLoaded(true);
        assertEquals(ObsOverlayLifecycleState.READY, lifecycle.state());
        assertEquals(0, installs.get());

        lifecycle.onClientStarted();
        assertEquals(ObsOverlayLifecycleState.ACTIVE, lifecycle.state());
        assertEquals(1, installs.get());
    }

    @Test
    void clientMayStartBeforeConfigLoads() {
        AtomicInteger installs = new AtomicInteger();
        ObsOverlayLifecycle lifecycle = new ObsOverlayLifecycle(() -> {
            installs.incrementAndGet();
            return new FakeInstallation(false, false);
        });

        lifecycle.onClientStarted();
        assertEquals(ObsOverlayLifecycleState.WAITING_FOR_CONFIG, lifecycle.state());
        assertEquals(0, installs.get());

        lifecycle.onConfigLoaded(true);
        assertEquals(ObsOverlayLifecycleState.ACTIVE, lifecycle.state());
        assertEquals(1, installs.get());
    }

    @Test
    void defaultDisabledConfigurationNeverInvokesNativeInstaller() {
        AtomicInteger installs = new AtomicInteger();
        ObsOverlayLifecycle lifecycle = new ObsOverlayLifecycle(() -> {
            installs.incrementAndGet();
            return new FakeInstallation(false, false);
        });

        assertFalse(ObsOverlaySettings.DEFAULT.enabled());
        lifecycle.onConfigLoaded(ObsOverlaySettings.DEFAULT.enabled());
        lifecycle.onClientStarted();
        lifecycle.onConfigLoaded(false);

        assertEquals(ObsOverlayLifecycleState.DISABLED, lifecycle.state());
        assertEquals(0, installs.get());
    }

    @Test
    void disabledOverlayNeverArmsNativeTargetSwap() {
        FakeInstallation installed = new FakeInstallation(false, false);
        ObsOverlayLifecycle lifecycle = new ObsOverlayLifecycle(() -> installed);
        lifecycle.onClientStarted();
        lifecycle.onConfigLoaded(false);

        int frames = 1_000_000;
        long started = System.nanoTime();
        for (int frame = 0; frame < frames; frame++) {
            assertNull(lifecycle.armTargetSwap());
        }
        long elapsed = System.nanoTime() - started;

        assertEquals(0, installed.targetSwapArms.get());
        assertEquals(ObsOverlayLifecycleState.DISABLED, lifecycle.state());
        System.out.printf(
                Locale.ROOT,
                "OBS_DISABLED_BENCHMARK frames=%d ns_per_frame=%.2f native_calls=0%n",
                frames,
                (double) elapsed / frames
        );
    }

    @Test
    void repeatedInitializationSignalsInstallOnlyOnce() {
        AtomicInteger installs = new AtomicInteger();
        ObsOverlayLifecycle lifecycle = new ObsOverlayLifecycle(() -> {
            installs.incrementAndGet();
            return new FakeInstallation(false, false);
        });

        lifecycle.onConfigLoaded(true);
        lifecycle.onConfigLoaded(true);
        lifecycle.onClientStarted();
        lifecycle.onClientStarted();
        lifecycle.onConfigLoaded(true);

        assertEquals(ObsOverlayLifecycleState.ACTIVE, lifecycle.state());
        assertEquals(1, installs.get());
    }

    @Test
    void installFailureRollsStateToFailedWithoutPretendingToBeActive() {
        IOException failure = new IOException("create failed");
        ObsOverlayLifecycle lifecycle = new ObsOverlayLifecycle(() -> {
            throw failure;
        });

        lifecycle.onConfigLoaded(true);
        lifecycle.onClientStarted();

        assertEquals(ObsOverlayLifecycleState.FAILED, lifecycle.state());
        assertEquals(failure, lifecycle.failure());
        assertFalse(lifecycle.retainsInstallation());
    }

    @Test
    void incompleteInstallRollbackRetainsResourcesUntilTheyCanBeRemoved() {
        FakeInstallation retained = new FakeInstallation(false, false);
        ObsOverlayLifecycle lifecycle = new ObsOverlayLifecycle(() -> {
            throw new ObsOverlayInstallException("rollback failed", new IOException("remove failed"), retained);
        });

        lifecycle.onConfigLoaded(true);
        lifecycle.onClientStarted();

        assertEquals(ObsOverlayLifecycleState.FAILED, lifecycle.state());
        assertTrue(lifecycle.retainsInstallation());

        lifecycle.onConfigLoaded(false);
        assertEquals(ObsOverlayLifecycleState.DISABLED, lifecycle.state());
        assertEquals(1, retained.uninstalls.get());
        assertFalse(lifecycle.retainsInstallation());
    }

    @Test
    void removeFailureKeepsInstallationStronglyReachableForRetry() {
        FakeInstallation installed = new FakeInstallation(false, false);
        installed.failUninstall = true;
        ObsOverlayLifecycle lifecycle = new ObsOverlayLifecycle(() -> installed);
        lifecycle.onConfigLoaded(true);
        lifecycle.onClientStarted();

        lifecycle.onConfigLoaded(false);
        assertEquals(ObsOverlayLifecycleState.FAILED, lifecycle.state());
        assertTrue(lifecycle.retainsInstallation());
        assertNotNull(lifecycle.failure());

        installed.failUninstall = false;
        lifecycle.onConfigLoaded(false);
        assertEquals(ObsOverlayLifecycleState.DISABLED, lifecycle.state());
        assertEquals(2, installed.uninstalls.get());
        assertFalse(lifecycle.retainsInstallation());
    }

    @Test
    void noObsModuleIsAHealthyActiveState() {
        ObsOverlayLifecycle lifecycle = new ObsOverlayLifecycle(
                () -> new FakeInstallation(false, false)
        );

        lifecycle.onConfigLoaded(true);
        lifecycle.onClientStarted();

        assertEquals(ObsOverlayLifecycleState.ACTIVE, lifecycle.state());
        assertFalse(lifecycle.isObsCaptureLoaded());
    }

    @Test
    void activeHookForwardsTargetSwapScope() {
        FakeInstallation installed = new FakeInstallation(false, false);
        ObsOverlayLifecycle lifecycle = new ObsOverlayLifecycle(() -> installed);

        lifecycle.onConfigLoaded(true);
        lifecycle.onClientStarted();
        assertEquals(ObsOverlayLifecycleState.ACTIVE, lifecycle.state());
        ObsOverlayInstallation armed = lifecycle.armTargetSwap();
        assertEquals(installed, armed);
        armed.disarmTargetSwap();
        assertEquals(1, installed.targetSwapArms.get());
        assertEquals(1, installed.targetSwapDisarms.get());
    }

    @Test
    void armedScopeRetainsItsOwnerAcrossLifecycleUninstall() {
        FakeInstallation installed = new FakeInstallation(false, false);
        ObsOverlayLifecycle lifecycle = new ObsOverlayLifecycle(() -> installed);
        lifecycle.onConfigLoaded(true);
        lifecycle.onClientStarted();

        ObsOverlayInstallation armed = lifecycle.armTargetSwap();
        lifecycle.onConfigLoaded(false);
        armed.disarmTargetSwap();

        assertEquals(ObsOverlayLifecycleState.DISABLED, lifecycle.state());
        assertEquals(1, installed.uninstalls.get());
        assertEquals(1, installed.targetSwapDisarms.get());
    }

    @Test
    void clientExitUninstallsExactlyOnce() {
        FakeInstallation installed = new FakeInstallation(false, true);
        ObsOverlayLifecycle lifecycle = new ObsOverlayLifecycle(() -> installed);
        lifecycle.onConfigLoaded(true);
        lifecycle.onClientStarted();

        lifecycle.stop();
        lifecycle.stop();

        assertEquals(ObsOverlayLifecycleState.DISABLED, lifecycle.state());
        assertEquals(1, installed.uninstalls.get());
        assertFalse(lifecycle.retainsInstallation());
    }

    @Test
    void configUnloadRemovesActiveInstallationAndWaitsForReload() {
        FakeInstallation installed = new FakeInstallation(false, false);
        ObsOverlayLifecycle lifecycle = new ObsOverlayLifecycle(() -> installed);
        lifecycle.onConfigLoaded(true);
        lifecycle.onClientStarted();

        lifecycle.onConfigUnloaded();

        assertEquals(ObsOverlayLifecycleState.WAITING_FOR_CONFIG, lifecycle.state());
        assertEquals(1, installed.uninstalls.get());
    }

    @Test
    void installingAndUninstallingAreExplicitTransitions() {
        AtomicReference<ObsOverlayLifecycle> lifecycleRef = new AtomicReference<>();
        FakeInstallation installed = new FakeInstallation(false, false);
        installed.beforeUninstall = () -> assertEquals(
                ObsOverlayLifecycleState.UNINSTALLING,
                lifecycleRef.get().state()
        );
        ObsOverlayLifecycle lifecycle = new ObsOverlayLifecycle(() -> {
            assertEquals(ObsOverlayLifecycleState.INSTALLING, lifecycleRef.get().state());
            return installed;
        });
        lifecycleRef.set(lifecycle);

        lifecycle.onConfigLoaded(true);
        lifecycle.onClientStarted();
        lifecycle.onConfigLoaded(false);

        assertEquals(ObsOverlayLifecycleState.DISABLED, lifecycle.state());
    }

    @Test
    void lateCallbackFailureCannotReviveAStoppedInstallation() {
        ObsOverlayLifecycle lifecycle = new ObsOverlayLifecycle(
                () -> new FakeInstallation(false, false)
        );
        lifecycle.onConfigLoaded(true);
        lifecycle.onClientStarted();
        lifecycle.onConfigLoaded(false);

        lifecycle.fail(new IOException("late callback"));

        assertEquals(ObsOverlayLifecycleState.DISABLED, lifecycle.state());
        assertNull(lifecycle.failure());
    }

    private static final class FakeInstallation implements ObsOverlayInstallation {
        private final boolean unsafeCaptureOrder;
        private final boolean obsCaptureLoaded;
        private final AtomicInteger uninstalls = new AtomicInteger();
        private final AtomicInteger targetSwapArms = new AtomicInteger();
        private final AtomicInteger targetSwapDisarms = new AtomicInteger();
        private boolean failUninstall;
        private Runnable beforeUninstall = () -> { };

        private FakeInstallation(boolean unsafeCaptureOrder, boolean obsCaptureLoaded) {
            this.unsafeCaptureOrder = unsafeCaptureOrder;
            this.obsCaptureLoaded = obsCaptureLoaded;
        }

        @Override
        public boolean unsafeCaptureOrder() {
            return unsafeCaptureOrder;
        }

        @Override
        public boolean isObsCaptureLoaded() {
            return obsCaptureLoaded;
        }

        @Override
        public ObsOverlayInstallation armTargetSwap() {
            targetSwapArms.incrementAndGet();
            return this;
        }

        @Override
        public void disarmTargetSwap() {
            targetSwapDisarms.incrementAndGet();
        }

        @Override
        public void uninstall() throws Exception {
            beforeUninstall.run();
            uninstalls.incrementAndGet();
            if (failUninstall) {
                throw new IOException("remove failed");
            }
        }
    }
}
