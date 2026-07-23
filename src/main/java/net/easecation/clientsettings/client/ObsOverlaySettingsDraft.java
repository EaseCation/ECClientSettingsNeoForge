package net.easecation.clientsettings.client;

import net.easecation.clientsettings.config.ObsOverlayConfig;
import net.easecation.clientsettings.feature.obsoverlay.ObsOverlayComponent;
import net.easecation.clientsettings.feature.obsoverlay.ObsOverlayScreen;
import net.easecation.clientsettings.feature.obsoverlay.ObsOverlaySettings;

import java.util.EnumMap;
import java.util.List;

final class ObsOverlaySettingsDraft {

    private boolean enabled;
    private boolean showTestMarker;
    private boolean failClosed;
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

    boolean component(ObsOverlayComponent component) {
        return components.get(component);
    }

    void setComponent(ObsOverlayComponent component, boolean enabled) {
        components.put(component, enabled);
    }

    boolean autoHide(ObsOverlayComponent component) {
        return autoHide.get(component);
    }

    void setAutoHide(ObsOverlayComponent component, boolean enabled) {
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
                components,
                autoHide,
                hideAllInGameScreens,
                screens,
                customHandledScreensEnabled,
                customHandledScreenIds
        ));
    }
}
