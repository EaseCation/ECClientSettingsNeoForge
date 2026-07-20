package net.easecation.clientsettings.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ClientSettingsConfig {

    public static final boolean DEFAULT_FORCE_SPRINT = true;
    public static final boolean DEFAULT_SWORD_BLOCKING_ANIMATION = false;
    public static final boolean DEFAULT_ALLOW_SERVER_WINDOW_TITLE = true;
    public static final boolean DEFAULT_ALLOW_SERVER_WINDOW_FRAME = true;
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue FORCE_SPRINT;
    public static final ModConfigSpec.BooleanValue SWORD_BLOCKING_ANIMATION;
    public static final ModConfigSpec.BooleanValue ALLOW_SERVER_WINDOW_TITLE;
    public static final ModConfigSpec.BooleanValue ALLOW_SERVER_WINDOW_FRAME;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("movement");
        FORCE_SPRINT = builder
                .comment("Automatically hold the vanilla sprint input while moving forward.")
                .translation("option.ecclientsettings.force_sprint")
                .define("forceSprint", DEFAULT_FORCE_SPRINT);
        builder.pop();

        builder.push("combat");
        SWORD_BLOCKING_ANIMATION = builder
                .comment("Play the legacy blocking animation when holding a sword and using it.")
                .translation("option.ecclientsettings.sword_blocking_animation")
                .define("swordBlockingAnimation", DEFAULT_SWORD_BLOCKING_ANIMATION);
        builder.pop();

        builder.push("serverWindowAppearance");
        ALLOW_SERVER_WINDOW_TITLE = builder
                .comment("Allow the connected server to customize the Minecraft window title.")
                .translation("option.ecclientsettings.server_window_title")
                .define("allowTitle", DEFAULT_ALLOW_SERVER_WINDOW_TITLE);
        ALLOW_SERVER_WINDOW_FRAME = builder
                .comment("Allow the connected server to customize the native window title bar colors.")
                .translation("option.ecclientsettings.server_window_frame")
                .define("allowFrame", DEFAULT_ALLOW_SERVER_WINDOW_FRAME);
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

    public static boolean swordBlockingAnimation() {
        return SWORD_BLOCKING_ANIMATION.get();
    }

    public static boolean allowServerWindowTitle() {
        return ALLOW_SERVER_WINDOW_TITLE.get();
    }

    public static boolean allowServerWindowFrame() {
        return ALLOW_SERVER_WINDOW_FRAME.get();
    }
}
