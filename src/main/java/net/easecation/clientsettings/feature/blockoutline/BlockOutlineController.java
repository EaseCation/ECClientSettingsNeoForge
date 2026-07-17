package net.easecation.clientsettings.feature.blockoutline;

import net.easecation.clientsettings.profile.model.BlockOutlineSettings;

import java.util.function.IntPredicate;

public final class BlockOutlineController {

    public boolean tryRender(BlockOutlineSettings settings, IntPredicate renderer) {
        return settings.enabled() && renderer.test(settings.color().value());
    }
}
