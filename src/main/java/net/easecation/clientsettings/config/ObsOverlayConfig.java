package net.easecation.clientsettings.config;

import net.easecation.clientsettings.feature.obsoverlay.ObsOverlayComponent;
import net.easecation.clientsettings.feature.obsoverlay.ObsOverlayRuntime;
import net.easecation.clientsettings.feature.obsoverlay.ObsOverlayScreen;
import net.easecation.clientsettings.feature.obsoverlay.ObsOverlaySettings;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class ObsOverlayConfig {

    public static final String FILE_NAME = "ecclientsettings-obs-overlay.toml";
    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.BooleanValue ENABLED;
    private static final ModConfigSpec.BooleanValue SHOW_TEST_MARKER;
    private static final ModConfigSpec.BooleanValue FAIL_CLOSED;
    private static final Map<ObsOverlayComponent, ModConfigSpec.BooleanValue> COMPONENTS =
            new EnumMap<>(ObsOverlayComponent.class);
    private static final Map<ObsOverlayComponent, ModConfigSpec.BooleanValue> AUTO_HIDE =
            new EnumMap<>(ObsOverlayComponent.class);
    private static final ModConfigSpec.BooleanValue HIDE_ALL_IN_GAME_SCREENS;
    private static final Map<ObsOverlayScreen, ModConfigSpec.BooleanValue> SCREENS =
            new EnumMap<>(ObsOverlayScreen.class);
    private static final ModConfigSpec.BooleanValue CUSTOM_HANDLED_SCREENS_ENABLED;
    private static final ModConfigSpec.ConfigValue<List<? extends String>> CUSTOM_HANDLED_SCREEN_IDS;

    private static volatile ObsOverlaySettings current = ObsOverlaySettings.DEFAULT;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.comment("OBS privacy overlay. Global by design so Profile switches cannot disable privacy controls.")
                .push("obsOverlay");
        ENABLED = builder.define("enabled", ObsOverlaySettings.DEFAULT.enabled());
        SHOW_TEST_MARKER = builder.define("showTestMarker", ObsOverlaySettings.DEFAULT.showTestMarker());
        FAIL_CLOSED = builder
                .comment("Hide protected elements locally if the native capture hook is unavailable or unsafe.")
                .define("failClosed", ObsOverlaySettings.DEFAULT.failClosed());

        builder.push("components");
        for (ObsOverlayComponent component : ObsOverlayComponent.values()) {
            COMPONENTS.put(component, builder.define(component.serializedName(), component.defaultEnabled()));
        }
        builder.pop();

        builder.push("autoHide");
        for (ObsOverlayComponent component : ObsOverlayComponent.values()) {
            if (component.autoHideSupported()) {
                AUTO_HIDE.put(component, builder.define(component.serializedName(), true));
            }
        }
        builder.pop();

        builder.push("screens");
        HIDE_ALL_IN_GAME_SCREENS = builder.define("allInGameScreens", false);
        for (ObsOverlayScreen screen : ObsOverlayScreen.values()) {
            SCREENS.put(screen, builder.define(screen.serializedName(), false));
        }
        CUSTOM_HANDLED_SCREENS_ENABLED = builder.define("customHandledScreensEnabled", false);
        CUSTOM_HANDLED_SCREEN_IDS = builder.defineListAllowEmpty(
                "customHandledScreenIds",
                List.of(),
                () -> "",
                value -> value instanceof String
        );
        builder.pop();
        builder.pop();
        SPEC = builder.build();
    }

    private ObsOverlayConfig() {
    }

    public static ObsOverlaySettings current() {
        return current;
    }

    public static void refresh() {
        current = read();
    }

    public static void save(ObsOverlaySettings settings) {
        ENABLED.set(settings.enabled());
        SHOW_TEST_MARKER.set(settings.showTestMarker());
        FAIL_CLOSED.set(settings.failClosed());
        COMPONENTS.forEach((component, value) -> value.set(settings.components().get(component)));
        AUTO_HIDE.forEach((component, value) -> value.set(settings.autoHide().get(component)));
        HIDE_ALL_IN_GAME_SCREENS.set(settings.hideAllInGameScreens());
        SCREENS.forEach((screen, value) -> value.set(settings.screens().get(screen)));
        CUSTOM_HANDLED_SCREENS_ENABLED.set(settings.customHandledScreensEnabled());
        CUSTOM_HANDLED_SCREEN_IDS.set(settings.customHandledScreenIds());
        SPEC.save();
        current = settings;
        ObsOverlayRuntime.onSettingsChanged(settings);
    }

    private static ObsOverlaySettings read() {
        EnumMap<ObsOverlayComponent, Boolean> components = new EnumMap<>(ObsOverlayComponent.class);
        EnumMap<ObsOverlayComponent, Boolean> autoHide = new EnumMap<>(ObsOverlayComponent.class);
        for (ObsOverlayComponent component : ObsOverlayComponent.values()) {
            components.put(component, COMPONENTS.get(component).get());
            ModConfigSpec.BooleanValue autoHideValue = AUTO_HIDE.get(component);
            autoHide.put(component, autoHideValue != null && autoHideValue.get());
        }
        EnumMap<ObsOverlayScreen, Boolean> screens = new EnumMap<>(ObsOverlayScreen.class);
        SCREENS.forEach((screen, value) -> screens.put(screen, value.get()));
        return new ObsOverlaySettings(
                ENABLED.get(),
                SHOW_TEST_MARKER.get(),
                FAIL_CLOSED.get(),
                components,
                autoHide,
                HIDE_ALL_IN_GAME_SCREENS.get(),
                screens,
                CUSTOM_HANDLED_SCREENS_ENABLED.get(),
                CUSTOM_HANDLED_SCREEN_IDS.get().stream().map(String::trim).filter(value -> !value.isEmpty()).toList()
        );
    }
}
