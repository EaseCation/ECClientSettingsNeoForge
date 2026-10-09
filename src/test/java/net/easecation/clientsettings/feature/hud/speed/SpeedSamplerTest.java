package net.easecation.clientsettings.feature.hud.speed;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SpeedSamplerTest {
    @Test void samplesActualHorizontalAndSpatialDistanceAndTickRate() {
        var sampler = new SpeedSampler(); var world = new Object(); var entity = new Object();
        sampler.sample(world, entity, 1, 0, 0, 0, 20);
        assertEquals(0, sampler.speed(true));
        sampler.sample(world, entity, 2, 0.3, 0.4, 0, 20);
        assertEquals(6, sampler.speed(true), 1e-10);
        assertEquals(10, sampler.speed(false), 1e-10);
        sampler.sample(world, entity, 3, 0.6, 0.8, 0, 10);
        assertEquals(3, sampler.speed(true), 1e-10);
        sampler.sample(world, entity, 4, 0.6, 0.8, 0, 20);
        assertEquals(0, sampler.speed(false));
    }
    @Test void ignoresDuplicateSamplesAndResetsWorldMountGapsAndTeleport() {
        var sampler = new SpeedSampler(); var world = new Object(); var entity = new Object();
        sampler.sample(world, entity, 1, 0, 0, 0, 20);
        sampler.sample(world, entity, 2, 1, 0, 0, 20);
        sampler.sample(world, entity, 2, 1, 0, 0, 20);
        assertEquals(20, sampler.speed(true));
        sampler.sample(world, entity, 3, 100, 0, 0, 20);
        assertEquals(0, sampler.speed(true));
        sampler.sample(world, entity, 4, 101, 0, 0, 20);
        assertEquals(20, sampler.speed(true));
        sampler.sample(world, new Object(), 5, 0, 0, 0, 20);
        assertEquals(0, sampler.speed(true));
        sampler.sample(new Object(), entity, 6, 10, 0, 0, 20);
        assertEquals(0, sampler.speed(true));
        sampler.sample(world, entity, 10, 0, 0, 0, 20);
        assertEquals(0, sampler.speed(true));
        sampler.sample(world, entity, 11, Double.NaN, 0, 0, 20);
        assertEquals(0, sampler.speed(true));
    }
}
