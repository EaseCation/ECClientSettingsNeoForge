package net.easecation.clientsettings.feature.obsoverlay.nativehook;

import org.lwjgl.system.CallbackI;
import org.lwjgl.system.libffi.FFICIF;

import static org.lwjgl.system.APIUtil.apiClosureRet;
import static org.lwjgl.system.APIUtil.apiCreateCIF;
import static org.lwjgl.system.APIUtil.apiStdcall;
import static org.lwjgl.system.MemoryUtil.memGetAddress;
import static org.lwjgl.system.libffi.LibFFI.ffi_type_pointer;
import static org.lwjgl.system.libffi.LibFFI.ffi_type_sint32;

@FunctionalInterface
interface SwapBuffersCallbackI extends CallbackI {

    FFICIF CIF = apiCreateCIF(apiStdcall(), ffi_type_sint32, ffi_type_pointer);

    @Override
    default FFICIF getCallInterface() {
        return CIF;
    }

    @Override
    default void callback(long returnValue, long arguments) {
        long deviceContext = memGetAddress(memGetAddress(arguments));
        apiClosureRet(returnValue, invoke(deviceContext));
    }

    int invoke(long deviceContext);
}
