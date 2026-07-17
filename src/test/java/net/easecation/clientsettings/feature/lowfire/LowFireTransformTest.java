package net.easecation.clientsettings.feature.lowfire;

import net.easecation.clientsettings.profile.model.LowFireSettings;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LowFireTransformTest {

    @Test
    void disabledPathCallsVanillaWithoutTouchingStack() {
        List<String> calls = new ArrayList<>();

        LowFireTransform.render(new LowFireSettings(false, 0.2), stack(calls), () -> calls.add("render"));

        assertEquals(List.of("render"), calls);
    }

    @Test
    void enabledPathPushesTranslatesRendersAndPops() {
        List<String> calls = new ArrayList<>();

        LowFireTransform.render(new LowFireSettings(true, 0.2), stack(calls), () -> calls.add("render"));

        assertEquals(List.of("push", "translate:-0.2", "render", "pop"), calls);
    }

    @Test
    void zeroOffsetPreservesGeometryAndStillBalancesStack() {
        List<String> calls = new ArrayList<>();

        LowFireTransform.render(new LowFireSettings(true, 0.0), stack(calls), () -> calls.add("render"));

        assertEquals(List.of("push", "translate:-0.0", "render", "pop"), calls);
    }

    @Test
    void delegatedFailureAlwaysPopsBeforePropagating() {
        List<String> calls = new ArrayList<>();
        IllegalStateException failure = new IllegalStateException("simulated render failure");

        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> LowFireTransform.render(
                new LowFireSettings(true, 0.5),
                stack(calls),
                () -> {
                    calls.add("render");
                    throw failure;
                }
        ));

        assertEquals(failure, thrown);
        assertEquals(List.of("push", "translate:-0.5", "render", "pop"), calls);
    }

    private static LowFireTransform.TransformStack stack(List<String> calls) {
        return new LowFireTransform.TransformStack() {
            @Override
            public void push() {
                calls.add("push");
            }

            @Override
            public void translateY(double offset) {
                calls.add("translate:" + offset);
            }

            @Override
            public void pop() {
                calls.add("pop");
            }
        };
    }
}
