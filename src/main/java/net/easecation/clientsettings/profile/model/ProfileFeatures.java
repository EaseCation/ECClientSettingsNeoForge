package net.easecation.clientsettings.profile.model;

public record ProfileFeatures(
        ForceSprintSettings forceSprint,
        BlockOutlineSettings blockOutline,
        LowFireSettings lowFire,
        FullbrightSettings fullbright,
        TimeChangerSettings timeChanger,
        ZoomSettings zoom,
        HitColorSettings hitColor
) {

    public static final ProfileFeatures DEFAULT = new ProfileFeatures(
            ForceSprintSettings.DEFAULT,
            BlockOutlineSettings.DEFAULT,
            LowFireSettings.DEFAULT,
            FullbrightSettings.DEFAULT,
            TimeChangerSettings.DEFAULT,
            ZoomSettings.DEFAULT,
            HitColorSettings.DEFAULT
    );

    public ProfileFeatures {
        forceSprint = ProfileValidation.requireNonNull(forceSprint, "features.forceSprint");
        blockOutline = ProfileValidation.requireNonNull(blockOutline, "features.blockOutline");
        lowFire = ProfileValidation.requireNonNull(lowFire, "features.lowFire");
        fullbright = ProfileValidation.requireNonNull(fullbright, "features.fullbright");
        timeChanger = ProfileValidation.requireNonNull(timeChanger, "features.timeChanger");
        zoom = ProfileValidation.requireNonNull(zoom, "features.zoom");
        hitColor = ProfileValidation.requireNonNull(hitColor, "features.hitColor");
    }

    public ProfileFeatures withForceSprint(boolean enabled) {
        return new ProfileFeatures(
                new ForceSprintSettings(enabled), blockOutline, lowFire, fullbright, timeChanger, zoom, hitColor
        );
    }

    public ProfileFeatures withBlockOutline(BlockOutlineSettings settings) {
        return new ProfileFeatures(
                forceSprint, settings, lowFire, fullbright, timeChanger, zoom, hitColor
        );
    }

    public ProfileFeatures withLowFire(LowFireSettings settings) {
        return new ProfileFeatures(
                forceSprint, blockOutline, settings, fullbright, timeChanger, zoom, hitColor
        );
    }

    public ProfileFeatures withTimeChanger(TimeChangerSettings settings) {
        return new ProfileFeatures(
                forceSprint, blockOutline, lowFire, fullbright, settings, zoom, hitColor
        );
    }

    public ProfileFeatures withFullbright(FullbrightSettings settings) {
        return new ProfileFeatures(
                forceSprint, blockOutline, lowFire, settings, timeChanger, zoom, hitColor
        );
    }
}
