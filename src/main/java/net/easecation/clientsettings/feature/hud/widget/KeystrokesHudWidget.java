package net.easecation.clientsettings.feature.hud.widget;

import net.easecation.clientsettings.feature.hud.HudRenderContext;
import net.easecation.clientsettings.feature.hud.HudShapeRenderer;
import net.easecation.clientsettings.feature.hud.HudSize;
import net.easecation.clientsettings.feature.hud.HudTextRenderer;
import net.easecation.clientsettings.feature.hud.HudWidget;
import net.easecation.clientsettings.feature.hud.keystrokes.KeystrokesInputTracker;
import net.easecation.clientsettings.profile.model.HudSettings;
import net.easecation.clientsettings.profile.model.HudWidgetId;
import net.easecation.clientsettings.profile.model.KeystrokesSettings;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;

import java.util.EnumMap;
import java.util.Map;

public final class KeystrokesHudWidget implements HudWidget {

    private static final int MIN_CPS_MOUSE_BUTTON_WIDTH = 40;

    private final Map<KeySlot, PressTransition> transitions = new EnumMap<>(KeySlot.class);

    public KeystrokesHudWidget() {
        for (KeySlot slot : KeySlot.values()) {
            transitions.put(slot, new PressTransition());
        }
    }

    @Override
    public HudWidgetId id() {
        return HudWidgetId.KEYSTROKES;
    }

    @Override
    public HudSize previewSize() {
        return size(KeystrokesSettings.DEFAULT);
    }

    @Override
    public HudSize previewSize(HudSettings settings) {
        return size(settings.keystrokes());
    }

    @Override
    public HudSize measure(HudRenderContext context) {
        return size(context.hudSettings().keystrokes());
    }

    @Override
    public boolean shouldRender(HudRenderContext context) {
        KeystrokesSettings settings = context.hudSettings().keystrokes();
        return context.preview()
                || settings.showMovement()
                || settings.showMouseButtons()
                || settings.showJump();
    }

    @Override
    public void render(HudRenderContext context, HudSize size) {
        KeystrokesSettings settings = context.hudSettings().keystrokes();
        int key = settings.keySize();
        int gap = settings.gap();
        int width = size.width();
        int y = 0;

        if (settings.showMovement()) {
            int movementWidth = key * 3 + gap * 2;
            int movementX = (width - movementWidth) / 2;
            int centeredX = (width - key) / 2;
            drawKey(context, settings, KeySlot.FORWARD, label(context, KeySlot.FORWARD), centeredX, y, key, key);
            y += key + gap;
            drawKey(context, settings, KeySlot.LEFT, label(context, KeySlot.LEFT), movementX, y, key, key);
            drawKey(
                    context,
                    settings,
                    KeySlot.BACK,
                    label(context, KeySlot.BACK),
                    movementX + key + gap,
                    y,
                    key,
                    key
            );
            drawKey(
                    context,
                    settings,
                    KeySlot.RIGHT,
                    label(context, KeySlot.RIGHT),
                    movementX + (key + gap) * 2,
                    y,
                    key,
                    key
            );
            y += key;
        }

        if (settings.showMouseButtons()) {
            if (y > 0) {
                y += gap;
            }
            int buttonWidth = (width - gap) / 2;
            int mouseHeight = settings.showCps() ? Math.max(key, 22) : key;
            drawMouseKey(
                    context,
                    settings,
                    KeySlot.LEFT_MOUSE,
                    Component.translatable("hud.ecclientsettings.keystrokes.left_mouse").getString(),
                    context.preview() ? 8 : KeystrokesInputTracker.leftCps(),
                    0,
                    y,
                    buttonWidth,
                    mouseHeight
            );
            drawMouseKey(
                    context,
                    settings,
                    KeySlot.RIGHT_MOUSE,
                    Component.translatable("hud.ecclientsettings.keystrokes.right_mouse").getString(),
                    context.preview() ? 4 : KeystrokesInputTracker.rightCps(),
                    buttonWidth + gap,
                    y,
                    buttonWidth,
                    mouseHeight
            );
            y += mouseHeight;
        }

        if (settings.showJump()) {
            if (y > 0) {
                y += gap;
            }
            int jumpHeight = Math.max(10, key / 2);
            drawKey(context, settings, KeySlot.JUMP, label(context, KeySlot.JUMP), 0, y, width, jumpHeight);
        }
    }

    private void drawMouseKey(
            HudRenderContext context,
            KeystrokesSettings settings,
            KeySlot slot,
            String label,
            int cps,
            int x,
            int y,
            int width,
            int height
    ) {
        double amount = pressAmount(context, settings, slot);
        drawKeyBox(context, settings, amount, x, y, width, height);
        if (!settings.showCps()) {
            drawCenteredText(context, settings, amount, label, x, y, width, height);
            return;
        }
        drawCenteredTextAt(context, settings, amount, label, x, y + 1, width);
        drawCenteredTextAt(context, settings, amount, cpsLabel(cps), x, y + 11, width);
    }

    private void drawKey(
            HudRenderContext context,
            KeystrokesSettings settings,
            KeySlot slot,
            String label,
            int x,
            int y,
            int width,
            int height
    ) {
        double amount = pressAmount(context, settings, slot);
        drawKeyBox(context, settings, amount, x, y, width, height);
        drawCenteredText(context, settings, amount, label, x, y, width, height);
    }

    private static void drawKeyBox(
            HudRenderContext context,
            KeystrokesSettings settings,
            double amount,
            int x,
            int y,
            int width,
            int height
    ) {
        int borderWidth = Math.min(settings.keyBorderWidth(), Math.min(width, height) / 2);
        int borderColor = blendArgb(
                settings.idleBorderColor().value(),
                settings.pressedBorderColor().value(),
                amount
        );
        int backgroundColor = blendArgb(
                settings.idleBackgroundColor().value(),
                settings.pressedBackgroundColor().value(),
                amount
        );
        if ((backgroundColor >>> 24) != 0) {
            HudShapeRenderer.fillRoundedRect(
                    context.graphics(),
                    x,
                    y,
                    width,
                    height,
                    settings.cornerRadius(),
                    backgroundColor
            );
        }
        if (borderWidth > 0 && (borderColor >>> 24) != 0) {
            HudShapeRenderer.drawRoundedBorder(
                    context.graphics(),
                    x,
                    y,
                    width,
                    height,
                    settings.cornerRadius(),
                    borderWidth,
                    borderColor
            );
        }
    }

    private static void drawCenteredText(
            HudRenderContext context,
            KeystrokesSettings settings,
            double amount,
            String text,
            int x,
            int y,
            int width,
            int height
    ) {
        int textY = y + (height - context.font().lineHeight) / 2;
        drawCenteredTextAt(context, settings, amount, text, x, textY, width);
    }

    private static void drawCenteredTextAt(
            HudRenderContext context,
            KeystrokesSettings settings,
            double amount,
            String text,
            int x,
            int y,
            int width
    ) {
        String fitted = fitText(context, text, Math.max(1, width - 4));
        int textX = x + (width - context.font().width(fitted)) / 2;
        if (amount >= 0.5) {
            HudTextRenderer.drawFixed(context, fitted, textX, y, settings.pressedTextColor().value());
        } else {
            HudTextRenderer.draw(context, fitted, textX, y);
        }
    }

    private double pressAmount(HudRenderContext context, KeystrokesSettings settings, KeySlot slot) {
        if (context.preview()) {
            return slot == KeySlot.FORWARD || slot == KeySlot.LEFT_MOUSE ? 1.0 : 0.0;
        }
        return transitions.get(slot).update(
                pressed(context, slot),
                settings.pressAnimationMillis(),
                System.nanoTime()
        );
    }

    private static boolean pressed(HudRenderContext context, KeySlot slot) {
        return switch (slot) {
            case FORWARD -> context.minecraft().options.keyUp.isDown();
            case LEFT -> context.minecraft().options.keyLeft.isDown();
            case BACK -> context.minecraft().options.keyDown.isDown();
            case RIGHT -> context.minecraft().options.keyRight.isDown();
            case JUMP -> context.minecraft().options.keyJump.isDown();
            case LEFT_MOUSE -> KeystrokesInputTracker.leftMouseDown();
            case RIGHT_MOUSE -> KeystrokesInputTracker.rightMouseDown();
        };
    }

    private static String label(HudRenderContext context, KeySlot slot) {
        KeyMapping mapping = switch (slot) {
            case FORWARD -> context.minecraft().options.keyUp;
            case LEFT -> context.minecraft().options.keyLeft;
            case BACK -> context.minecraft().options.keyDown;
            case RIGHT -> context.minecraft().options.keyRight;
            case JUMP -> context.minecraft().options.keyJump;
            default -> throw new IllegalArgumentException("Mouse slots use explicit labels");
        };
        return mapping.getTranslatedKeyMessage().getString();
    }

    private static HudSize size(KeystrokesSettings settings) {
        int key = settings.keySize();
        int gap = settings.gap();
        int width = key * 3 + gap * 2;
        if (settings.showMouseButtons() && settings.showCps()) {
            width = Math.max(width, MIN_CPS_MOUSE_BUTTON_WIDTH * 2 + gap);
        }
        int height = 0;
        if (settings.showMovement()) {
            height += key * 2 + gap;
        }
        if (settings.showMouseButtons()) {
            if (height > 0) {
                height += gap;
            }
            height += settings.showCps() ? Math.max(key, 22) : key;
        }
        if (settings.showJump()) {
            if (height > 0) {
                height += gap;
            }
            height += Math.max(10, key / 2);
        }
        return new HudSize(width, Math.max(key, height));
    }

    private static String cpsLabel(int cps) {
        return cps > 99 ? "99+" : cps + " CPS";
    }

    private static String fitText(HudRenderContext context, String text, int maximumWidth) {
        if (context.font().width(text) <= maximumWidth) {
            return text;
        }
        String suffix = "...";
        int contentWidth = Math.max(0, maximumWidth - context.font().width(suffix));
        return context.font().plainSubstrByWidth(text, contentWidth) + suffix;
    }

    private static int blendArgb(int from, int to, double amount) {
        double eased = amount * amount * (3.0 - 2.0 * amount);
        int alpha = blendChannel(from >>> 24, to >>> 24, eased);
        int red = blendChannel(from >>> 16, to >>> 16, eased);
        int green = blendChannel(from >>> 8, to >>> 8, eased);
        int blue = blendChannel(from, to, eased);
        return alpha << 24 | red << 16 | green << 8 | blue;
    }

    private static int blendChannel(int from, int to, double amount) {
        return (int) Math.round((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * amount);
    }

    private enum KeySlot {
        FORWARD,
        LEFT,
        BACK,
        RIGHT,
        JUMP,
        LEFT_MOUSE,
        RIGHT_MOUSE
    }

    private static final class PressTransition {
        private double amount;
        private long lastUpdateNanos;

        private double update(boolean pressed, int durationMillis, long nowNanos) {
            if (durationMillis == 0 || lastUpdateNanos == 0L) {
                amount = pressed ? 1.0 : 0.0;
                lastUpdateNanos = nowNanos;
                return amount;
            }
            double elapsedMillis = (nowNanos - lastUpdateNanos) / 1_000_000.0;
            double step = elapsedMillis / durationMillis;
            amount = pressed ? Math.min(1.0, amount + step) : Math.max(0.0, amount - step);
            lastUpdateNanos = nowNanos;
            return amount;
        }
    }
}
