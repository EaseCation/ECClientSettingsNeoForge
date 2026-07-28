package net.easecation.clientsettings.feature.hud.widget;

import net.easecation.clientsettings.feature.hud.HudRenderContext;
import net.easecation.clientsettings.feature.hud.HudSize;
import net.easecation.clientsettings.profile.model.HudWidgetId;

public final class CombinedCpsHudWidget extends TextHudWidget {

    private static final HudSize SIZE = new HudSize(72, 10);

    @Override
    public HudWidgetId id() {
        return HudWidgetId.COMBINED_CPS;
    }

    @Override
    protected String text(HudRenderContext context) {
        return format(
                CpsHudWidget.currentCps(context, true),
                CpsHudWidget.currentCps(context, false)
        );
    }

    @Override
    protected HudSize size() {
        return SIZE;
    }

    static String format(int leftCps, int rightCps) {
        return "CPS " + CpsHudWidget.formatCps(leftCps)
                + " | " + CpsHudWidget.formatCps(rightCps);
    }
}
