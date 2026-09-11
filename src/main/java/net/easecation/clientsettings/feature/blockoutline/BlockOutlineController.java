package net.easecation.clientsettings.feature.blockoutline;

import net.easecation.clientsettings.profile.model.BlockOutlineSettings;

import java.util.function.IntPredicate;

public final class BlockOutlineController {

    public boolean tryRender(BlockOutlineSettings settings, IntPredicate renderer) {
        return tryRender(settings, renderer, ignored -> false);
    }

    public boolean tryRender(
            BlockOutlineSettings settings,
            IntPredicate outlineRenderer,
            IntPredicate fillRenderer
    ) {
        boolean rendered = false;
        if (settings.fillEnabled()) {
            rendered |= fillRenderer.test(settings.fillColor().value());
        }
        if (settings.enabled()) {
            rendered |= outlineRenderer.test(settings.color().value());
        }
        return rendered;
    }
}
