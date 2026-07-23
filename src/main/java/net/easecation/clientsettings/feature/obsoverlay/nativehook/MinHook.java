package net.easecation.clientsettings.feature.obsoverlay.nativehook;

import com.sun.jna.Callback;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.win32.StdCallLibrary;

interface MinHook extends StdCallLibrary {
    int MH_Initialize();

    int MH_CreateHook(Pointer target, SwapBuffersCallback detour, PointerByReference original);

    int MH_EnableHook(Pointer target);

    int MH_DisableHook(Pointer target);

    int MH_RemoveHook(Pointer target);

    int MH_Uninitialize();

    interface SwapBuffersCallback extends Callback, StdCallLibrary.StdCallCallback {
        int invoke(Pointer deviceContext);
    }
}
