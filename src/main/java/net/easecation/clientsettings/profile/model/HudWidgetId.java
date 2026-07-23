package net.easecation.clientsettings.profile.model;

public enum HudWidgetId {
    ARMOR("armor"),
    POTIONS("potions"),
    PING("ping"),
    FPS("fps"),
    LEFT_CPS("left_cps"),
    RIGHT_CPS("right_cps"),
    KEYSTROKES("keystrokes");

    private final String serializedName;

    HudWidgetId(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
