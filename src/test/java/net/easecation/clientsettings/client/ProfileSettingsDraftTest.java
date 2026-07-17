package net.easecation.clientsettings.client;

import net.easecation.clientsettings.profile.model.ArgbColor;
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

    private ProfileManager manager() throws IOException {
        return ProfileManager.load(new ProfileStore(temporaryDirectory), ProfileDefinition.defaults(true));
    }
}
