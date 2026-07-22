package net.easecation.clientsettings.feature.hud;

import net.easecation.clientsettings.profile.model.HudWidgetId;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HudWidgetRegistryTest {

    @Test
    void registryHasExactlyOneRendererAndPreviewSizeForEveryId() {
        EnumSet<HudWidgetId> ids = HudWidgetRegistry.widgets().stream()
                .map(HudWidget::id)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(HudWidgetId.class)));

        assertEquals(EnumSet.allOf(HudWidgetId.class), ids);
        assertEquals(HudWidgetId.values().length, HudWidgetRegistry.widgets().size());
        for (HudWidgetId id : HudWidgetId.values()) {
            HudWidget widget = HudWidgetRegistry.widget(id);
            assertSame(widget, HudWidgetRegistry.widget(id));
            assertTrue(widget.previewSize().width() > 0);
            assertTrue(widget.previewSize().height() > 0);
        }
    }
}
