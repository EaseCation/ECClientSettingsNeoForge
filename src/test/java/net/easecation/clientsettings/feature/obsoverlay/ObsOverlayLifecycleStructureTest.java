package net.easecation.clientsettings.feature.obsoverlay;

import net.easecation.clientsettings.config.ObsOverlayConfig;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObsOverlayLifecycleStructureTest {

    @Test
    void unloadedConfigCannotBeReadThroughAPlaceholderDefault() {
        assertFalse(ObsOverlayConfig.isLoaded());
        assertThrows(IllegalStateException.class, ObsOverlayConfig::current);
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

    private static String classBytes(Class<?> type) throws IOException {
        String resource = "/" + type.getName().replace('.', '/') + ".class";
        try (InputStream input = type.getResourceAsStream(resource)) {
            if (input == null) {
                throw new IOException("Missing class resource " + resource);
            }
            return new String(input.readAllBytes(), StandardCharsets.ISO_8859_1);
        }
    }
}
