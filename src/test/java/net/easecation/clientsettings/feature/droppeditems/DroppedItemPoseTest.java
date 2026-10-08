package net.easecation.clientsettings.feature.droppeditems;

import net.easecation.clientsettings.profile.model.DroppedItemSettings;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DroppedItemPoseTest {
    @Test void physicsSettlesAndRotationOnlyTumblesInAir() {
        var physics = new DroppedItemSettings(true, true, false);
        assertEquals(DroppedItemPose.resolve(physics, true, 1, 2),
                DroppedItemPose.resolve(physics, true, 100, 2));
        assertNotEquals(DroppedItemPose.resolve(physics, false, 1, 2).pitch(),
                DroppedItemPose.resolve(physics, false, 100, 2).pitch());
        var stopped = new DroppedItemSettings(true, false, false);
        assertEquals(DroppedItemPose.resolve(stopped, false, 1, 2),
                DroppedItemPose.resolve(stopped, false, 100, 2));
    }

    @Test void everyRotatedCornerStaysAboveGroundIncludingFlatStacks() {
        for (double age : new double[]{0, 1, 20, 100, 10000}) {
            var pose = DroppedItemPose.resolve(new DroppedItemSettings(true, true, false), false, age, 1);
            double lift = pose.lift(-0.4, 0.3, -0.02, 0.04, 0.12);
            double lowest = Double.POSITIVE_INFINITY;
            for (double y : new double[]{-0.4, 0.3}) {
                for (double z : new double[]{-0.14, 0.16}) {
                    double transformed = y * Math.cos(pose.pitch()) - z * Math.sin(pose.pitch()) + lift;
                    lowest = Math.min(lowest, transformed);
                    assertTrue(transformed >= 0.0625 - 1e-10);
                }
            }
            assertEquals(0.0625, lowest, 1e-10);
        }
    }

    @Test void vanillaPoseAndIndependentFloatingSwitch() {
        assertEquals(0, DroppedItemPose.resolve(DroppedItemSettings.DEFAULT, true, 30, 2).pitch());
        assertEquals(3.5, DroppedItemPose.resolve(DroppedItemSettings.DEFAULT, true, 30, 2).yaw());
        for (boolean physics : new boolean[]{false, true}) {
            for (boolean rotation : new boolean[]{false, true}) {
                assertEquals(DroppedItemPose.resolve(new DroppedItemSettings(physics, rotation, true), false, 30, 2),
                        DroppedItemPose.resolve(new DroppedItemSettings(physics, rotation, false), false, 30, 2));
            }
        }
    }
}
