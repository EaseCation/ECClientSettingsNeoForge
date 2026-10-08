package net.easecation.clientsettings.feature.droppeditems;

import net.easecation.clientsettings.profile.model.DroppedItemSettings;

/** Pure display geometry: no entity angles, age, or position are mutated. */
public record DroppedItemPose(double pitch, double yaw) {
    public static DroppedItemPose resolve(DroppedItemSettings settings, boolean onGround,
            double age, double phase) {
        if (!settings.physics()) {
            return new DroppedItemPose(0, settings.rotation() ? age / 20.0 + phase : 0);
        }
        // Grounded items settle. Airborne tumbling is age-based, independent of frame rate.
        double pitch = onGround || !settings.rotation() ? Math.PI / 2 : Math.PI / 2 + age / 10.0;
        return new DroppedItemPose(pitch, phase);
    }

    public double lift(double minY, double maxY, double minZ, double maxZ, double stackHalfDepth) {
        double cosine = Math.cos(pitch);
        double sine = Math.sin(pitch);
        // R_y(yaw) * R_x(pitch): yaw never changes vertical bounds.
        double lowestY = Math.min(minY * cosine, maxY * cosine)
                + Math.min(-minZ * sine, -maxZ * sine) - Math.abs(sine) * stackHalfDepth;
        return -lowestY + 0.0625;
    }
}
