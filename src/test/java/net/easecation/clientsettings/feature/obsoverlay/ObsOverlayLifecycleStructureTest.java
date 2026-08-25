package net.easecation.clientsettings.feature.obsoverlay;

import net.easecation.clientsettings.config.ObsOverlayConfig;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObsOverlayLifecycleStructureTest {

    @Test
    void unloadedConfigCannotBeReadThroughAPlaceholderDefault() {
        assertFalse(ObsOverlayConfig.isLoaded());
        assertNull(ObsOverlayConfig.currentOrNull());
        assertThrows(IllegalStateException.class, ObsOverlayConfig::current);
    }

    @Test
    void initialBufferSwapPassesThroughBeforeConfigLoads() {
        assertFalse(ObsOverlayConfig.isLoaded());
        assertTrue(ObsOverlayRuntime.preparePublicFrameForCapture());
    }

    @Test
    void runtimeClassDoesNotResolveNativeHookOrWin32GlfwTypes() throws IOException {
        String classFile = classBytes(ObsOverlayRuntime.class);

        assertFalse(classFile.contains("/nativehook/"));
        assertFalse(classFile.contains("GLFWNativeWin32"));
        assertTrue(classFile.contains("ObsOverlayNativeProvider"));
    }

    @Test
    void nativeProviderKeepsImplementationBehindItsLazyHolder() throws IOException {
        String providerClass = classBytes(ObsOverlayNativeProvider.class);

        assertFalse(providerClass.contains("/nativehook/"));
        assertTrue(providerClass.contains("ObsOverlayNativeProvider$Holder"));
    }

    @Test
    void renderSwapScopeArmsAndDisarmsAroundTheOriginalCall() throws IOException {
        String classFile = classBytes(
                "/net/easecation/clientsettings/mixin/obsoverlay/RenderSystemMixin.class"
        );

        assertTrue(classFile.contains("armTargetSwap"));
        assertTrue(classFile.contains("disarmTargetSwap"));
        assertTrue(classFile.contains("preparePublicFrameForCapture"));
    }

    private static String classBytes(Class<?> type) throws IOException {
        String resource = "/" + type.getName().replace('.', '/') + ".class";
        return classBytes(resource);
    }

    private static String classBytes(String resource) throws IOException {
        try (InputStream input = ObsOverlayLifecycleStructureTest.class.getResourceAsStream(resource)) {
            if (input == null) {
                throw new IOException("Missing class resource " + resource);
            }
            return new String(input.readAllBytes(), StandardCharsets.ISO_8859_1);
        }
    }
}
