package net.easecation.clientsettings.feature.hud;

import net.easecation.clientsettings.profile.model.HudWidgetId;
import net.easecation.clientsettings.profile.model.HudSettings;

public interface HudWidget {

    HudWidgetId id();

    HudSize previewSize();

    default HudSize previewSize(HudSettings settings) {
        return previewSize();
    }

    HudSize measure(HudRenderContext context);

    default boolean shouldRender(HudRenderContext context) {
        return true;
    }

    void render(HudRenderContext context, HudSize size);
}
