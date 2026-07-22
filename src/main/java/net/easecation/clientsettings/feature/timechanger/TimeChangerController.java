package net.easecation.clientsettings.feature.timechanger;

import net.easecation.clientsettings.profile.model.TimeChangerMode;
import net.easecation.clientsettings.profile.model.TimeChangerSettings;

import java.util.Optional;

public final class TimeChangerController {

    private Object worldIdentity;
    private Baseline baseline;
    private long clientTick;
    private TimeChangerSettings currentSettings = TimeChangerSettings.DEFAULT;

    public void enterWorld(Object identity) {
        if (worldIdentity != identity) {
            worldIdentity = identity;
            baseline = null;
            currentSettings = TimeChangerSettings.DEFAULT;
        }
    }

    public void tick() {
        clientTick++;
    }

    public ClientTimeValues onServerTime(
            Object identity,
            long gameTime,
            long dayTime,
            boolean tickDayTime,
            TimeChangerSettings settings
    ) {
        enterWorld(identity);
        baseline = new Baseline(gameTime, dayTime, tickDayTime, clientTick, false);
        currentSettings = settings;
        if (settings.mode() == TimeChangerMode.FOLLOW_SERVER) {
            return new ClientTimeValues(gameTime, dayTime, tickDayTime);
        }
        return new ClientTimeValues(gameTime, fixedDayTime(settings), false);
    }

    public Optional<ClientTimeValues> applySettings(
            Object identity,
            long currentGameTime,
            long currentDayTime,
            TimeChangerSettings settings
    ) {
        enterWorld(identity);
        if (baseline == null && settings.mode() != TimeChangerMode.FOLLOW_SERVER) {
            baseline = new Baseline(currentGameTime, currentDayTime, true, clientTick, true);
        }
        currentSettings = settings;
        if (settings.mode() == TimeChangerMode.FOLLOW_SERVER) {
            return baseline == null ? Optional.empty() : Optional.of(expectedServerTime());
        }
        return Optional.of(new ClientTimeValues(currentGameTime, fixedDayTime(settings), false));
    }

    public Optional<ClientTimeValues> restoreBeforeProfileSwitch(Object identity) {
        enterWorld(identity);
        currentSettings = TimeChangerSettings.DEFAULT;
        return baseline == null ? Optional.empty() : Optional.of(expectedServerTime());
    }

    public void clear() {
        worldIdentity = null;
        baseline = null;
        currentSettings = TimeChangerSettings.DEFAULT;
        clientTick = 0L;
    }

    public boolean hasServerBaseline() {
        return baseline != null && !baseline.approximate();
    }

    public boolean hasApproximateBaseline() {
        return baseline != null && baseline.approximate();
    }

    public TimeChangerSettings currentSettings() {
        return currentSettings;
    }

    private ClientTimeValues expectedServerTime() {
        long elapsed = clientTick - baseline.receiptTick();
        return new ClientTimeValues(
                baseline.gameTime() + elapsed,
                baseline.dayTime() + (baseline.tickDayTime() ? elapsed : 0L),
                baseline.tickDayTime()
        );
    }

    public static int fixedDayTime(TimeChangerSettings settings) {
        return switch (settings.mode()) {
            case DAY -> 1_000;
            case NOON -> 6_000;
            case SUNSET -> 12_000;
            case MIDNIGHT -> 18_000;
            case CUSTOM -> settings.customTime();
            case FOLLOW_SERVER -> throw new IllegalArgumentException("FOLLOW_SERVER has no fixed day time");
        };
    }

    private record Baseline(
            long gameTime,
            long dayTime,
            boolean tickDayTime,
            long receiptTick,
            boolean approximate
    ) {
    }
}
