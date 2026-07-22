package net.easecation.clientsettings.profile.model;

public record TimeChangerSettings(TimeChangerMode mode, int customTime) {

    public static final TimeChangerSettings DEFAULT = new TimeChangerSettings(TimeChangerMode.FOLLOW_SERVER, 6000);

    public TimeChangerSettings {
        mode = ProfileValidation.requireNonNull(mode, "timeChanger.mode");
        if (customTime < 0 || customTime > 23_999) {
            throw new IllegalArgumentException("timeChanger.customTime must be in 0..23999");
        }
    }
}
