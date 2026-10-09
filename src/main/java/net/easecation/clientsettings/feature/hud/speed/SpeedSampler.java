package net.easecation.clientsettings.feature.hud.speed;

/** Position deltas represent actual movement, unlike the post-friction velocity field. */
public final class SpeedSampler {
    private Object world;
    private Object target;
    private int tick;
    private double x, y, z;
    private double horizontal, spatial;
    public void sample(Object world, Object target, int tick, double x, double y, double z, double ticksPerSecond) {
        if (world == null || target == null || !Double.isFinite(x) || !Double.isFinite(y)
                || !Double.isFinite(z) || !Double.isFinite(ticksPerSecond) || ticksPerSecond <= 0) {
            clear(); return;
        }
        if (world == this.world && target == this.target && tick == this.tick) return;
        double dx = x - this.x, dy = y - this.y, dz = z - this.z;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (world != this.world || target != this.target || tick != this.tick + 1 || distance > 16) {
            horizontal = spatial = 0;
        } else {
            horizontal = Math.hypot(dx, dz) * ticksPerSecond;
            spatial = distance * ticksPerSecond;
        }
        this.world = world; this.target = target; this.tick = tick;
        this.x = x; this.y = y; this.z = z;
    }
    public double speed(boolean horizontalOnly) { return horizontalOnly ? horizontal : spatial; }
    public void clear() { world = target = null; tick = 0; x = y = z = horizontal = spatial = 0; }
}
