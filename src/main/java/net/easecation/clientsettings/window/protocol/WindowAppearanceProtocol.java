package net.easecation.clientsettings.window.protocol;

public final class WindowAppearanceProtocol {

    public static final int VERSION = 1;
    public static final String NAMESPACE = "ECJavaEditionClientMod";
    public static final String SYSTEM = "ECClientWindowSystem";
    public static final String SET_APPEARANCE_EVENT = "SetWindowAppearanceEvent";
    public static final String RESET_APPEARANCE_EVENT = "ResetWindowAppearanceEvent";

    private WindowAppearanceProtocol() {
    }

    public static boolean isWindowSystem(String namespace, String system) {
        return NAMESPACE.equals(namespace) && SYSTEM.equals(system);
    }
}
