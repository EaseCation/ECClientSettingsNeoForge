package net.easecation.clientsettings.feature.hud.widget;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PotionHudWidgetTest {

    @Test
    void everyVanillaRomanLevelIncludesAnExplicitLabel() {
        assertEquals("enchantment.level.1", PotionHudWidget.effectLevelTranslationKey(0));
        assertEquals("enchantment.level.10", PotionHudWidget.effectLevelTranslationKey(9));
    }

    @Test
    void levelTranslationHelperRejectsNumericFallbackLevels() {
        assertThrows(IllegalArgumentException.class, () -> PotionHudWidget.effectLevelTranslationKey(-1));
        assertThrows(IllegalArgumentException.class, () -> PotionHudWidget.effectLevelTranslationKey(10));
    }
}
