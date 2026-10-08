package net.easecation.clientsettings.feature.helditeminfo;

/** Game-tick countdown, so pausing never consumes the display lifetime. */
public final class HeldItemInfoTimer {
    private int elapsed;
    private int lifetime;
    public void restart(double baseSeconds, double extraLineSeconds, int lines) {
        elapsed = 0;
        lifetime = lines == 0 ? 0 : (int) Math.round((baseSeconds + extraLineSeconds * Math.max(0, lines - 1)) * 20);
    }
    public void tick() { if (elapsed < lifetime) elapsed++; }
    public void clear() { elapsed = 0; lifetime = 0; }
    public float opacity(float partialTick) {
        return Math.clamp((lifetime - elapsed - Math.clamp(partialTick, 0, 1)) / 10.0F, 0.0F, 1.0F);
    }
}
