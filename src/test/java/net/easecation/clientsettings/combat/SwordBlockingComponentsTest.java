package net.easecation.clientsettings.combat;

import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.component.Consumable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SwordBlockingComponentsTest {

    private static final Consumable VIA_BEDROCK_COMPONENT = Consumable.builder()
            .consumeSeconds(1_000_000F)
            .animation(ItemUseAnimation.BLOCK)
            .hasConsumeParticles(false)
            .build();

    @Test
    void disabledClientSettingRemovesViaBedrockComponent() {
        assertNull(SwordBlockingComponents.overrideSwordConsumable(VIA_BEDROCK_COMPONENT, false));
    }

    @Test
    void enabledClientSettingProvidesComponentWithoutServerInjection() {
        Consumable result = SwordBlockingComponents.overrideSwordConsumable(null, true);

        assertTrue(SwordBlockingComponents.isViaBedrockBlockingComponent(result));
    }

    @Test
    void unrelatedConsumableIsNeverOverridden() {
        Consumable food = Consumable.builder().build();

        assertSame(food, SwordBlockingComponents.overrideSwordConsumable(food, false));
        assertSame(food, SwordBlockingComponents.overrideSwordConsumable(food, true));
    }

    @Test
    void offhandBlockWinsOnlyForBlockingSwordAgainstBlockTarget() {
        assertTrue(SwordBlockingComponents.shouldPrioritizeOffhandBlock(true, true, true));
        assertFalse(SwordBlockingComponents.shouldPrioritizeOffhandBlock(false, true, true));
        assertFalse(SwordBlockingComponents.shouldPrioritizeOffhandBlock(true, false, true));
        assertFalse(SwordBlockingComponents.shouldPrioritizeOffhandBlock(true, true, false));
    }
}
