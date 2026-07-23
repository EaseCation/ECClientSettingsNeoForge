package net.easecation.clientsettings.client;

import net.easecation.clientsettings.config.ObsOverlayConfig;
import net.easecation.clientsettings.feature.obsoverlay.ObsOverlayComponent;
import net.easecation.clientsettings.feature.obsoverlay.ObsOverlayScreen;
import net.easecation.clientsettings.feature.obsoverlay.ObsOverlaySettings;
import net.easecation.clientsettings.feature.obsoverlay.PlayerAliasColorMode;
import net.easecation.clientsettings.feature.obsoverlay.PlayerAliasFormat;
import net.easecation.clientsettings.feature.obsoverlay.PlayerNameTagMode;

import java.util.EnumMap;
import java.util.List;

final class ObsOverlaySettingsDraft {

    private boolean enabled;
    private boolean showTestMarker;
    private boolean failClosed;
    private PlayerNameTagMode playerNameTagMode;
    private PlayerAliasFormat playerAliasFormat;
    private PlayerAliasColorMode playerAliasColorMode;
    private boolean playerNameTagsAutoHide;
    private final EnumMap<ObsOverlayComponent, Boolean> components;
    private final EnumMap<ObsOverlayComponent, Boolean> autoHide;
    private boolean hideAllInGameScreens;
    private final EnumMap<ObsOverlayScreen, Boolean> screens;
    private boolean customHandledScreensEnabled;
    private List<String> customHandledScreenIds;

    ObsOverlaySettingsDraft(ObsOverlaySettings settings) {
        enabled = settings.enabled();
        showTestMarker = settings.showTestMarker();
        failClosed = settings.failClosed();
        playerNameTagMode = settings.playerNameTagMode();
        playerAliasFormat = settings.playerAliasFormat();
        playerAliasColorMode = settings.playerAliasColorMode();
        playerNameTagsAutoHide = settings.playerNameTagsAutoHide();
        components = new EnumMap<>(settings.components());
        autoHide = new EnumMap<>(settings.autoHide());
        hideAllInGameScreens = settings.hideAllInGameScreens();
        screens = new EnumMap<>(settings.screens());
        customHandledScreensEnabled = settings.customHandledScreensEnabled();
        customHandledScreenIds = settings.customHandledScreenIds();
    }

    static ObsOverlaySettingsDraft current() {
        return new ObsOverlaySettingsDraft(ObsOverlayConfig.current());
    }

    boolean enabled() {
        return enabled;
    }

    void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    boolean showTestMarker() {
        return showTestMarker;
    }

    void setShowTestMarker(boolean showTestMarker) {
        this.showTestMarker = showTestMarker;
    }

    boolean failClosed() {
        return failClosed;
    }

    void setFailClosed(boolean failClosed) {
        this.failClosed = failClosed;
    }

    PlayerNameTagMode playerNameTagMode() {
        return playerNameTagMode;
    }

    void setPlayerNameTagMode(PlayerNameTagMode mode) {
        playerNameTagMode = mode;
    }

    PlayerAliasFormat playerAliasFormat() {
        return playerAliasFormat;
    }

    void setPlayerAliasFormat(PlayerAliasFormat format) {
        playerAliasFormat = format;
    }

    PlayerAliasColorMode playerAliasColorMode() {
        return playerAliasColorMode;
    }

    void setPlayerAliasColorMode(PlayerAliasColorMode colorMode) {
        playerAliasColorMode = colorMode;
    }

    boolean playerNameTagsAutoHide() {
        return playerNameTagsAutoHide;
    }

    void setPlayerNameTagsAutoHide(boolean autoHide) {
        playerNameTagsAutoHide = autoHide;
    }

    boolean component(ObsOverlayComponent component) {
        return components.get(component);
    }

    void setComponent(ObsOverlayComponent component, boolean enabled) {
        if (component.internal()) {
            throw new IllegalArgumentException("Internal OBS components use dedicated settings: " + component);
        }
        components.put(component, enabled);
    }

    boolean autoHide(ObsOverlayComponent component) {
        return autoHide.get(component);
    }

    void setAutoHide(ObsOverlayComponent component, boolean enabled) {
        if (component.internal()) {
            throw new IllegalArgumentException("Internal OBS components use dedicated settings: " + component);
        }
        autoHide.put(component, enabled);
    }

    boolean hideAllInGameScreens() {
        return hideAllInGameScreens;
    }

    void setHideAllInGameScreens(boolean hideAllInGameScreens) {
        this.hideAllInGameScreens = hideAllInGameScreens;
    }

    boolean screen(ObsOverlayScreen screen) {
        return screens.get(screen);
    }

    void setScreen(ObsOverlayScreen screen, boolean enabled) {
        screens.put(screen, enabled);
    }

    boolean customHandledScreensEnabled() {
        return customHandledScreensEnabled;
    }

    void setCustomHandledScreensEnabled(boolean enabled) {
        customHandledScreensEnabled = enabled;
    }

    List<String> customHandledScreenIds() {
        return customHandledScreenIds;
    }

    void setCustomHandledScreenIds(List<String> ids) {
        customHandledScreenIds = List.copyOf(ids);
    }

    void save() {
        ObsOverlayConfig.save(new ObsOverlaySettings(
                enabled,
                showTestMarker,
                failClosed,
                playerNameTagMode,
                playerAliasFormat,
                playerAliasColorMode,
                playerNameTagsAutoHide,
                components,
                autoHide,
                hideAllInGameScreens,
                screens,
                customHandledScreensEnabled,
                customHandledScreenIds
        ));
    }
}
