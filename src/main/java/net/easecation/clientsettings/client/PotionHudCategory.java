package net.easecation.clientsettings.client;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.easecation.clientsettings.profile.model.PotionHudSettings;
import net.easecation.clientsettings.profile.model.ArgbColor;
import net.minecraft.network.chat.Component;
import java.util.function.Supplier;

final class PotionHudCategory {
    private PotionHudCategory() {}
    static Supplier<PotionHudSettings> add(ConfigBuilder builder, ConfigEntryBuilder entries,
            PotionHudSettings current, net.easecation.clientsettings.profile.model.HudSettings baseSettings,
            Supplier<net.easecation.clientsettings.profile.model.HudWidgetStyle> liveStyle) {
        var category = builder.getOrCreateCategory(Component.translatable("category.ecclientsettings.potions"));
        var defaults = PotionHudSettings.DEFAULT;
        category.addEntry(entries.startTextDescription(Component.translatable("option.ecclientsettings.potions.description")).build());
        var controls = new java.util.ArrayList<me.shedaniel.clothconfig2.api.AbstractConfigListEntry<?>>();
        controls.add(entries.startTextDescription(Component.translatable("option.ecclientsettings.potions.group.hideVanilla")).build());
        var hideVanilla = entries.startBooleanToggle(Component.translatable("option.ecclientsettings.potions.hideVanilla"), current.hideVanilla())
                .setDefaultValue(defaults.hideVanilla()).build();
        controls.add(hideVanilla);
        var vanillaLevel = entries.startBooleanToggle(Component.translatable("option.ecclientsettings.potions.vanillaLevel"), current.vanillaLevel())
                .setDefaultValue(defaults.vanillaLevel()).build();
        controls.add(vanillaLevel);
        var vanillaTime = entries.startBooleanToggle(Component.translatable("option.ecclientsettings.potions.vanillaTime"), current.vanillaTime())
                .setDefaultValue(defaults.vanillaTime()).build();
        controls.add(vanillaTime);
        var vanillaLevelColor = entries.startAlphaColorField(Component.translatable("option.ecclientsettings.potions.vanillaLevelColor"), current.vanillaLevelColor().value())
                .setDefaultValue(defaults.vanillaLevelColor().value()).build();
        controls.add(vanillaLevelColor);
        var vanillaTimeColor = entries.startAlphaColorField(Component.translatable("option.ecclientsettings.potions.vanillaTimeColor"), current.vanillaTimeColor().value())
                .setDefaultValue(defaults.vanillaTimeColor().value()).build();
        controls.add(vanillaTimeColor);
        int ecPreviewIndex = controls.size();
        controls.add(entries.startTextDescription(Component.translatable("option.ecclientsettings.potions.group.showName")).build());
        var showName = entries.startBooleanToggle(Component.translatable("option.ecclientsettings.potions.showName"), current.showName())
                .setDefaultValue(defaults.showName()).build();
        controls.add(showName);
        var showLevel = entries.startBooleanToggle(Component.translatable("option.ecclientsettings.potions.showLevel"), current.showLevel())
                .setDefaultValue(defaults.showLevel()).build();
        controls.add(showLevel);
        var showTime = entries.startBooleanToggle(Component.translatable("option.ecclientsettings.potions.showTime"), current.showTime())
                .setDefaultValue(defaults.showTime()).build();
        controls.add(showTime);
        var effectNameColor = entries.startBooleanToggle(Component.translatable("option.ecclientsettings.potions.effectNameColor"), current.effectNameColor())
                .setDefaultValue(defaults.effectNameColor()).build();
        controls.add(effectNameColor);
        var levelColor = entries.startAlphaColorField(Component.translatable("option.ecclientsettings.potions.levelColor"), current.levelColor().value())
                .setDefaultValue(defaults.levelColor().value()).build();
        controls.add(levelColor);
        var timeColor = entries.startAlphaColorField(Component.translatable("option.ecclientsettings.potions.timeColor"), current.timeColor().value())
                .setDefaultValue(defaults.timeColor().value()).build();
        controls.add(timeColor);
        controls.add(entries.startTextDescription(Component.translatable("option.ecclientsettings.potions.group.warningEnabled")).build());
        var warningEnabled = entries.startBooleanToggle(Component.translatable("option.ecclientsettings.potions.warningEnabled"), current.warningEnabled())
                .setDefaultValue(defaults.warningEnabled())
                .setTooltip(Component.translatable("option.ecclientsettings.potions.warningEnabled.tooltip")).build();
        controls.add(warningEnabled);
        var warningSeconds = entries.startIntField(Component.translatable("option.ecclientsettings.potions.warningSeconds"), current.warningSeconds())
                .setDefaultValue(defaults.warningSeconds()).setMin(1).setMax(300)
                .setTooltip(Component.translatable("option.ecclientsettings.potions.warningSeconds.tooltip")).build();
        controls.add(warningSeconds);
        Supplier<PotionHudSettings> live = () -> new PotionHudSettings(
                hideVanilla.getValue(), vanillaLevel.getValue(), vanillaTime.getValue(), new ArgbColor(vanillaLevelColor.getValue()), new ArgbColor(vanillaTimeColor.getValue()), showName.getValue(), showLevel.getValue(), showTime.getValue(), effectNameColor.getValue(), new ArgbColor(levelColor.getValue()), new ArgbColor(timeColor.getValue()), warningEnabled.getValue(), warningSeconds.getValue()
        );
        category.addEntry(new VanillaPotionPreviewEntry(live));
        controls.add(ecPreviewIndex + 1, new PotionStylePreviewEntry(baseSettings, liveStyle, live));
        controls.forEach(category::addEntry);
        return live;
    }
}
