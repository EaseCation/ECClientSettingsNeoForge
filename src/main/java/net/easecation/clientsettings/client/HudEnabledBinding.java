package net.easecation.clientsettings.client;

import net.easecation.clientsettings.profile.model.HudSettings;
import net.easecation.clientsettings.profile.model.HudWidgetId;

/** Shared draft is authoritative across the settings page and its nested editor. */
final class HudEnabledBinding {
    private final ProfileSettingsDraft draft;
    private final HudWidgetId id;
    private final boolean initial;

    HudEnabledBinding(ProfileSettingsDraft draft, HudWidgetId id) {
        this.draft = draft;
        this.id = id;
        this.initial = value();
    }

    boolean value() { return draft.hudSettings().widget(id).enabled(); }
    boolean edited() { return value() != initial; }
    boolean defaultValue() { return HudSettings.DEFAULT.widget(id).enabled(); }
    void toggle() { draft.setHudEnabled(id, !value()); }
    void reset() { draft.setHudEnabled(id, defaultValue()); }
}
