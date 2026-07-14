package net.easecation.clientsettings.window.windows;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinNT.HRESULT;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.StdCallLibrary;

import java.util.OptionalInt;

public final class JnaDwmFacade implements DwmFacade {

    @Override
    public OptionalInt getInt(long hwnd, int attribute) {
        IntByReference value = new IntByReference();
        HRESULT result = DwmLibrary.INSTANCE.DwmGetWindowAttribute(
                hwnd(hwnd), attribute, value.getPointer(), Integer.BYTES
        );
        return succeeded(result) ? OptionalInt.of(value.getValue()) : OptionalInt.empty();
    }

    @Override
    public boolean setInt(long hwnd, int attribute, int value) {
        IntByReference data = new IntByReference(value);
        return succeeded(DwmLibrary.INSTANCE.DwmSetWindowAttribute(
                hwnd(hwnd), attribute, data.getPointer(), Integer.BYTES
        ));
    }

    private static HWND hwnd(long value) {
        return new HWND(Pointer.createConstant(value));
    }

    private static boolean succeeded(HRESULT result) {
        return result != null && result.intValue() >= 0;
    }

    private interface DwmLibrary extends StdCallLibrary {
        DwmLibrary INSTANCE = Native.load("dwmapi", DwmLibrary.class);

        HRESULT DwmGetWindowAttribute(HWND hwnd, int attribute, Pointer value, int valueSize);

        HRESULT DwmSetWindowAttribute(HWND hwnd, int attribute, Pointer value, int valueSize);
    }
}
