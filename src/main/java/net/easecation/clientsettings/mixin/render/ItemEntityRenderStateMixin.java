package net.easecation.clientsettings.mixin.render;

import net.easecation.clientsettings.feature.droppeditems.DroppedItemRenderState;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ItemEntityRenderState.class)
abstract class ItemEntityRenderStateMixin implements DroppedItemRenderState {
    @Unique private boolean ecclientsettings$onGround;

    @Override public boolean ecclientsettings$isOnGround() { return ecclientsettings$onGround; }
    @Override public void ecclientsettings$setOnGround(boolean value) { ecclientsettings$onGround = value; }
}
