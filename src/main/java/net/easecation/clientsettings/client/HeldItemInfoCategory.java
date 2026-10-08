package net.easecation.clientsettings.client;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.easecation.clientsettings.profile.model.HeldItemBackground;
import net.easecation.clientsettings.profile.model.HeldItemInfoSettings;
import net.minecraft.network.chat.Component;
import java.util.Locale;

final class HeldItemInfoCategory {
    private HeldItemInfoCategory() {}
    static void add(ConfigBuilder builder, ConfigEntryBuilder entries, ProfileSettingsDraft draft) {
        ConfigCategory category = builder.getOrCreateCategory(Component.translatable("category.ecclientsettings.held_item_info"));
        category.addEntry(entries.startTextDescription(Component.translatable("option.ecclientsettings.held_item_info.description")).build());
        HeldItemInfoSettings current = draft.features().heldItemInfo();
        HeldItemInfoSettings defaults = HeldItemInfoSettings.DEFAULT;
        category.addEntry(entries.startBooleanToggle(Component.translatable("option.ecclientsettings.held_item_info.enabled"), current.enabled())
                .setDefaultValue(defaults.enabled())
                .setSaveConsumer(draft::setHeldItemEnabled).build());
        category.addEntry(entries.startBooleanToggle(Component.translatable("option.ecclientsettings.held_item_info.showName"), current.showName())
                .setDefaultValue(defaults.showName())
                .setSaveConsumer(draft::setHeldItemShowName).build());
        category.addEntry(entries.startBooleanToggle(Component.translatable("option.ecclientsettings.held_item_info.showDescription"), current.showDescription())
                .setDefaultValue(defaults.showDescription())
                .setSaveConsumer(draft::setHeldItemShowDescription).build());
        category.addEntry(entries.startBooleanToggle(Component.translatable("option.ecclientsettings.held_item_info.showEnchantments"), current.showEnchantments())
                .setDefaultValue(defaults.showEnchantments())
                .setSaveConsumer(draft::setHeldItemShowEnchantments).build());
        category.addEntry(entries.startBooleanToggle(Component.translatable("option.ecclientsettings.held_item_info.showAdditional"), current.showAdditional())
                .setDefaultValue(defaults.showAdditional())
                .setSaveConsumer(draft::setHeldItemShowAdditional).build());
        category.addEntry(entries.startBooleanToggle(Component.translatable("option.ecclientsettings.held_item_info.showOmitted"), current.showOmitted())
                .setDefaultValue(defaults.showOmitted())
                .setSaveConsumer(draft::setHeldItemShowOmitted).build());
        category.addEntry(entries.startIntField(Component.translatable("option.ecclientsettings.held_item_info.maxCharacters"), current.maxCharacters())
                .setDefaultValue(defaults.maxCharacters())
                .setMin(1).setMax(256)
                .setSaveConsumer(draft::setHeldItemMaxCharacters).build());
        category.addEntry(entries.startIntField(Component.translatable("option.ecclientsettings.held_item_info.maxLines"), current.maxLines())
                .setDefaultValue(defaults.maxLines())
                .setMin(1).setMax(32)
                .setSaveConsumer(draft::setHeldItemMaxLines).build());
        category.addEntry(entries.startIntField(Component.translatable("option.ecclientsettings.held_item_info.maxDescriptionLines"), current.maxDescriptionLines())
                .setDefaultValue(defaults.maxDescriptionLines())
                .setMin(0).setMax(32)
                .setSaveConsumer(draft::setHeldItemMaxDescriptionLines).build());
        category.addEntry(entries.startIntField(Component.translatable("option.ecclientsettings.held_item_info.lineSpacing"), current.lineSpacing())
                .setDefaultValue(defaults.lineSpacing())
                .setMin(9).setMax(32)
                .setSaveConsumer(draft::setHeldItemLineSpacing).build());
        category.addEntry(entries.startIntField(Component.translatable("option.ecclientsettings.held_item_info.nameGap"), current.nameGap())
                .setDefaultValue(defaults.nameGap())
                .setMin(0).setMax(32)
                .setSaveConsumer(draft::setHeldItemNameGap).build());
        category.addEntry(entries.startIntField(Component.translatable("option.ecclientsettings.held_item_info.verticalOffset"), current.verticalOffset())
                .setDefaultValue(defaults.verticalOffset())
                .setMin(-200).setMax(200)
                .setSaveConsumer(draft::setHeldItemVerticalOffset).build());
        category.addEntry(entries.startDoubleField(Component.translatable("option.ecclientsettings.held_item_info.baseSeconds"), current.baseSeconds())
                .setDefaultValue(defaults.baseSeconds())
                .setMin(0.5).setMax(30.0)
                .setSaveConsumer(draft::setHeldItemBaseSeconds).build());
        category.addEntry(entries.startDoubleField(Component.translatable("option.ecclientsettings.held_item_info.extraLineSeconds"), current.extraLineSeconds())
                .setDefaultValue(defaults.extraLineSeconds())
                .setMin(0.0).setMax(5.0)
                .setSaveConsumer(draft::setHeldItemExtraLineSeconds).build());
        category.addEntry(entries.startEnumSelector(Component.translatable("option.ecclientsettings.held_item_info.background"), HeldItemBackground.class, current.background())
                .setDefaultValue(defaults.background())
                .setEnumNameProvider(value -> Component.translatable("option.ecclientsettings.held_item_info.background." + value.name().toLowerCase(Locale.ROOT)))
                .setSaveConsumer(draft::setHeldItemBackground).build());
        category.addEntry(entries.startAlphaColorField(Component.translatable("option.ecclientsettings.held_item_info.backgroundColor"), current.backgroundColor().value())
                .setDefaultValue(defaults.backgroundColor().value())
                .setSaveConsumer(draft::setHeldItemBackgroundColor).build());
        category.addEntry(entries.startBooleanToggle(Component.translatable("option.ecclientsettings.held_item_info.chroma"), current.chroma())
                .setDefaultValue(defaults.chroma())
                .setSaveConsumer(draft::setHeldItemChroma).build());
        category.addEntry(entries.startDoubleField(Component.translatable("option.ecclientsettings.held_item_info.chromaSpeed"), current.chromaSpeed())
                .setDefaultValue(defaults.chromaSpeed())
                .setMin(0.01).setMax(2.0)
                .setSaveConsumer(draft::setHeldItemChromaSpeed).build());
        category.addEntry(entries.startDoubleField(Component.translatable("option.ecclientsettings.held_item_info.chromaSaturation"), current.chromaSaturation())
                .setDefaultValue(defaults.chromaSaturation())
                .setMin(0.0).setMax(1.0)
                .setSaveConsumer(draft::setHeldItemChromaSaturation).build());
        category.addEntry(entries.startDoubleField(Component.translatable("option.ecclientsettings.held_item_info.chromaBrightness"), current.chromaBrightness())
                .setDefaultValue(defaults.chromaBrightness())
                .setMin(0.0).setMax(1.0)
                .setSaveConsumer(draft::setHeldItemChromaBrightness).build());
        category.addEntry(entries.startDoubleField(Component.translatable("option.ecclientsettings.held_item_info.chromaOpacity"), current.chromaOpacity())
                .setDefaultValue(defaults.chromaOpacity())
                .setMin(0.0).setMax(1.0)
                .setSaveConsumer(draft::setHeldItemChromaOpacity).build());
    }
}
