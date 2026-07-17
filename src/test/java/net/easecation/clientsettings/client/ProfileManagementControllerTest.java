package net.easecation.clientsettings.client;

import net.easecation.clientsettings.profile.model.ProfileDefinition;
import net.easecation.clientsettings.profile.runtime.ProfileManager;
import net.easecation.clientsettings.profile.store.ProfileStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProfileManagementControllerTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void delegatesValidatedCrudWithoutPreMutatingState() throws IOException {
        ProfileManager manager = ProfileManager.load(
                new ProfileStore(temporaryDirectory),
                ProfileDefinition.defaults(true)
        );
        ProfileManagementController controller = new ProfileManagementController(manager);

        ProfileDefinition created = controller.create("Practice");
        ProfileDefinition copy = controller.duplicate(created.id(), "Practice Copy");
        controller.rename(copy.id(), "Ranked");
        controller.select(copy.id());

        assertEquals(copy.id(), controller.activeProfileId());
        assertEquals(3, controller.profiles().size());
        assertThrows(IllegalArgumentException.class, () -> controller.rename(created.id(), "ranked"));
        assertEquals("Practice", controller.profiles().get(1).name());

        controller.delete(copy.id());
        assertEquals("default", controller.activeProfileId());
        assertEquals(2, controller.profiles().size());
    }
}
