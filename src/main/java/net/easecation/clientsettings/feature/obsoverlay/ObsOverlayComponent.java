package net.easecation.clientsettings.feature.obsoverlay;

public enum ObsOverlayComponent {
    DEBUG_MENU("debug_menu", Group.HUD, true, true),
    CHAT("chat", Group.HUD, false, true),
    CHAT_INPUT("chat_input", Group.HUD, false, false),
    PLAYER_LIST("player_list", Group.HUD, false, false),
    SUBTITLES("subtitles", Group.HUD, false, true),
    SCOREBOARD("scoreboard", Group.HUD, false, true),
    ACTION_BAR("action_bar", Group.HUD, false, true),
    TITLE("title", Group.HUD, false, true),
    STATUS_EFFECTS("status_effects", Group.HUD, false, true),
    MAIN_HUD("main_hud", Group.HUD, false, true),
    EC_HUD("ec_hud", Group.HUD, false, true),
    NAME_TAGS("name_tags", Group.WORLD, false, true),
    SNEAKING_NAME_TAGS("sneaking_name_tags", Group.WORLD, false, true),
    SIGN_TEXT("sign_text", Group.WORLD, false, true),
    CHESTS("chests", Group.WORLD, false, true),
    ITEM_FRAME_MAPS("item_frame_maps", Group.WORLD, false, true),
    BANNER_PATTERNS("banner_patterns", Group.WORLD, false, true),
    BEACON_BEAMS("beacon_beams", Group.WORLD, false, true);

    private final String serializedName;
    private final Group group;
    private final boolean defaultEnabled;
    private final boolean autoHideSupported;

    ObsOverlayComponent(String serializedName, Group group, boolean defaultEnabled, boolean autoHideSupported) {
        this.serializedName = serializedName;
        this.group = group;
        this.defaultEnabled = defaultEnabled;
        this.autoHideSupported = autoHideSupported;
    }

    public String serializedName() {
        return serializedName;
    }

    public Group group() {
        return group;
    }

    public boolean defaultEnabled() {
        return defaultEnabled;
    }

    public boolean autoHideSupported() {
        return autoHideSupported;
    }

    public enum Group {
        HUD,
        WORLD
    }
}
