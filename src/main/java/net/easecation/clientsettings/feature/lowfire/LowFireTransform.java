package net.easecation.clientsettings.feature.lowfire;

import net.easecation.clientsettings.profile.model.LowFireSettings;

public final class LowFireTransform {

    private LowFireTransform() {
    }

    public static void render(LowFireSettings settings, TransformStack stack, Runnable vanillaRenderer) {
        if (!settings.enabled()) {
            vanillaRenderer.run();
            return;
        }

        stack.push();
        try {
            stack.translateY(-settings.verticalOffset());
            vanillaRenderer.run();
        } finally {
            stack.pop();
        }
    }

    public interface TransformStack {
        void push();

        void translateY(double offset);

        void pop();
    }
}
