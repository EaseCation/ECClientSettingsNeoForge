package net.easecation.clientsettings.feature.hud.keystrokes;

import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.lifecycle.ClientStoppingEvent;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.Deque;

public final class KeystrokesInputTracker {

    private static final long CPS_WINDOW_NANOS = 1_000_000_000L;
    private static final Deque<Long> LEFT_CLICKS = new ArrayDeque<>();
    private static final Deque<Long> RIGHT_CLICKS = new ArrayDeque<>();

    private KeystrokesInputTracker() {
    }

    public static void onMouseButton(InputEvent.MouseButton.Pre event) {
        if (event.getAction() != GLFW.GLFW_PRESS || !validGameplayContext()) {
            return;
        }
        long now = System.nanoTime();
        if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            record(LEFT_CLICKS, now);
        } else if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            record(RIGHT_CLICKS, now);
        }
    }

    public static boolean leftMouseDown() {
        return mouseDown(GLFW.GLFW_MOUSE_BUTTON_LEFT);
    }

    public static boolean rightMouseDown() {
        return mouseDown(GLFW.GLFW_MOUSE_BUTTON_RIGHT);
    }

    public static int leftCps() {
        return cps(LEFT_CLICKS, System.nanoTime());
    }

    public static int rightCps() {
        return cps(RIGHT_CLICKS, System.nanoTime());
    }

    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        clear();
    }

    public static void onClientStopping(ClientStoppingEvent event) {
        clear();
    }

    private static void record(Deque<Long> clicks, long now) {
        prune(clicks, now);
        clicks.addLast(now);
    }

    private static int cps(Deque<Long> clicks, long now) {
        prune(clicks, now);
        return clicks.size();
    }

    private static void prune(Deque<Long> clicks, long now) {
        long cutoff = now - CPS_WINDOW_NANOS;
        while (!clicks.isEmpty() && clicks.getFirst() <= cutoff) {
            clicks.removeFirst();
        }
    }

    private static boolean mouseDown(int button) {
        Minecraft minecraft = Minecraft.getInstance();
        return validGameplayContext()
                && GLFW.glfwGetMouseButton(minecraft.getWindow().getWindow(), button) == GLFW.GLFW_PRESS;
    }

    private static boolean validGameplayContext() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.screen == null
                && minecraft.player != null
                && minecraft.level != null
                && minecraft.isWindowActive();
    }

    private static void clear() {
        LEFT_CLICKS.clear();
        RIGHT_CLICKS.clear();
    }
}
