package net.easecation.clientsettings.movement;

import net.minecraft.world.entity.player.Input;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SprintInputOverrideTest {

    @Test
    void disabledReturnsOriginalInput() {
        Input input = new Input(true, false, true, false, true, false, false);

        assertSame(input, SprintInputOverride.apply(input, false));
    }

    @Test
    void enabledWithoutForwardInputReturnsOriginalInput() {
        Input input = new Input(false, false, true, false, false, false, false);

        assertSame(input, SprintInputOverride.apply(input, true));
    }

    @Test
    void enabledForwardInputSetsSprintAndPreservesOtherFields() {
        Input input = new Input(true, true, false, true, true, true, false);

        Input result = SprintInputOverride.apply(input, true);

        assertTrue(result.sprint());
        assertEquals(input.forward(), result.forward());
        assertEquals(input.backward(), result.backward());
        assertEquals(input.left(), result.left());
        assertEquals(input.right(), result.right());
        assertEquals(input.jump(), result.jump());
        assertEquals(input.shift(), result.shift());
    }

    @Test
    void alreadySprintingReturnsOriginalInput() {
        Input input = new Input(true, false, false, false, false, false, true);

        assertSame(input, SprintInputOverride.apply(input, true));
    }
}
