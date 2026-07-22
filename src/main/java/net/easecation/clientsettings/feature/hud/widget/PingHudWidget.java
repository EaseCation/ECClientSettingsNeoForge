package net.easecation.clientsettings.feature.hud.widget;

import net.easecation.clientsettings.feature.hud.HudRenderContext;
import net.easecation.clientsettings.profile.model.HudWidgetId;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;

public final class PingHudWidget extends TextHudWidget {

    private static final int UNKNOWN = -1;
    private static final int MAX_DISPLAYED_LATENCY = 9_999;

    @Override
    public HudWidgetId id() {
        return HudWidgetId.PING;
    }

    @Override
    protected String text(HudRenderContext context) {
        int latency = context.preview() ? 35 : latency(context);
        return formatLatency(latency);
    }

    private static int latency(HudRenderContext context) {
        if (context.player() == null || context.minecraft().hasSingleplayerServer()) {
            return UNKNOWN;
        }
        ClientPacketListener connection = context.minecraft().getConnection();
        if (connection == null) {
            return UNKNOWN;
        }
        PlayerInfo info = connection.getPlayerInfo(context.player().getUUID());
        if (info == null || info.getLatency() <= 0) {
            return UNKNOWN;
        }
        return info.getLatency();
    }

    static String formatLatency(int latency) {
        if (latency <= 0) {
            return "-- ms";
        }
        return latency > MAX_DISPLAYED_LATENCY ? "9999+ ms" : latency + " ms";
    }
}
