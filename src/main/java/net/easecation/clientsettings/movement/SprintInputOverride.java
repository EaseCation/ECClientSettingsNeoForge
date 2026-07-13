package net.easecation.clientsettings.movement;

import net.minecraft.world.entity.player.Input;

/** Adds the vanilla sprint input without changing any other movement key state. */
public final class SprintInputOverride {

    private SprintInputOverride() {
    }

    public static Input apply(Input input, boolean enabled) {
        if (!enabled || !input.forward() || input.sprint()) {
            return input;
        }

        return new Input(
                input.forward(),
                input.backward(),
                input.left(),
                input.right(),
                input.jump(),
                input.shift(),
                true
        );
    }
}
