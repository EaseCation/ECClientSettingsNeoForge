package net.easecation.clientsettings.feature.hud;

import net.easecation.clientsettings.feature.hud.widget.ArmorHudWidget;
import net.easecation.clientsettings.feature.hud.widget.CpsHudWidget;
import net.easecation.clientsettings.feature.hud.widget.FpsHudWidget;
import net.easecation.clientsettings.feature.hud.widget.KeystrokesHudWidget;
import net.easecation.clientsettings.feature.hud.widget.PingHudWidget;
import net.easecation.clientsettings.feature.hud.widget.PotionHudWidget;
import net.easecation.clientsettings.profile.model.HudWidgetId;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class HudWidgetRegistry {

    private static final Map<HudWidgetId, HudWidget> BY_ID;
    private static final List<HudWidget> WIDGETS;

    static {
        EnumMap<HudWidgetId, HudWidget> widgets = new EnumMap<>(HudWidgetId.class);
        register(widgets, new ArmorHudWidget());
        register(widgets, new PotionHudWidget());
        register(widgets, new PingHudWidget());
        register(widgets, new FpsHudWidget());
        register(widgets, CpsHudWidget.left());
        register(widgets, CpsHudWidget.right());
        register(widgets, new KeystrokesHudWidget());
        if (widgets.size() != HudWidgetId.values().length) {
            throw new IllegalStateException("Every HUD widget ID must have a renderer");
        }
        BY_ID = Map.copyOf(widgets);
        WIDGETS = List.copyOf(widgets.values());
    }

    private HudWidgetRegistry() {
    }

    public static HudWidget widget(HudWidgetId id) {
        HudWidget widget = BY_ID.get(Objects.requireNonNull(id, "id"));
        if (widget == null) {
            throw new IllegalArgumentException("Unknown HUD widget: " + id);
        }
        return widget;
    }

    public static List<HudWidget> widgets() {
        return WIDGETS;
    }

    private static void register(EnumMap<HudWidgetId, HudWidget> widgets, HudWidget widget) {
        HudWidget previous = widgets.put(widget.id(), widget);
        if (previous != null) {
            throw new IllegalStateException("Duplicate HUD widget renderer: " + widget.id());
        }
    }
}
