package net.easecation.clientsettings.feature.hud.widget;

import net.easecation.clientsettings.feature.hud.HudRenderContext;
import net.easecation.clientsettings.feature.hud.keystrokes.KeystrokesInputTracker;
import net.easecation.clientsettings.profile.model.HudWidgetId;

public final class CpsHudWidget extends TextHudWidget {

    private static final int MAX_DISPLAYED_CPS = 99;

    private final HudWidgetId id;
    private final String label;
    private final boolean leftButton;

    private CpsHudWidget(HudWidgetId id, String label, boolean leftButton) {
        this.id = id;
        this.label = label;
        this.leftButton = leftButton;
    }

    public static CpsHudWidget left() {
        return new CpsHudWidget(HudWidgetId.LEFT_CPS, "L CPS", true);
    }

    public static CpsHudWidget right() {
        return new CpsHudWidget(HudWidgetId.RIGHT_CPS, "R CPS", false);
    }

    @Override
    public HudWidgetId id() {
        return id;
    }

    @Override
    protected String text(HudRenderContext context) {
        int cps = context.preview()
                ? (leftButton ? 8 : 4)
                : (leftButton ? KeystrokesInputTracker.leftCps() : KeystrokesInputTracker.rightCps());
        return label + " " + (cps > MAX_DISPLAYED_CPS ? "99+" : Math.max(0, cps));
    }
}
