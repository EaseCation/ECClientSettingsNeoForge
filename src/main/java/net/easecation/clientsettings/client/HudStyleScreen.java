package net.easecation.clientsettings.client;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.easecation.clientsettings.profile.model.ArgbColor;
import net.easecation.clientsettings.profile.model.HudTextColorMode;
import net.easecation.clientsettings.profile.model.HudSettings;
import net.easecation.clientsettings.profile.model.HudWidgetId;
import net.easecation.clientsettings.profile.model.HudWidgetStyle;
import net.easecation.clientsettings.profile.model.KeystrokesSettings;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.function.Supplier;

final class HudStyleScreen {

    private HudStyleScreen() {
    }

    static Screen create(Screen parent, ProfileSettingsDraft draft, HudWidgetId id) {
        HudWidgetStyle current = draft.hudSettings().widget(id).style();
        HudWidgetStyle defaults = HudWidgetStyle.defaultsFor(id);
        StyleDraft style = new StyleDraft(current);
        KeystrokesDraft keystrokes = id == HudWidgetId.KEYSTROKES
                ? new KeystrokesDraft(draft.hudSettings().keystrokes())
                : null;

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable(
                        "screen.ecclientsettings.hud_style.title",
                        Component.translatable(widgetTranslationKey(id))
                ));
        ConfigEntryBuilder entries = builder.entryBuilder();
        ConfigCategory category = builder.getOrCreateCategory(
                Component.translatable("category.ecclientsettings.hud_style")
        );

        var textMode = entries.startEnumSelector(
                        Component.translatable("option.ecclientsettings.hud_style.text_mode"),
                        HudTextColorMode.class,
                        current.textColorMode()
                )
                .setDefaultValue(defaults.textColorMode())
                .setEnumNameProvider(mode -> Component.translatable(
                        "option.ecclientsettings.hud_style.text_mode."
                                + mode.name().toLowerCase(Locale.ROOT)
                ))
                .setTooltip(Component.translatable("option.ecclientsettings.hud_style.text_mode.tooltip"))
                .setSaveConsumer(value -> style.textColorMode = value)
                .build();
        var textColor = entries.startAlphaColorField(
                        Component.translatable("option.ecclientsettings.hud_style.text_color"),
                        current.textColor().value()
                )
                .setDefaultValue(defaults.textColor().value())
                .setTooltip(Component.translatable("option.ecclientsettings.hud_style.text_color.tooltip"))
                .setSaveConsumer(value -> style.textColor = new ArgbColor(value))
                .build();
        var textShadow = entries.startBooleanToggle(
                        Component.translatable("option.ecclientsettings.hud_style.text_shadow"),
                        current.textShadow()
                )
                .setDefaultValue(defaults.textShadow())
                .setSaveConsumer(value -> style.textShadow = value)
                .build();
        var animationSpeed = entries.startDoubleField(
                        Component.translatable("option.ecclientsettings.hud_style.animation_speed"),
                        current.animationSpeed()
                )
                .setDefaultValue(defaults.animationSpeed())
                .setMin(HudWidgetStyle.MIN_ANIMATION_SPEED)
                .setMax(HudWidgetStyle.MAX_ANIMATION_SPEED)
                .setTooltip(Component.translatable("option.ecclientsettings.hud_style.animation_speed.tooltip"))
                .setSaveConsumer(value -> style.animationSpeed = value)
                .build();
        var rainbowSpread = entries.startDoubleField(
                        Component.translatable("option.ecclientsettings.hud_style.rainbow_spread"),
                        current.rainbowSpread()
                )
                .setDefaultValue(defaults.rainbowSpread())
                .setMin(HudWidgetStyle.MIN_RAINBOW_SPREAD)
                .setMax(HudWidgetStyle.MAX_RAINBOW_SPREAD)
                .setTooltip(Component.translatable("option.ecclientsettings.hud_style.rainbow_spread.tooltip"))
                .setSaveConsumer(value -> style.rainbowSpread = value)
                .build();
        var backgroundEnabled = entries.startBooleanToggle(
                        Component.translatable("option.ecclientsettings.hud_style.background_enabled"),
                        current.backgroundEnabled()
                )
                .setDefaultValue(defaults.backgroundEnabled())
                .setSaveConsumer(value -> style.backgroundEnabled = value)
                .build();
        var backgroundColor = entries.startAlphaColorField(
                        Component.translatable("option.ecclientsettings.hud_style.background_color"),
                        current.backgroundColor().value()
                )
                .setDefaultValue(defaults.backgroundColor().value())
                .setSaveConsumer(value -> style.backgroundColor = new ArgbColor(value))
                .build();
        var borderEnabled = entries.startBooleanToggle(
                        Component.translatable("option.ecclientsettings.hud_style.border_enabled"),
                        current.borderEnabled()
                )
                .setDefaultValue(defaults.borderEnabled())
                .setSaveConsumer(value -> style.borderEnabled = value)
                .build();
        var borderColor = entries.startAlphaColorField(
                        Component.translatable("option.ecclientsettings.hud_style.border_color"),
                        current.borderColor().value()
                )
                .setDefaultValue(defaults.borderColor().value())
                .setSaveConsumer(value -> style.borderColor = new ArgbColor(value))
                .build();
        var borderWidth = entries.startIntSlider(
                        Component.translatable("option.ecclientsettings.hud_style.border_width"),
                        current.borderWidth(),
                        HudWidgetStyle.MIN_BORDER_WIDTH,
                        HudWidgetStyle.MAX_BORDER_WIDTH
                )
                .setDefaultValue(defaults.borderWidth())
                .setSaveConsumer(value -> style.borderWidth = value)
                .build();
        var padding = entries.startIntSlider(
                        Component.translatable("option.ecclientsettings.hud_style.padding"),
                        current.padding(),
                        HudWidgetStyle.MIN_PADDING,
                        HudWidgetStyle.MAX_PADDING
                )
                .setDefaultValue(defaults.padding())
                .setTooltip(Component.translatable("option.ecclientsettings.hud_style.padding.tooltip"))
                .setSaveConsumer(value -> style.padding = value)
                .build();

        Supplier<HudWidgetStyle> liveStyle = () -> new HudWidgetStyle(
                backgroundEnabled.getValue(),
                new ArgbColor(backgroundColor.getValue()),
                borderEnabled.getValue(),
                new ArgbColor(borderColor.getValue()),
                borderWidth.getValue(),
                padding.getValue(),
                textShadow.getValue(),
                textMode.getValue(),
                new ArgbColor(textColor.getValue()),
                animationSpeed.getValue(),
                rainbowSpread.getValue()
        );

        category.addEntry(entries.startTextDescription(
                Component.translatable("option.ecclientsettings.hud_style.description")
        ).build());
        if (keystrokes == null) {
            category.addEntry(new HudStylePreviewEntry(id, liveStyle));
        } else {
            addKeystrokesCategory(
                    builder,
                    entries,
                    draft.hudSettings(),
                    category,
                    keystrokes,
                    liveStyle
            );
        }
        category.addEntry(textMode);
        category.addEntry(textColor);
        category.addEntry(textShadow);
        category.addEntry(animationSpeed);
        category.addEntry(rainbowSpread);
        category.addEntry(backgroundEnabled);
        category.addEntry(backgroundColor);
        category.addEntry(borderEnabled);
        category.addEntry(borderColor);
        category.addEntry(borderWidth);
        category.addEntry(padding);

        builder.setSavingRunnable(() -> {
            draft.setHudStyle(id, style.materialize());
            if (keystrokes != null) {
                draft.setKeystrokesSettings(keystrokes.materialize());
            }
        });
        return builder.build();
    }

    private static void addKeystrokesCategory(
            ConfigBuilder builder,
            ConfigEntryBuilder entries,
            HudSettings baseSettings,
            ConfigCategory styleCategory,
            KeystrokesDraft draft,
            Supplier<HudWidgetStyle> styleSupplier
    ) {
        KeystrokesSettings defaults = KeystrokesSettings.DEFAULT;
        var showMovement = entries.startBooleanToggle(
                        Component.translatable("option.ecclientsettings.keystrokes.show_movement"),
                        draft.showMovement
                )
                .setDefaultValue(defaults.showMovement())
                .setSaveConsumer(value -> draft.showMovement = value)
                .build();
        var showJump = entries.startBooleanToggle(
                        Component.translatable("option.ecclientsettings.keystrokes.show_jump"),
                        draft.showJump
                )
                .setDefaultValue(defaults.showJump())
                .setSaveConsumer(value -> draft.showJump = value)
                .build();
        var showMouseButtons = entries.startBooleanToggle(
                        Component.translatable("option.ecclientsettings.keystrokes.show_mouse_buttons"),
                        draft.showMouseButtons
                )
                .setDefaultValue(defaults.showMouseButtons())
                .setSaveConsumer(value -> draft.showMouseButtons = value)
                .build();
        var showCps = entries.startBooleanToggle(
                        Component.translatable("option.ecclientsettings.keystrokes.show_cps"),
                        draft.showCps
                )
                .setDefaultValue(defaults.showCps())
                .setTooltip(Component.translatable("option.ecclientsettings.keystrokes.show_cps.tooltip"))
                .setSaveConsumer(value -> draft.showCps = value)
                .build();
        var keySize = entries.startIntSlider(
                        Component.translatable("option.ecclientsettings.keystrokes.key_size"),
                        draft.keySize,
                        KeystrokesSettings.MIN_KEY_SIZE,
                        KeystrokesSettings.MAX_KEY_SIZE
                )
                .setDefaultValue(defaults.keySize())
                .setSaveConsumer(value -> draft.keySize = value)
                .build();
        var gap = entries.startIntSlider(
                        Component.translatable("option.ecclientsettings.keystrokes.gap"),
                        draft.gap,
                        KeystrokesSettings.MIN_GAP,
                        KeystrokesSettings.MAX_GAP
                )
                .setDefaultValue(defaults.gap())
                .setSaveConsumer(value -> draft.gap = value)
                .build();
        var cornerRadius = entries.startIntSlider(
                        Component.translatable("option.ecclientsettings.keystrokes.corner_radius"),
                        draft.cornerRadius,
                        KeystrokesSettings.MIN_CORNER_RADIUS,
                        KeystrokesSettings.MAX_CORNER_RADIUS
                )
                .setDefaultValue(defaults.cornerRadius())
                .setSaveConsumer(value -> draft.cornerRadius = value)
                .build();
        var keyBorderWidth = entries.startIntSlider(
                        Component.translatable("option.ecclientsettings.keystrokes.key_border_width"),
                        draft.keyBorderWidth,
                        KeystrokesSettings.MIN_KEY_BORDER_WIDTH,
                        KeystrokesSettings.MAX_KEY_BORDER_WIDTH
                )
                .setDefaultValue(defaults.keyBorderWidth())
                .setSaveConsumer(value -> draft.keyBorderWidth = value)
                .build();
        var pressAnimationMillis = entries.startIntSlider(
                        Component.translatable("option.ecclientsettings.keystrokes.press_animation"),
                        draft.pressAnimationMillis,
                        KeystrokesSettings.MIN_PRESS_ANIMATION_MILLIS,
                        KeystrokesSettings.MAX_PRESS_ANIMATION_MILLIS
                )
                .setDefaultValue(defaults.pressAnimationMillis())
                .setTooltip(Component.translatable(
                        "option.ecclientsettings.keystrokes.press_animation.tooltip"
                ))
                .setSaveConsumer(value -> draft.pressAnimationMillis = value)
                .build();
        var idleBackgroundColor = entries.startAlphaColorField(
                        Component.translatable("option.ecclientsettings.keystrokes.idle_background"),
                        draft.idleBackgroundColor.value()
                )
                .setDefaultValue(defaults.idleBackgroundColor().value())
                .setSaveConsumer(value -> draft.idleBackgroundColor = new ArgbColor(value))
                .build();
        var pressedBackgroundColor = entries.startAlphaColorField(
                        Component.translatable("option.ecclientsettings.keystrokes.pressed_background"),
                        draft.pressedBackgroundColor.value()
                )
                .setDefaultValue(defaults.pressedBackgroundColor().value())
                .setSaveConsumer(value -> draft.pressedBackgroundColor = new ArgbColor(value))
                .build();
        var idleBorderColor = entries.startAlphaColorField(
                        Component.translatable("option.ecclientsettings.keystrokes.idle_border"),
                        draft.idleBorderColor.value()
                )
                .setDefaultValue(defaults.idleBorderColor().value())
                .setSaveConsumer(value -> draft.idleBorderColor = new ArgbColor(value))
                .build();
        var pressedBorderColor = entries.startAlphaColorField(
                        Component.translatable("option.ecclientsettings.keystrokes.pressed_border"),
                        draft.pressedBorderColor.value()
                )
                .setDefaultValue(defaults.pressedBorderColor().value())
                .setSaveConsumer(value -> draft.pressedBorderColor = new ArgbColor(value))
                .build();
        var pressedTextColor = entries.startAlphaColorField(
                        Component.translatable("option.ecclientsettings.keystrokes.pressed_text"),
                        draft.pressedTextColor.value()
                )
                .setDefaultValue(defaults.pressedTextColor().value())
                .setTooltip(Component.translatable("option.ecclientsettings.keystrokes.pressed_text.tooltip"))
                .setSaveConsumer(value -> draft.pressedTextColor = new ArgbColor(value))
                .build();

        Supplier<KeystrokesSettings> liveSettings = () -> new KeystrokesSettings(
                showMovement.getValue(),
                showJump.getValue(),
                showMouseButtons.getValue(),
                showCps.getValue(),
                keySize.getValue(),
                gap.getValue(),
                cornerRadius.getValue(),
                keyBorderWidth.getValue(),
                new ArgbColor(idleBackgroundColor.getValue()),
                new ArgbColor(pressedBackgroundColor.getValue()),
                new ArgbColor(idleBorderColor.getValue()),
                new ArgbColor(pressedBorderColor.getValue()),
                new ArgbColor(pressedTextColor.getValue()),
                pressAnimationMillis.getValue()
        );
        styleCategory.addEntry(new KeystrokesStylePreviewEntry(
                baseSettings,
                styleSupplier,
                liveSettings
        ));

        ConfigCategory category = builder.getOrCreateCategory(
                Component.translatable("category.ecclientsettings.keystrokes")
        );
        category.addEntry(entries.startTextDescription(
                Component.translatable("option.ecclientsettings.keystrokes.description")
        ).build());
        category.addEntry(showMovement);
        category.addEntry(showJump);
        category.addEntry(showMouseButtons);
        category.addEntry(showCps);
        category.addEntry(keySize);
        category.addEntry(gap);
        category.addEntry(cornerRadius);
        category.addEntry(keyBorderWidth);
        category.addEntry(pressAnimationMillis);
        category.addEntry(idleBackgroundColor);
        category.addEntry(pressedBackgroundColor);
        category.addEntry(idleBorderColor);
        category.addEntry(pressedBorderColor);
        category.addEntry(pressedTextColor);
    }

    private static String widgetTranslationKey(HudWidgetId id) {
        return "option.ecclientsettings.hud." + id.serializedName() + ".enabled";
    }

    private static final class StyleDraft {
        private boolean backgroundEnabled;
        private ArgbColor backgroundColor;
        private boolean borderEnabled;
        private ArgbColor borderColor;
        private int borderWidth;
        private int padding;
        private boolean textShadow;
        private HudTextColorMode textColorMode;
        private ArgbColor textColor;
        private double animationSpeed;
        private double rainbowSpread;

        private StyleDraft(HudWidgetStyle style) {
            backgroundEnabled = style.backgroundEnabled();
            backgroundColor = style.backgroundColor();
            borderEnabled = style.borderEnabled();
            borderColor = style.borderColor();
            borderWidth = style.borderWidth();
            padding = style.padding();
            textShadow = style.textShadow();
            textColorMode = style.textColorMode();
            textColor = style.textColor();
            animationSpeed = style.animationSpeed();
            rainbowSpread = style.rainbowSpread();
        }

        private HudWidgetStyle materialize() {
            return new HudWidgetStyle(
                    backgroundEnabled,
                    backgroundColor,
                    borderEnabled,
                    borderColor,
                    borderWidth,
                    padding,
                    textShadow,
                    textColorMode,
                    textColor,
                    animationSpeed,
                    rainbowSpread
            );
        }
    }

    private static final class KeystrokesDraft {
        private boolean showMovement;
        private boolean showJump;
        private boolean showMouseButtons;
        private boolean showCps;
        private int keySize;
        private int gap;
        private int cornerRadius;
        private int keyBorderWidth;
        private ArgbColor idleBackgroundColor;
        private ArgbColor pressedBackgroundColor;
        private ArgbColor idleBorderColor;
        private ArgbColor pressedBorderColor;
        private ArgbColor pressedTextColor;
        private int pressAnimationMillis;

        private KeystrokesDraft(KeystrokesSettings settings) {
            showMovement = settings.showMovement();
            showJump = settings.showJump();
            showMouseButtons = settings.showMouseButtons();
            showCps = settings.showCps();
            keySize = settings.keySize();
            gap = settings.gap();
            cornerRadius = settings.cornerRadius();
            keyBorderWidth = settings.keyBorderWidth();
            idleBackgroundColor = settings.idleBackgroundColor();
            pressedBackgroundColor = settings.pressedBackgroundColor();
            idleBorderColor = settings.idleBorderColor();
            pressedBorderColor = settings.pressedBorderColor();
            pressedTextColor = settings.pressedTextColor();
            pressAnimationMillis = settings.pressAnimationMillis();
        }

        private KeystrokesSettings materialize() {
            return new KeystrokesSettings(
                    showMovement,
                    showJump,
                    showMouseButtons,
                    showCps,
                    keySize,
                    gap,
                    cornerRadius,
                    keyBorderWidth,
                    idleBackgroundColor,
                    pressedBackgroundColor,
                    idleBorderColor,
                    pressedBorderColor,
                    pressedTextColor,
                    pressAnimationMillis
            );
        }
    }
}
