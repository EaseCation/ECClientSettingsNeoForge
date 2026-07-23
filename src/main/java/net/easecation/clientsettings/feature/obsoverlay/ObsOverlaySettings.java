package net.easecation.clientsettings.feature.obsoverlay;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public record ObsOverlaySettings(
        boolean enabled,
        boolean showTestMarker,
        boolean failClosed,
        Map<ObsOverlayComponent, Boolean> components,
        Map<ObsOverlayComponent, Boolean> autoHide,
        boolean hideAllInGameScreens,
        Map<ObsOverlayScreen, Boolean> screens,
        boolean customHandledScreensEnabled,
        List<String> customHandledScreenIds
) {

    public static final ObsOverlaySettings DEFAULT = defaults();

    public ObsOverlaySettings {
        components = immutableComponentMap(components, false);
        autoHide = immutableComponentMap(autoHide, true);
        screens = immutableScreenMap(screens);
        customHandledScreenIds = customHandledScreenIds.stream()
                .map(value -> value.trim().toLowerCase(Locale.ROOT))
                .filter(value -> !value.isEmpty())
                .distinct()
                .toList();
    }

    public boolean protects(ObsOverlayComponent component) {
        return enabled && components.get(component);
    }

    public boolean autoHides(ObsOverlayComponent component) {
        return component.autoHideSupported() && autoHide.get(component);
    }

    public boolean protects(ObsOverlayScreen screen) {
        return enabled && screens.get(screen);
    }

    private static ObsOverlaySettings defaults() {
        EnumMap<ObsOverlayComponent, Boolean> components = new EnumMap<>(ObsOverlayComponent.class);
        EnumMap<ObsOverlayComponent, Boolean> autoHide = new EnumMap<>(ObsOverlayComponent.class);
        for (ObsOverlayComponent component : ObsOverlayComponent.values()) {
            components.put(component, component.defaultEnabled());
            autoHide.put(component, component.autoHideSupported());
        }
        EnumMap<ObsOverlayScreen, Boolean> screens = new EnumMap<>(ObsOverlayScreen.class);
        for (ObsOverlayScreen screen : ObsOverlayScreen.values()) {
            screens.put(screen, false);
        }
        return new ObsOverlaySettings(false, true, true, components, autoHide, false, screens, false, List.of());
    }

    private static Map<ObsOverlayComponent, Boolean> immutableComponentMap(
            Map<ObsOverlayComponent, Boolean> source,
            boolean missingValue
    ) {
        EnumMap<ObsOverlayComponent, Boolean> copy = new EnumMap<>(ObsOverlayComponent.class);
        for (ObsOverlayComponent component : ObsOverlayComponent.values()) {
            copy.put(component, source.getOrDefault(component, missingValue));
        }
        return Collections.unmodifiableMap(copy);
    }

    private static Map<ObsOverlayScreen, Boolean> immutableScreenMap(Map<ObsOverlayScreen, Boolean> source) {
        EnumMap<ObsOverlayScreen, Boolean> copy = new EnumMap<>(ObsOverlayScreen.class);
        for (ObsOverlayScreen screen : ObsOverlayScreen.values()) {
            copy.put(screen, source.getOrDefault(screen, false));
        }
        return Collections.unmodifiableMap(copy);
    }
}
