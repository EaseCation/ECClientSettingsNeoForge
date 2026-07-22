package net.easecation.clientsettings.profile.model;

public enum HudWidgetId {
    ARMOR("armor"),
    POTIONS("potions"),
    PING("ping"),
    FPS("fps"),
    KEYSTROKES("keystrokes");

    private final String serializedName;

    HudWidgetId(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
