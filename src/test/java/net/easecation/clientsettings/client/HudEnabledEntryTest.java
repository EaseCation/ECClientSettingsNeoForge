package net.easecation.clientsettings.client;

import net.easecation.clientsettings.profile.model.HudWidgetId;
import net.easecation.clientsettings.profile.model.ProfileDefinition;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HudEnabledEntryTest {
    @Test void constructsBeforeClothBindsScreenAndNeverOverwritesEditorChanges() {
        var profile = ProfileDefinition.defaults(true);
        var draft = new ProfileSettingsDraft(profile.id(), profile.features());
        for (var id : HudWidgetId.values()) {
            var entry = assertDoesNotThrow(() -> new HudEnabledEntry(draft, id));
            assertNull(entry.getConfigScreen());
            assertFalse(entry.getValue());
            draft.setHudEnabled(id, true);
            assertTrue(entry.getValue());
            assertTrue(entry.isEdited());
            entry.save();
            assertTrue(draft.hudSettings().widget(id).enabled());
        }
    }
}
