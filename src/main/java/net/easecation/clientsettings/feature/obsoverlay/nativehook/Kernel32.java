package net.easecation.clientsettings.feature.obsoverlay.nativehook;

import com.sun.jna.Pointer;
import com.sun.jna.win32.StdCallLibrary;

interface Kernel32 extends StdCallLibrary {
    Pointer GetModuleHandleA(String moduleName);

    Pointer GetProcAddress(Pointer module, String procedureName);
}
