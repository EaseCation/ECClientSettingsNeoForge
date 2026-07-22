package net.easecation.clientsettings.profile.model;

public record ProfileFeatures(
        ForceSprintSettings forceSprint,
        BlockOutlineSettings blockOutline,
        LowFireSettings lowFire,
        FullbrightSettings fullbright,
        TimeChangerSettings timeChanger,
        ZoomSettings zoom,
        HitColorSettings hitColor,
        HudSettings hud
) {

    public static final ProfileFeatures DEFAULT = new ProfileFeatures(
            ForceSprintSettings.DEFAULT,
            BlockOutlineSettings.DEFAULT,
            LowFireSettings.DEFAULT,
            FullbrightSettings.DEFAULT,
            TimeChangerSettings.DEFAULT,
            ZoomSettings.DEFAULT,
            HitColorSettings.DEFAULT,
            HudSettings.DEFAULT
    );

    public ProfileFeatures(
            ForceSprintSettings forceSprint,
            BlockOutlineSettings blockOutline,
            LowFireSettings lowFire,
            FullbrightSettings fullbright,
            TimeChangerSettings timeChanger,
            ZoomSettings zoom,
            HitColorSettings hitColor
    ) {
        this(forceSprint, blockOutline, lowFire, fullbright, timeChanger, zoom, hitColor, HudSettings.DEFAULT);
    }

    public ProfileFeatures {
        forceSprint = ProfileValidation.requireNonNull(forceSprint, "features.forceSprint");
        blockOutline = ProfileValidation.requireNonNull(blockOutline, "features.blockOutline");
        lowFire = ProfileValidation.requireNonNull(lowFire, "features.lowFire");
        fullbright = ProfileValidation.requireNonNull(fullbright, "features.fullbright");
        timeChanger = ProfileValidation.requireNonNull(timeChanger, "features.timeChanger");
        zoom = ProfileValidation.requireNonNull(zoom, "features.zoom");
        hitColor = ProfileValidation.requireNonNull(hitColor, "features.hitColor");
        hud = ProfileValidation.requireNonNull(hud, "features.hud");
    }

    public ProfileFeatures withForceSprint(boolean enabled) {
        return new ProfileFeatures(
                new ForceSprintSettings(enabled), blockOutline, lowFire, fullbright, timeChanger, zoom, hitColor, hud
        );
    }

    public ProfileFeatures withBlockOutline(BlockOutlineSettings settings) {
        return new ProfileFeatures(
                forceSprint, settings, lowFire, fullbright, timeChanger, zoom, hitColor, hud
        );
    }

    public ProfileFeatures withLowFire(LowFireSettings settings) {
        return new ProfileFeatures(
                forceSprint, blockOutline, settings, fullbright, timeChanger, zoom, hitColor, hud
        );
    }

    public ProfileFeatures withTimeChanger(TimeChangerSettings settings) {
        return new ProfileFeatures(
                forceSprint, blockOutline, lowFire, fullbright, settings, zoom, hitColor, hud
        );
    }

    public ProfileFeatures withZoom(ZoomSettings settings) {
        return new ProfileFeatures(
                forceSprint, blockOutline, lowFire, fullbright, timeChanger, settings, hitColor, hud
        );
    }

    public ProfileFeatures withFullbright(FullbrightSettings settings) {
        return new ProfileFeatures(
                forceSprint, blockOutline, lowFire, settings, timeChanger, zoom, hitColor, hud
        );
    }

    public ProfileFeatures withHitColor(HitColorSettings settings) {
        return new ProfileFeatures(
                forceSprint, blockOutline, lowFire, fullbright, timeChanger, zoom, settings, hud
        );
    }

    public ProfileFeatures withHud(HudSettings settings) {
        return new ProfileFeatures(
                forceSprint, blockOutline, lowFire, fullbright, timeChanger, zoom, hitColor, settings
        );
    }
}
