package net.easecation.clientsettings.window.windows;

import net.easecation.clientsettings.ECClientSettings;
import net.easecation.clientsettings.window.WindowFrameBackend;
import net.easecation.clientsettings.window.protocol.FrameAppearance;

import java.util.OptionalInt;
import java.util.function.LongSupplier;

public final class WindowsWindowFrameBackend implements WindowFrameBackend {

    static final int DWMWA_USE_IMMERSIVE_DARK_MODE_OLD = 19;
    static final int DWMWA_USE_IMMERSIVE_DARK_MODE = 20;
    static final int DWMWA_CAPTION_COLOR = 35;
    static final int DWMWA_TEXT_COLOR = 36;
    static final int DWM_COLOR_DEFAULT = 0xFFFFFFFF;

    private final DwmFacade dwm;
    private final LongSupplier hwndSupplier;
    private Baseline baseline;
    private boolean failureLogged;

    public WindowsWindowFrameBackend(DwmFacade dwm, LongSupplier hwndSupplier) {
        this.dwm = dwm;
        this.hwndSupplier = hwndSupplier;
    }

    @Override
    public void apply(FrameAppearance appearance) {
        long hwnd = hwndSupplier.getAsLong();
        if (hwnd == 0L) {
            logFailureOnce("Cannot customize the title bar because GLFW returned no Win32 HWND");
            return;
        }
        if (appearance.mode() == FrameAppearance.Mode.SYSTEM) {
            restore();
            return;
        }

        Baseline current = ensureBaseline(hwnd);
        restoreValues(current);
        switch (appearance.mode()) {
            case DARK -> applyDark(current);
            case RGB -> applyRgb(current, appearance);
            case SYSTEM -> throw new IllegalStateException("System mode should have restored the baseline");
        }
    }

    @Override
    public void restore() {
        if (baseline == null) {
            return;
        }
        restoreValues(baseline);
        baseline = null;
        failureLogged = false;
    }

    private void applyRgb(Baseline current, FrameAppearance appearance) {
        boolean backgroundApplied = dwm.setInt(
                current.hwnd(), DWMWA_CAPTION_COLOR, rgbToColorRef(appearance.background())
        );
        boolean textApplied = backgroundApplied && dwm.setInt(
                current.hwnd(), DWMWA_TEXT_COLOR, rgbToColorRef(appearance.effectiveTextColor())
        );
        if (backgroundApplied && textApplied) {
            return;
        }

        restoreValues(current);
        if (appearance.fallback() == FrameAppearance.Fallback.DARK) {
            applyDark(current);
        } else {
            baseline = null;
        }
        logFailureOnce("Exact Windows title bar colors are unavailable; applied the requested fallback");
    }

    private void applyDark(Baseline current) {
        if (dwm.setInt(current.hwnd(), DWMWA_USE_IMMERSIVE_DARK_MODE, 1)) {
            return;
        }
        if (dwm.setInt(current.hwnd(), DWMWA_USE_IMMERSIVE_DARK_MODE_OLD, 1)) {
            return;
        }
        restoreValues(current);
        baseline = null;
        logFailureOnce("Windows dark title bar mode is unavailable; restored the system appearance");
    }

    private Baseline ensureBaseline(long hwnd) {
        if (baseline == null || baseline.hwnd() != hwnd) {
            baseline = new Baseline(
                    hwnd,
                    capture(hwnd, DWMWA_USE_IMMERSIVE_DARK_MODE),
                    capture(hwnd, DWMWA_USE_IMMERSIVE_DARK_MODE_OLD),
                    capture(hwnd, DWMWA_CAPTION_COLOR),
                    capture(hwnd, DWMWA_TEXT_COLOR)
            );
        }
        return baseline;
    }

    private CapturedValue capture(long hwnd, int attribute) {
        OptionalInt value = dwm.getInt(hwnd, attribute);
        return value.isPresent() ? new CapturedValue(true, value.getAsInt()) : new CapturedValue(false, 0);
    }

    private void restoreValues(Baseline values) {
        restoreAttribute(values.hwnd(), DWMWA_CAPTION_COLOR, values.captionColor(), DWM_COLOR_DEFAULT);
        restoreAttribute(values.hwnd(), DWMWA_TEXT_COLOR, values.textColor(), DWM_COLOR_DEFAULT);
        restoreAttribute(values.hwnd(), DWMWA_USE_IMMERSIVE_DARK_MODE, values.darkMode(), 0);
        restoreAttribute(values.hwnd(), DWMWA_USE_IMMERSIVE_DARK_MODE_OLD, values.oldDarkMode(), 0);
    }

    private void restoreAttribute(long hwnd, int attribute, CapturedValue captured, int defaultValue) {
        dwm.setInt(hwnd, attribute, captured.present() ? captured.value() : defaultValue);
    }

    static int rgbToColorRef(int rgb) {
        return rgb & 0x00FF00 | (rgb & 0xFF0000) >> 16 | (rgb & 0x0000FF) << 16;
    }

    private void logFailureOnce(String message) {
        if (!failureLogged) {
            failureLogged = true;
            ECClientSettings.LOGGER.debug(message);
        }
    }

    private record CapturedValue(boolean present, int value) {
    }

    private record Baseline(
            long hwnd,
            CapturedValue darkMode,
            CapturedValue oldDarkMode,
            CapturedValue captionColor,
            CapturedValue textColor
    ) {
    }
}
