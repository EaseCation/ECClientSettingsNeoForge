package net.easecation.clientsettings.profile.model;

public record PotionHudSettings(
        boolean hideVanilla,
        boolean vanillaLevel,
        boolean vanillaTime,
        ArgbColor vanillaLevelColor,
        ArgbColor vanillaTimeColor,
        boolean showName,
        boolean showLevel,
        boolean showTime,
        boolean effectNameColor,
        ArgbColor levelColor,
        ArgbColor timeColor,
        boolean warningEnabled,
        int warningSeconds
) {
    public static final PotionHudSettings DEFAULT = new PotionHudSettings(
            false, false, false, ArgbColor.parse("#FFFFFFFF"), ArgbColor.parse("#FFFFFFFF"), true, true, true, false, ArgbColor.parse("#FFFFFFFF"), ArgbColor.parse("#FFFFFFFF"), true, 20
    );
    public PotionHudSettings {
        vanillaLevelColor = ProfileValidation.requireNonNull(vanillaLevelColor, "hud.potions.vanillaLevelColor");
        vanillaTimeColor = ProfileValidation.requireNonNull(vanillaTimeColor, "hud.potions.vanillaTimeColor");
        levelColor = ProfileValidation.requireNonNull(levelColor, "hud.potions.levelColor");
        timeColor = ProfileValidation.requireNonNull(timeColor, "hud.potions.timeColor");
        if (warningSeconds < 1 || warningSeconds > 300) throw new IllegalArgumentException("Warning seconds must be in 1..300");
    }
    public PotionHudSettings withHideVanilla(boolean value) {
        return new PotionHudSettings(value, vanillaLevel, vanillaTime, vanillaLevelColor, vanillaTimeColor, showName, showLevel, showTime, effectNameColor, levelColor, timeColor, warningEnabled, warningSeconds);
    }
    public PotionHudSettings withVanillaLevel(boolean value) {
        return new PotionHudSettings(hideVanilla, value, vanillaTime, vanillaLevelColor, vanillaTimeColor, showName, showLevel, showTime, effectNameColor, levelColor, timeColor, warningEnabled, warningSeconds);
    }
    public PotionHudSettings withVanillaTime(boolean value) {
        return new PotionHudSettings(hideVanilla, vanillaLevel, value, vanillaLevelColor, vanillaTimeColor, showName, showLevel, showTime, effectNameColor, levelColor, timeColor, warningEnabled, warningSeconds);
    }
    public PotionHudSettings withVanillaLevelColor(ArgbColor value) {
        return new PotionHudSettings(hideVanilla, vanillaLevel, vanillaTime, value, vanillaTimeColor, showName, showLevel, showTime, effectNameColor, levelColor, timeColor, warningEnabled, warningSeconds);
    }
    public PotionHudSettings withVanillaTimeColor(ArgbColor value) {
        return new PotionHudSettings(hideVanilla, vanillaLevel, vanillaTime, vanillaLevelColor, value, showName, showLevel, showTime, effectNameColor, levelColor, timeColor, warningEnabled, warningSeconds);
    }
    public PotionHudSettings withShowName(boolean value) {
        return new PotionHudSettings(hideVanilla, vanillaLevel, vanillaTime, vanillaLevelColor, vanillaTimeColor, value, showLevel, showTime, effectNameColor, levelColor, timeColor, warningEnabled, warningSeconds);
    }
    public PotionHudSettings withShowLevel(boolean value) {
        return new PotionHudSettings(hideVanilla, vanillaLevel, vanillaTime, vanillaLevelColor, vanillaTimeColor, showName, value, showTime, effectNameColor, levelColor, timeColor, warningEnabled, warningSeconds);
    }
    public PotionHudSettings withShowTime(boolean value) {
        return new PotionHudSettings(hideVanilla, vanillaLevel, vanillaTime, vanillaLevelColor, vanillaTimeColor, showName, showLevel, value, effectNameColor, levelColor, timeColor, warningEnabled, warningSeconds);
    }
    public PotionHudSettings withEffectNameColor(boolean value) {
        return new PotionHudSettings(hideVanilla, vanillaLevel, vanillaTime, vanillaLevelColor, vanillaTimeColor, showName, showLevel, showTime, value, levelColor, timeColor, warningEnabled, warningSeconds);
    }
    public PotionHudSettings withLevelColor(ArgbColor value) {
        return new PotionHudSettings(hideVanilla, vanillaLevel, vanillaTime, vanillaLevelColor, vanillaTimeColor, showName, showLevel, showTime, effectNameColor, value, timeColor, warningEnabled, warningSeconds);
    }
    public PotionHudSettings withTimeColor(ArgbColor value) {
        return new PotionHudSettings(hideVanilla, vanillaLevel, vanillaTime, vanillaLevelColor, vanillaTimeColor, showName, showLevel, showTime, effectNameColor, levelColor, value, warningEnabled, warningSeconds);
    }
    public PotionHudSettings withWarningEnabled(boolean value) {
        return new PotionHudSettings(hideVanilla, vanillaLevel, vanillaTime, vanillaLevelColor, vanillaTimeColor, showName, showLevel, showTime, effectNameColor, levelColor, timeColor, value, warningSeconds);
    }
    public PotionHudSettings withWarningSeconds(int value) {
        return new PotionHudSettings(hideVanilla, vanillaLevel, vanillaTime, vanillaLevelColor, vanillaTimeColor, showName, showLevel, showTime, effectNameColor, levelColor, timeColor, warningEnabled, value);
    }
}
