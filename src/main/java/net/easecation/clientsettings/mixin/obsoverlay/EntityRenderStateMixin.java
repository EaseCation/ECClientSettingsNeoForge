package net.easecation.clientsettings.mixin.obsoverlay;

import net.easecation.clientsettings.feature.obsoverlay.PlayerNameTagMode;
import net.easecation.clientsettings.feature.obsoverlay.PlayerNameTagRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(EntityRenderState.class)
abstract class EntityRenderStateMixin implements PlayerNameTagRenderState {

    @Unique
    private PlayerNameTagMode ecclientsettings$playerNameTagMode;
    @Unique
    private Component ecclientsettings$originalPlayerNameTag;
    @Unique
    private Component ecclientsettings$playerNameTagAlias;

    @Override
    public void ecclientsettings$setPlayerNameTagState(
            PlayerNameTagMode mode,
            @Nullable Component original,
            @Nullable Component alias
    ) {
        ecclientsettings$playerNameTagMode = mode;
        ecclientsettings$originalPlayerNameTag = original;
        ecclientsettings$playerNameTagAlias = alias;
    }

    @Override
    public void ecclientsettings$clearPlayerNameTagState() {
        ecclientsettings$playerNameTagMode = null;
        ecclientsettings$originalPlayerNameTag = null;
        ecclientsettings$playerNameTagAlias = null;
    }

    @Override
    @Nullable
    public PlayerNameTagMode ecclientsettings$getPlayerNameTagMode() {
        return ecclientsettings$playerNameTagMode;
    }

    @Override
    @Nullable
    public Component ecclientsettings$getOriginalPlayerNameTag() {
        return ecclientsettings$originalPlayerNameTag;
    }

    @Override
    @Nullable
    public Component ecclientsettings$getPlayerNameTagAlias() {
        return ecclientsettings$playerNameTagAlias;
    }
}
