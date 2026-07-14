package net.easecation.clientsettings.window;

import net.easecation.clientsettings.ECClientSettings;
import net.easecation.clientsettings.window.protocol.ParseResult;
import net.easecation.clientsettings.window.protocol.WindowAppearanceCommand;
import net.easecation.clientsettings.window.protocol.WindowAppearanceParser;
import net.easecation.clientsettings.window.protocol.WindowAppearanceProtocol;
import net.easecation.neteasebridge.client.neoforge.NeteaseRpcDisconnectedEvent;
import net.easecation.neteasebridge.client.neoforge.NeteaseRpcModEventS2CEvent;

public final class WindowAppearanceEvents {

    private WindowAppearanceEvents() {
    }

    public static void onModEvent(NeteaseRpcModEventS2CEvent event) {
        var modEvent = event.getModEvent();
        if (!WindowAppearanceProtocol.isWindowSystem(modEvent.namespace(), modEvent.system())) {
            return;
        }

        switch (modEvent.eventName()) {
            case WindowAppearanceProtocol.SET_APPEARANCE_EVENT -> handleSet(modEvent.data());
            case WindowAppearanceProtocol.RESET_APPEARANCE_EVENT -> {
                ParseResult<Boolean> result = WindowAppearanceParser.parseReset(modEvent.data());
                if (result.successful()) {
                    WindowAppearanceController.getInstance().reset();
                } else {
                    logInvalid(result.error());
                }
            }
            default -> {
            }
        }
    }

    public static void onDisconnected(NeteaseRpcDisconnectedEvent event) {
        WindowAppearanceController.getInstance().disconnect();
    }

    private static void handleSet(org.msgpack.value.Value data) {
        ParseResult<WindowAppearanceCommand> result = WindowAppearanceParser.parseSet(data);
        if (!result.successful()) {
            logInvalid(result.error());
            return;
        }

        WindowAppearanceController.ApplyResult applied = WindowAppearanceController.getInstance().apply(result.value());
        if (applied == WindowAppearanceController.ApplyResult.RATE_LIMITED) {
            ECClientSettings.LOGGER.debug("Dropped a rate-limited server window appearance update");
        }
    }

    private static void logInvalid(String reason) {
        ECClientSettings.LOGGER.debug("Ignored invalid server window appearance event: {}", reason);
    }
}
