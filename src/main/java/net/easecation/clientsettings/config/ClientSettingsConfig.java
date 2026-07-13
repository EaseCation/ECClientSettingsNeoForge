package net.easecation.clientsettings.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ClientSettingsConfig {

    public static final boolean DEFAULT_FORCE_SPRINT = true;
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue FORCE_SPRINT;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("movement");
        FORCE_SPRINT = builder
                .comment("Automatically hold the vanilla sprint input while moving forward.")
                .translation("option.ecclientsettings.force_sprint")
                .define("forceSprint", DEFAULT_FORCE_SPRINT);
        builder.pop();
        SPEC = builder.build();
    }

    private ClientSettingsConfig() {
    }

    public static boolean forceSprint() {
        return FORCE_SPRINT.get();
    }

    public static void setForceSprint(boolean enabled) {
        FORCE_SPRINT.set(enabled);
        SPEC.save();
    }
}
