package net.easecation.clientsettings.client;

import net.easecation.clientsettings.profile.model.HudWidgetId;
import net.easecation.clientsettings.profile.model.ProfileDefinition;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HudEditorEntryTest {

    @Test
    void entryHitBoxIncludesTopLeftAndExcludesBottomRight() {
        assertTrue(HudEditorEntry.contains(10, 20, 10, 20, 100, 18));
        assertTrue(HudEditorEntry.contains(109.9, 37.9, 10, 20, 100, 18));
        assertFalse(HudEditorEntry.contains(110, 38, 10, 20, 100, 18));
        assertFalse(HudEditorEntry.contains(9.9, 20, 10, 20, 100, 18));
    }

    @Test
    void layoutOnlyChangesMarkTheClothEntryEdited() {
        ProfileDefinition profile = ProfileDefinition.defaults(true);
        ProfileSettingsDraft draft = new ProfileSettingsDraft(profile.id(), profile.features());

        assertFalse(HudEditorEntry.isDraftEdited(draft));

        draft.setHudLayout(HudWidgetId.FPS, 0.4, 0.6, 1.2);

        assertTrue(HudEditorEntry.isDraftEdited(draft));
    }

    @Test
    void entryOverridesClothsEditedState() throws NoSuchMethodException {
        assertEquals(
                HudEditorEntry.class,
                HudEditorEntry.class.getDeclaredMethod("isEdited").getDeclaringClass()
        );
    }
}
