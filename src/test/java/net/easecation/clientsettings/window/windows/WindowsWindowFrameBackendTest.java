package net.easecation.clientsettings.window.windows;

import net.easecation.clientsettings.window.protocol.FrameAppearance;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class WindowsWindowFrameBackendTest {

    private static final long HWND = 42L;

    @Test
    void appliesAndRestoresExactRgbColors() {
        FakeDwm dwm = baselineDwm();
        WindowsWindowFrameBackend backend = new WindowsWindowFrameBackend(dwm, () -> HWND);

        backend.apply(FrameAppearance.rgb(0x123456, 0xABCDEF, FrameAppearance.Fallback.SYSTEM));

        assertEquals(0x563412, dwm.values.get(WindowsWindowFrameBackend.DWMWA_CAPTION_COLOR));
        assertEquals(0xEFCDAB, dwm.values.get(WindowsWindowFrameBackend.DWMWA_TEXT_COLOR));
        backend.restore();
        assertEquals(0x010203, dwm.values.get(WindowsWindowFrameBackend.DWMWA_CAPTION_COLOR));
        assertEquals(0xF0F0F0, dwm.values.get(WindowsWindowFrameBackend.DWMWA_TEXT_COLOR));
    }

    @Test
    void rollsBackPartialRgbAndFallsBackToDark() {
        FakeDwm dwm = baselineDwm();
        dwm.failOnInvocation.put(WindowsWindowFrameBackend.DWMWA_TEXT_COLOR, 2);
        WindowsWindowFrameBackend backend = new WindowsWindowFrameBackend(dwm, () -> HWND);

        backend.apply(FrameAppearance.rgb(0x181818, 0xFFFFFF, FrameAppearance.Fallback.DARK));

        assertEquals(0x010203, dwm.values.get(WindowsWindowFrameBackend.DWMWA_CAPTION_COLOR));
        assertEquals(0xF0F0F0, dwm.values.get(WindowsWindowFrameBackend.DWMWA_TEXT_COLOR));
        assertEquals(1, dwm.values.get(WindowsWindowFrameBackend.DWMWA_USE_IMMERSIVE_DARK_MODE));
    }

    @Test
    void usesLegacyDarkAttributeWhenModernAttributeFails() {
        FakeDwm dwm = baselineDwm();
        dwm.alwaysFail.add(WindowsWindowFrameBackend.DWMWA_USE_IMMERSIVE_DARK_MODE);
        WindowsWindowFrameBackend backend = new WindowsWindowFrameBackend(dwm, () -> HWND);

        backend.apply(FrameAppearance.dark());

        assertEquals(1, dwm.values.get(WindowsWindowFrameBackend.DWMWA_USE_IMMERSIVE_DARK_MODE_OLD));
    }

    @Test
    void restoresSystemWhenRgbAndDarkFallbackAreUnsupported() {
        FakeDwm dwm = baselineDwm();
        dwm.alwaysFail.add(WindowsWindowFrameBackend.DWMWA_CAPTION_COLOR);
        dwm.alwaysFail.add(WindowsWindowFrameBackend.DWMWA_USE_IMMERSIVE_DARK_MODE);
        dwm.alwaysFail.add(WindowsWindowFrameBackend.DWMWA_USE_IMMERSIVE_DARK_MODE_OLD);
        WindowsWindowFrameBackend backend = new WindowsWindowFrameBackend(dwm, () -> HWND);

        backend.apply(FrameAppearance.rgb(0, 0xFFFFFF, FrameAppearance.Fallback.DARK));

        assertEquals(0, dwm.values.get(WindowsWindowFrameBackend.DWMWA_USE_IMMERSIVE_DARK_MODE));
        assertEquals(0, dwm.values.get(WindowsWindowFrameBackend.DWMWA_USE_IMMERSIVE_DARK_MODE_OLD));
        assertEquals(0x010203, dwm.values.get(WindowsWindowFrameBackend.DWMWA_CAPTION_COLOR));
    }

    @Test
    void ignoresMissingWindowHandle() {
        FakeDwm dwm = baselineDwm();
        WindowsWindowFrameBackend backend = new WindowsWindowFrameBackend(dwm, () -> 0L);

        backend.apply(FrameAppearance.dark());

        assertEquals(0, dwm.setCalls);
    }

    @Test
    void convertsRgbToColorRef() {
        assertEquals(0x563412, WindowsWindowFrameBackend.rgbToColorRef(0x123456));
        assertEquals(0x181818, WindowsWindowFrameBackend.rgbToColorRef(0x181818));
    }

    private static FakeDwm baselineDwm() {
        FakeDwm dwm = new FakeDwm();
        dwm.values.put(WindowsWindowFrameBackend.DWMWA_USE_IMMERSIVE_DARK_MODE, 0);
        dwm.values.put(WindowsWindowFrameBackend.DWMWA_USE_IMMERSIVE_DARK_MODE_OLD, 0);
        dwm.values.put(WindowsWindowFrameBackend.DWMWA_CAPTION_COLOR, 0x010203);
        dwm.values.put(WindowsWindowFrameBackend.DWMWA_TEXT_COLOR, 0xF0F0F0);
        return dwm;
    }

    private static final class FakeDwm implements DwmFacade {
        private final Map<Integer, Integer> values = new HashMap<>();
        private final Set<Integer> alwaysFail = new HashSet<>();
        private final Map<Integer, Integer> failOnInvocation = new HashMap<>();
        private final Map<Integer, Integer> invocationCounts = new HashMap<>();
        private int setCalls;

        @Override
        public OptionalInt getInt(long hwnd, int attribute) {
            Integer value = values.get(attribute);
            return value == null ? OptionalInt.empty() : OptionalInt.of(value);
        }

        @Override
        public boolean setInt(long hwnd, int attribute, int value) {
            setCalls++;
            int invocation = invocationCounts.merge(attribute, 1, Integer::sum);
            if (alwaysFail.contains(attribute) || failOnInvocation.getOrDefault(attribute, -1) == invocation) {
                return false;
            }
            values.put(attribute, value);
            return true;
        }
    }
}
