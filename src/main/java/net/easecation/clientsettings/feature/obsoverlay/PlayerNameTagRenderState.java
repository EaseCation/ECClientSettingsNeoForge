package net.easecation.clientsettings.feature.obsoverlay;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/** Per-render-state marker used to keep original and anonymized player names distinct. */
public interface PlayerNameTagRenderState {

    void ecclientsettings$setPlayerNameTagState(
            PlayerNameTagMode mode,
            @Nullable Component original,
            @Nullable Component alias
    );

    void ecclientsettings$clearPlayerNameTagState();

    @Nullable
    PlayerNameTagMode ecclientsettings$getPlayerNameTagMode();

    @Nullable
    Component ecclientsettings$getOriginalPlayerNameTag();

    @Nullable
    Component ecclientsettings$getPlayerNameTagAlias();
}
