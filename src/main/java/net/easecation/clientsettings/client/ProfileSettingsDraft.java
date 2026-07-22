package net.easecation.clientsettings.client;

import net.easecation.clientsettings.profile.model.ProfileFeatures;
import net.easecation.clientsettings.profile.model.ArgbColor;
import net.easecation.clientsettings.profile.model.BlockOutlineSettings;
import net.easecation.clientsettings.profile.model.LowFireSettings;
import net.easecation.clientsettings.profile.model.FullbrightMode;
import net.easecation.clientsettings.profile.model.FullbrightSettings;
import net.easecation.clientsettings.profile.model.HitColorSettings;
import net.easecation.clientsettings.profile.model.HudSettings;
import net.easecation.clientsettings.profile.model.HudWidgetId;
import net.easecation.clientsettings.profile.model.HudWidgetSettings;
import net.easecation.clientsettings.profile.model.HudWidgetStyle;
import net.easecation.clientsettings.profile.model.KeystrokesSettings;
import net.easecation.clientsettings.profile.model.TimeChangerMode;
import net.easecation.clientsettings.profile.model.TimeChangerSettings;
import net.easecation.clientsettings.profile.model.ZoomActivation;
import net.easecation.clientsettings.profile.model.ZoomSettings;
import net.easecation.clientsettings.profile.runtime.ProfileManager;

import java.io.IOException;
import java.util.Objects;

public final class ProfileSettingsDraft {

    private final String profileId;
    private final ProfileFeatures initialFeatures;
    private ProfileFeatures pendingFeatures;
    private boolean zoomEnabled;
    private ZoomActivation zoomActivation;
    private double zoomDivisor;
    private double zoomMaxDivisor;
    private double zoomAnimationSpeed;
    private boolean zoomScrollAdjustment;
    private boolean zoomReduceSensitivity;
    private boolean zoomSmoothCamera;

    public ProfileSettingsDraft(String profileId, ProfileFeatures features) {
        this.profileId = profileId;
        this.initialFeatures = Objects.requireNonNull(features, "features");
        this.pendingFeatures = features;
        ZoomSettings zoom = features.zoom();
        this.zoomEnabled = zoom.enabled();
        this.zoomActivation = zoom.activation();
        this.zoomDivisor = zoom.divisor();
        this.zoomMaxDivisor = zoom.maxDivisor();
        this.zoomAnimationSpeed = zoom.animationSpeed();
        this.zoomScrollAdjustment = zoom.scrollAdjustment();
        this.zoomReduceSensitivity = zoom.reduceSensitivity();
        this.zoomSmoothCamera = zoom.smoothCamera();
    }

    public static ProfileSettingsDraft active(ProfileManager profiles) {
        return new ProfileSettingsDraft(
                profiles.activeSnapshot().id(),
                profiles.activeSnapshot().features()
        );
    }

    public String profileId() {
        return profileId;
    }

    public ProfileFeatures features() {
        return pendingFeatures;
    }

    public void setForceSprint(boolean enabled) {
        pendingFeatures = pendingFeatures.withForceSprint(enabled);
    }

    public void setBlockOutlineEnabled(boolean enabled) {
        BlockOutlineSettings current = pendingFeatures.blockOutline();
        pendingFeatures = pendingFeatures.withBlockOutline(new BlockOutlineSettings(enabled, current.color()));
    }

    public void setBlockOutlineColor(int color) {
        BlockOutlineSettings current = pendingFeatures.blockOutline();
        pendingFeatures = pendingFeatures.withBlockOutline(new BlockOutlineSettings(
                current.enabled(),
                new ArgbColor(color)
        ));
    }

    public void setLowFireEnabled(boolean enabled) {
        LowFireSettings current = pendingFeatures.lowFire();
        pendingFeatures = pendingFeatures.withLowFire(new LowFireSettings(enabled, current.verticalOffset()));
    }

    public void setLowFireOffset(double offset) {
        LowFireSettings current = pendingFeatures.lowFire();
        pendingFeatures = pendingFeatures.withLowFire(new LowFireSettings(current.enabled(), offset));
    }

    public void setFullbrightMode(FullbrightMode mode) {
        FullbrightSettings current = pendingFeatures.fullbright();
        pendingFeatures = pendingFeatures.withFullbright(new FullbrightSettings(mode, current.strength()));
    }

    public void setFullbrightStrength(double strength) {
        FullbrightSettings current = pendingFeatures.fullbright();
        pendingFeatures = pendingFeatures.withFullbright(new FullbrightSettings(current.mode(), strength));
    }

    public void setTimeChangerMode(TimeChangerMode mode) {
        TimeChangerSettings current = pendingFeatures.timeChanger();
        pendingFeatures = pendingFeatures.withTimeChanger(new TimeChangerSettings(mode, current.customTime()));
    }

    public void setTimeChangerCustomTime(int customTime) {
        TimeChangerSettings current = pendingFeatures.timeChanger();
        pendingFeatures = pendingFeatures.withTimeChanger(new TimeChangerSettings(current.mode(), customTime));
    }

    public void setHitColorEnabled(boolean enabled) {
        HitColorSettings current = pendingFeatures.hitColor();
        pendingFeatures = pendingFeatures.withHitColor(new HitColorSettings(enabled, current.color()));
    }

    public void setHitColor(int color) {
        HitColorSettings current = pendingFeatures.hitColor();
        pendingFeatures = pendingFeatures.withHitColor(new HitColorSettings(
                current.enabled(),
                new ArgbColor(color)
        ));
    }

    public HudSettings hudSettings() {
        return pendingFeatures.hud();
    }

    public void setHudEnabled(HudWidgetId id, boolean enabled) {
        setHudSettings(hudSettings().withEnabled(id, enabled));
    }

    public void setHudWidget(HudWidgetId id, HudWidgetSettings settings) {
        setHudSettings(hudSettings().withWidget(id, settings));
    }

    public void setHudLayout(HudWidgetId id, double normalizedX, double normalizedY, double scale) {
        setHudSettings(hudSettings().withLayout(id, normalizedX, normalizedY, scale));
    }

    public void setHudStyle(HudWidgetId id, HudWidgetStyle style) {
        setHudSettings(hudSettings().withStyle(id, style));
    }

    public void setKeystrokesSettings(KeystrokesSettings settings) {
        setHudSettings(hudSettings().withKeystrokes(settings));
    }

    public void restoreHudSettings(HudSettings settings) {
        setHudSettings(settings);
    }

    public void resetHudLayout(HudWidgetId id) {
        HudWidgetSettings current = hudSettings().widget(id);
        HudWidgetSettings defaults = HudSettings.DEFAULT.widget(id);
        setHudWidget(id, new HudWidgetSettings(
                current.enabled(),
                defaults.normalizedX(),
                defaults.normalizedY(),
                defaults.scale(),
                current.style()
        ));
    }

    public void resetHudLayout() {
        HudSettings reset = hudSettings();
        for (HudWidgetId id : HudWidgetId.values()) {
            HudWidgetSettings current = reset.widget(id);
            HudWidgetSettings defaults = HudSettings.DEFAULT.widget(id);
            reset = reset.withWidget(id, new HudWidgetSettings(
                    current.enabled(),
                    defaults.normalizedX(),
                    defaults.normalizedY(),
                    defaults.scale(),
                    current.style()
            ));
        }
        setHudSettings(reset);
    }

    public void resetHudStyle(HudWidgetId id) {
        setHudStyle(id, HudWidgetStyle.defaultsFor(id));
    }

    private void setHudSettings(HudSettings settings) {
        pendingFeatures = pendingFeatures.withHud(Objects.requireNonNull(settings, "HUD settings"));
    }

    public boolean zoomEnabled() {
        return zoomEnabled;
    }

    public ZoomActivation zoomActivation() {
        return zoomActivation;
    }

    public double zoomDivisor() {
        return zoomDivisor;
    }

    public double zoomMaxDivisor() {
        return zoomMaxDivisor;
    }

    public double zoomAnimationSpeed() {
        return zoomAnimationSpeed;
    }

    public boolean zoomScrollAdjustment() {
        return zoomScrollAdjustment;
    }

    public boolean zoomReduceSensitivity() {
        return zoomReduceSensitivity;
    }

    public boolean zoomSmoothCamera() {
        return zoomSmoothCamera;
    }

    public void setZoomEnabled(boolean value) {
        zoomEnabled = value;
    }

    public void setZoomActivation(ZoomActivation value) {
        zoomActivation = Objects.requireNonNull(value, "zoom activation");
    }

    public void setZoomDivisor(double value) {
        zoomDivisor = value;
    }

    public void setZoomMaxDivisor(double value) {
        zoomMaxDivisor = value;
    }

    public void setZoomAnimationSpeed(double value) {
        zoomAnimationSpeed = value;
    }

    public void setZoomScrollAdjustment(boolean value) {
        zoomScrollAdjustment = value;
    }

    public void setZoomReduceSensitivity(boolean value) {
        zoomReduceSensitivity = value;
    }

    public void setZoomSmoothCamera(boolean value) {
        zoomSmoothCamera = value;
    }

    public boolean edited() {
        return !materializedFeatures().equals(initialFeatures);
    }

    public void save(ProfileManager profiles) throws IOException {
        if (!profiles.activeSnapshot().id().equals(profileId)) {
            throw new IOException("Active Profile changed while its settings screen was open");
        }
        ProfileFeatures materialized = materializedFeatures();
        if (!materialized.equals(initialFeatures)) {
            profiles.updateActiveFeatures(ignored -> materialized);
        }
    }

    private ProfileFeatures materializedFeatures() {
        return pendingFeatures.withZoom(new ZoomSettings(
                zoomEnabled,
                zoomActivation,
                zoomDivisor,
                zoomMaxDivisor,
                zoomAnimationSpeed,
                zoomScrollAdjustment,
                zoomReduceSensitivity,
                zoomSmoothCamera
        ));
    }
}
