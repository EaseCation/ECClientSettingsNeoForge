package net.easecation.clientsettings.feature.obsoverlay.nativehook;

import com.sun.jna.Pointer;
import com.sun.jna.win32.StdCallLibrary;

interface User32 extends StdCallLibrary {
    Pointer WindowFromDC(Pointer deviceContext);
}
