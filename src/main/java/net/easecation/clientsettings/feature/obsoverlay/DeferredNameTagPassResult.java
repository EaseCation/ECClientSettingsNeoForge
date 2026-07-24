package net.easecation.clientsettings.feature.obsoverlay;

/** Outcome of routing a player name tag through an OBS overlay render target. */
public enum DeferredNameTagPassResult {
    BEGIN_FAILED,
    FLUSH_FAILED,
    COMPLETE
}
