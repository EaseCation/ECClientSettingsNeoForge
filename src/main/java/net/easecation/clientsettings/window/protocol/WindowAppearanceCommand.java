package net.easecation.clientsettings.window.protocol;

public record WindowAppearanceCommand(TitleAppearance title, FrameAppearance frame) {

    public WindowAppearanceCommand {
        if (title == null && frame == null) {
            throw new IllegalArgumentException("Window appearance command must change title or frame");
        }
    }
}
