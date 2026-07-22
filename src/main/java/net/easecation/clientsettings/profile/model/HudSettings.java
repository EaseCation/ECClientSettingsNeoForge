package net.easecation.clientsettings.profile.model;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public record HudSettings(
        Map<HudWidgetId, HudWidgetSettings> widgets,
        KeystrokesSettings keystrokes
) {

    public static final HudSettings DEFAULT = new HudSettings(Map.of(
            HudWidgetId.ARMOR, new HudWidgetSettings(
                    false, 1.0, 1.0, 1.0, HudWidgetStyle.defaultsFor(HudWidgetId.ARMOR)
            ),
            HudWidgetId.POTIONS, new HudWidgetSettings(
                    false, 0.0, 0.25, 1.0, HudWidgetStyle.defaultsFor(HudWidgetId.POTIONS)
            ),
            HudWidgetId.PING, new HudWidgetSettings(
                    false, 1.0, 0.0, 1.0, HudWidgetStyle.defaultsFor(HudWidgetId.PING)
            ),
            HudWidgetId.FPS, new HudWidgetSettings(
                    false, 0.0, 0.0, 1.0, HudWidgetStyle.defaultsFor(HudWidgetId.FPS)
            ),
            HudWidgetId.KEYSTROKES, new HudWidgetSettings(
                    false, 0.5, 0.08, 1.0, HudWidgetStyle.defaultsFor(HudWidgetId.KEYSTROKES)
            )
    ), KeystrokesSettings.DEFAULT);

    public HudSettings(Map<HudWidgetId, HudWidgetSettings> widgets) {
        this(widgets, KeystrokesSettings.DEFAULT);
    }

    public HudSettings {
        ProfileValidation.requireNonNull(widgets, "hud.widgets");
        EnumMap<HudWidgetId, HudWidgetSettings> copy = new EnumMap<>(HudWidgetId.class);
        for (Map.Entry<HudWidgetId, HudWidgetSettings> entry : widgets.entrySet()) {
            HudWidgetId id = ProfileValidation.requireNonNull(entry.getKey(), "hud.widgetId");
            HudWidgetSettings settings = ProfileValidation.requireNonNull(
                    entry.getValue(), "hud." + id.serializedName()
            );
            copy.put(id, settings);
        }
        for (HudWidgetId id : HudWidgetId.values()) {
            if (!copy.containsKey(id)) {
                throw new IllegalArgumentException("hud is missing widget: " + id.serializedName());
            }
        }
        widgets = Collections.unmodifiableMap(copy);
        keystrokes = ProfileValidation.requireNonNull(keystrokes, "hud.keystrokes");
    }

    public HudWidgetSettings widget(HudWidgetId id) {
        return widgets.get(ProfileValidation.requireNonNull(id, "hud.widgetId"));
    }

    public HudSettings withWidget(HudWidgetId id, HudWidgetSettings settings) {
        EnumMap<HudWidgetId, HudWidgetSettings> updated = new EnumMap<>(widgets);
        updated.put(
                ProfileValidation.requireNonNull(id, "hud.widgetId"),
                ProfileValidation.requireNonNull(settings, "hud.widget")
        );
        return new HudSettings(updated, keystrokes);
    }

    public HudSettings withEnabled(HudWidgetId id, boolean enabled) {
        HudWidgetSettings current = widget(id);
        return withWidget(id, new HudWidgetSettings(
                enabled, current.normalizedX(), current.normalizedY(), current.scale(), current.style()
        ));
    }

    public HudSettings withLayout(HudWidgetId id, double normalizedX, double normalizedY, double scale) {
        HudWidgetSettings current = widget(id);
        return withWidget(id, new HudWidgetSettings(
                current.enabled(), normalizedX, normalizedY, scale, current.style()
        ));
    }

    public HudSettings withStyle(HudWidgetId id, HudWidgetStyle style) {
        HudWidgetSettings current = widget(id);
        return withWidget(id, new HudWidgetSettings(
                current.enabled(), current.normalizedX(), current.normalizedY(), current.scale(), style
        ));
    }

    public HudSettings withKeystrokes(KeystrokesSettings settings) {
        return new HudSettings(widgets, ProfileValidation.requireNonNull(settings, "hud.keystrokes"));
    }
}
