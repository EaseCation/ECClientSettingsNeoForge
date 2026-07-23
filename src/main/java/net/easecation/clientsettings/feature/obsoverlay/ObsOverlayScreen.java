package net.easecation.clientsettings.feature.obsoverlay;

public enum ObsOverlayScreen {
    INVENTORY("inventory"),
    CREATIVE_INVENTORY("creative_inventory"),
    PAUSE_MENU("pause_menu"),
    COMMAND_BLOCK("command_block");

    private final String serializedName;

    ObsOverlayScreen(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
