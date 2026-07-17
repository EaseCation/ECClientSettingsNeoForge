package net.easecation.clientsettings.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ClientSettingsConfig {

    public static final boolean DEFAULT_FORCE_SPRINT = true;
    public static final boolean DEFAULT_ALLOW_SERVER_WINDOW_TITLE = true;
    public static final boolean DEFAULT_ALLOW_SERVER_WINDOW_FRAME = true;
    public static final int PROFILE_MIGRATION_VERSION = 1;
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue FORCE_SPRINT;
    public static final ModConfigSpec.IntValue PROFILE_MIGRATION;
    public static final ModConfigSpec.BooleanValue ALLOW_SERVER_WINDOW_TITLE;
    public static final ModConfigSpec.BooleanValue ALLOW_SERVER_WINDOW_FRAME;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("movement");
        FORCE_SPRINT = builder
                .comment("Deprecated migration source. Runtime force sprint is stored in the active Profile.")
                .translation("option.ecclientsettings.force_sprint")
                .define("forceSprint", DEFAULT_FORCE_SPRINT);
        builder.pop();

        builder.push("migration");
        PROFILE_MIGRATION = builder
                .comment("Internal migration marker for versioned Profile storage.")
                .defineInRange("profileVersion", 0, 0, PROFILE_MIGRATION_VERSION);
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

    public static boolean legacyForceSprint() {
        return FORCE_SPRINT.get();
    }

    public static int profileMigrationVersion() {
        return PROFILE_MIGRATION.get();
    }

    public static void setProfileMigrationVersion(int version) {
        PROFILE_MIGRATION.set(version);
        SPEC.save();
    }

    public static boolean allowServerWindowTitle() {
        return ALLOW_SERVER_WINDOW_TITLE.get();
    }

    public static boolean allowServerWindowFrame() {
        return ALLOW_SERVER_WINDOW_FRAME.get();
    }

    public static void setServerWindowPermissions(boolean allowTitle, boolean allowFrame) {
        ALLOW_SERVER_WINDOW_TITLE.set(allowTitle);
        ALLOW_SERVER_WINDOW_FRAME.set(allowFrame);
        SPEC.save();
    }
}
