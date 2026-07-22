package net.easecation.clientsettings.client;

import net.easecation.clientsettings.profile.model.ArgbColor;
import net.easecation.clientsettings.profile.model.FullbrightMode;
import net.easecation.clientsettings.profile.model.HudSettings;
import net.easecation.clientsettings.profile.model.HudWidgetId;
import net.easecation.clientsettings.profile.model.TimeChangerMode;
import net.easecation.clientsettings.profile.model.ZoomActivation;
import net.easecation.clientsettings.profile.model.ZoomSettings;
import net.easecation.clientsettings.profile.model.ProfileDefinition;
import net.easecation.clientsettings.profile.runtime.ProfileManager;
import net.easecation.clientsettings.profile.store.ProfileStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProfileSettingsDraftTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void draftDoesNotMutateProfileUntilSave() throws IOException {
        ProfileManager profiles = manager();
        ProfileSettingsDraft draft = ProfileSettingsDraft.active(profiles);
        draft.setForceSprint(false);

        assertTrue(profiles.activeSnapshot().features().forceSprint().enabled());
        assertTrue(draft.edited());

        draft.save(profiles);
        assertFalse(profiles.activeSnapshot().features().forceSprint().enabled());
    }

    @Test
    void cancelEquivalentLeavesPersistedProfileUnchanged() throws IOException {
        ProfileManager profiles = manager();
        ProfileSettingsDraft.active(profiles).setForceSprint(false);

        ProfileManager reloaded = manager();
        assertTrue(reloaded.activeSnapshot().features().forceSprint().enabled());
    }

    @Test
    void staleDraftCannotOverwriteNewlySelectedProfile() throws IOException {
        ProfileManager profiles = manager();
        ProfileSettingsDraft draft = ProfileSettingsDraft.active(profiles);
        draft.setForceSprint(false);
        ProfileDefinition other = profiles.create("Other");
        profiles.switchTo(other.id());

        assertThrows(IOException.class, () -> draft.save(profiles));
        assertTrue(profiles.activeSnapshot().features().forceSprint().enabled());
    }

    @Test
    void failedSaveKeepsDraftAndLeavesRuntimeUnchanged() throws IOException {
        ProfileManager available = manager();
        ProfileSettingsDraft draft = ProfileSettingsDraft.active(available);
        draft.setForceSprint(false);
        ProfileManager unavailable = ProfileManager.unavailable(ProfileDefinition.defaults(true), "read only");

        assertThrows(IOException.class, () -> draft.save(unavailable));
        assertTrue(draft.edited());
        assertTrue(available.activeSnapshot().features().forceSprint().enabled());
    }

    @Test
    void blockOutlineDraftSavesEnablementAndPackedColorTogether() throws IOException {
        ProfileManager profiles = manager();
        ProfileSettingsDraft draft = ProfileSettingsDraft.active(profiles);
        draft.setBlockOutlineEnabled(true);
        draft.setBlockOutlineColor(0x40123456);

        draft.save(profiles);

        assertTrue(profiles.activeSnapshot().features().blockOutline().enabled());
        assertEquals(
                new ArgbColor(0x40123456),
                profiles.activeSnapshot().features().blockOutline().color()
        );
    }

    @Test
    void lowFireDraftSavesEnablementAndOffsetTogether() throws IOException {
        ProfileManager profiles = manager();
        ProfileSettingsDraft draft = ProfileSettingsDraft.active(profiles);
        draft.setLowFireEnabled(true);
        draft.setLowFireOffset(0.5);

        draft.save(profiles);

        assertTrue(profiles.activeSnapshot().features().lowFire().enabled());
        assertEquals(0.5, profiles.activeSnapshot().features().lowFire().verticalOffset());
    }

    @Test
    void fullbrightDraftSavesModeAndStrengthTogether() throws IOException {
        ProfileManager profiles = manager();
        ProfileSettingsDraft draft = ProfileSettingsDraft.active(profiles);
        draft.setFullbrightMode(FullbrightMode.NIGHT_VISION);
        draft.setFullbrightStrength(0.75);

        draft.save(profiles);

        assertEquals(FullbrightMode.NIGHT_VISION, profiles.activeSnapshot().features().fullbright().mode());
        assertEquals(0.75, profiles.activeSnapshot().features().fullbright().strength());
    }

    @Test
    void timeChangerDraftSavesModeAndValidatedCustomTimeTogether() throws IOException {
        ProfileManager profiles = manager();
        ProfileSettingsDraft draft = ProfileSettingsDraft.active(profiles);
        draft.setTimeChangerMode(TimeChangerMode.CUSTOM);
        draft.setTimeChangerCustomTime(23_999);

        draft.save(profiles);

        assertEquals(TimeChangerMode.CUSTOM, profiles.activeSnapshot().features().timeChanger().mode());
        assertEquals(23_999, profiles.activeSnapshot().features().timeChanger().customTime());
    }

    @Test
    void zoomDraftValidatesAllFieldsTogetherAtSaveTime() throws IOException {
        ProfileManager profiles = manager();
        ProfileSettingsDraft draft = ProfileSettingsDraft.active(profiles);
        draft.setZoomEnabled(true);
        draft.setZoomActivation(ZoomActivation.TOGGLE);
        draft.setZoomDivisor(12.0);
        draft.setZoomMaxDivisor(24.0);
        draft.setZoomAnimationSpeed(5.0);
        draft.setZoomScrollAdjustment(true);
        draft.setZoomReduceSensitivity(false);
        draft.setZoomSmoothCamera(true);

        draft.save(profiles);

        assertEquals(ZoomActivation.TOGGLE, profiles.activeSnapshot().features().zoom().activation());
        assertEquals(12.0, profiles.activeSnapshot().features().zoom().divisor());
        assertEquals(24.0, profiles.activeSnapshot().features().zoom().maxDivisor());
        assertEquals(5.0, profiles.activeSnapshot().features().zoom().animationSpeed());
        assertTrue(profiles.activeSnapshot().features().zoom().scrollAdjustment());
        assertFalse(profiles.activeSnapshot().features().zoom().reduceSensitivity());
        assertTrue(profiles.activeSnapshot().features().zoom().smoothCamera());
    }

    @Test
    void invalidZoomPairDoesNotPartiallySaveOtherDraftFields() throws IOException {
        ProfileManager profiles = manager();
        ProfileSettingsDraft draft = ProfileSettingsDraft.active(profiles);
        draft.setForceSprint(false);
        draft.setZoomDivisor(16.0);
        draft.setZoomMaxDivisor(8.0);

        assertThrows(IllegalArgumentException.class, () -> draft.save(profiles));

        assertTrue(profiles.activeSnapshot().features().forceSprint().enabled());
        assertEquals(ZoomSettings.DEFAULT, profiles.activeSnapshot().features().zoom());
    }

    @Test
    void hitColorDraftSavesEnablementAndPackedColorTogether() throws IOException {
        ProfileManager profiles = manager();
        ProfileSettingsDraft draft = ProfileSettingsDraft.active(profiles);
        draft.setHitColorEnabled(true);
        draft.setHitColor(0x60123456);

        draft.save(profiles);

        assertTrue(profiles.activeSnapshot().features().hitColor().enabled());
        assertEquals(new ArgbColor(0x60123456), profiles.activeSnapshot().features().hitColor().color());
    }

    @Test
    void hudChangesRemainInDraftUntilSettingsAreSaved() throws IOException {
        ProfileManager profiles = manager();
        ProfileSettingsDraft draft = ProfileSettingsDraft.active(profiles);

        draft.setHudEnabled(HudWidgetId.FPS, true);
        draft.setHudLayout(HudWidgetId.FPS, 0.4, 0.6, 1.5);

        assertEquals(HudSettings.DEFAULT, profiles.activeSnapshot().features().hud());
        assertTrue(draft.hudSettings().widget(HudWidgetId.FPS).enabled());
        assertEquals(0.4, draft.hudSettings().widget(HudWidgetId.FPS).normalizedX());
        assertEquals(0.6, draft.hudSettings().widget(HudWidgetId.FPS).normalizedY());
        assertEquals(1.5, draft.hudSettings().widget(HudWidgetId.FPS).scale());

        draft.save(profiles);

        assertEquals(draft.hudSettings(), profiles.activeSnapshot().features().hud());
    }

    @Test
    void resettingHudLayoutPreservesDraftEnablement() {
        ProfileSettingsDraft draft = new ProfileSettingsDraft("default", ProfileDefinition.defaults(true).features());
        draft.setHudEnabled(HudWidgetId.ARMOR, true);
        draft.setHudLayout(HudWidgetId.ARMOR, 0.2, 0.8, 2.0);

        draft.resetHudLayout();

        assertTrue(draft.hudSettings().widget(HudWidgetId.ARMOR).enabled());
        assertEquals(
                HudSettings.DEFAULT.widget(HudWidgetId.ARMOR).normalizedX(),
                draft.hudSettings().widget(HudWidgetId.ARMOR).normalizedX()
        );
        assertEquals(
                HudSettings.DEFAULT.widget(HudWidgetId.ARMOR).normalizedY(),
                draft.hudSettings().widget(HudWidgetId.ARMOR).normalizedY()
        );
        assertEquals(
                HudSettings.DEFAULT.widget(HudWidgetId.ARMOR).scale(),
                draft.hudSettings().widget(HudWidgetId.ARMOR).scale()
        );
    }

    private ProfileManager manager() throws IOException {
        return ProfileManager.load(new ProfileStore(temporaryDirectory), ProfileDefinition.defaults(true));
    }
}
