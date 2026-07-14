package net.easecation.clientsettings.window.windows;

import java.util.OptionalInt;

public interface DwmFacade {

    OptionalInt getInt(long hwnd, int attribute);

    boolean setInt(long hwnd, int attribute, int value);
}
