package net.easecation.clientsettings.feature.droppeditems;

/** Ground contact is captured each extraction because item render states are reused. */
public interface DroppedItemRenderState {
    boolean ecclientsettings$isOnGround();
    void ecclientsettings$setOnGround(boolean value);
}
