package net.easecation.clientsettings.profile.model;

public record HeldItemInfoSettings(
        boolean enabled,
        boolean showName,
        boolean showDescription,
        boolean showEnchantments,
        boolean showAdditional,
        boolean showOmitted,
        int maxCharacters,
        int maxLines,
        int maxDescriptionLines,
        int lineSpacing,
        int nameGap,
        int verticalOffset,
        double baseSeconds,
        double extraLineSeconds,
        HeldItemBackground background,
        ArgbColor backgroundColor,
        boolean chroma,
        double chromaSpeed,
        double chromaSaturation,
        double chromaBrightness,
        double chromaOpacity
) {
    public static final HeldItemInfoSettings DEFAULT = new HeldItemInfoSettings(
            false, true, true, true, true, true, 30, 8, 3, 10, 2, 0, 2.0, 0.5, HeldItemBackground.VANILLA, ArgbColor.parse("#B0100010"), false, 0.05, 1.0, 1.0, 0.4
    );

    public HeldItemInfoSettings {
        if (maxCharacters < 1 || maxCharacters > 256) throw new IllegalArgumentException("heldItemInfo.maxCharacters out of range");
        if (maxLines < 1 || maxLines > 32) throw new IllegalArgumentException("heldItemInfo.maxLines out of range");
        if (maxDescriptionLines < 0 || maxDescriptionLines > 32) throw new IllegalArgumentException("heldItemInfo.maxDescriptionLines out of range");
        if (lineSpacing < 9 || lineSpacing > 32) throw new IllegalArgumentException("heldItemInfo.lineSpacing out of range");
        if (nameGap < 0 || nameGap > 32) throw new IllegalArgumentException("heldItemInfo.nameGap out of range");
        if (verticalOffset < -200 || verticalOffset > 200) throw new IllegalArgumentException("heldItemInfo.verticalOffset out of range");
        baseSeconds = ProfileValidation.requireRange(baseSeconds, 0.5, 30, "heldItemInfo.baseSeconds");
        extraLineSeconds = ProfileValidation.requireRange(extraLineSeconds, 0, 5, "heldItemInfo.extraLineSeconds");
        background = ProfileValidation.requireNonNull(background, "heldItemInfo.background");
        backgroundColor = ProfileValidation.requireNonNull(backgroundColor, "heldItemInfo.backgroundColor");
        chromaSpeed = ProfileValidation.requireRange(chromaSpeed, 0.01, 2, "heldItemInfo.chromaSpeed");
        chromaSaturation = ProfileValidation.requireRange(chromaSaturation, 0, 1, "heldItemInfo.chromaSaturation");
        chromaBrightness = ProfileValidation.requireRange(chromaBrightness, 0, 1, "heldItemInfo.chromaBrightness");
        chromaOpacity = ProfileValidation.requireRange(chromaOpacity, 0, 1, "heldItemInfo.chromaOpacity");
    }

    public HeldItemInfoSettings withEnabled(boolean value) {
        return new HeldItemInfoSettings(value, showName, showDescription, showEnchantments, showAdditional, showOmitted, maxCharacters, maxLines, maxDescriptionLines, lineSpacing, nameGap, verticalOffset, baseSeconds, extraLineSeconds, background, backgroundColor, chroma, chromaSpeed, chromaSaturation, chromaBrightness, chromaOpacity);
    }

    public HeldItemInfoSettings withShowName(boolean value) {
        return new HeldItemInfoSettings(enabled, value, showDescription, showEnchantments, showAdditional, showOmitted, maxCharacters, maxLines, maxDescriptionLines, lineSpacing, nameGap, verticalOffset, baseSeconds, extraLineSeconds, background, backgroundColor, chroma, chromaSpeed, chromaSaturation, chromaBrightness, chromaOpacity);
    }

    public HeldItemInfoSettings withShowDescription(boolean value) {
        return new HeldItemInfoSettings(enabled, showName, value, showEnchantments, showAdditional, showOmitted, maxCharacters, maxLines, maxDescriptionLines, lineSpacing, nameGap, verticalOffset, baseSeconds, extraLineSeconds, background, backgroundColor, chroma, chromaSpeed, chromaSaturation, chromaBrightness, chromaOpacity);
    }

    public HeldItemInfoSettings withShowEnchantments(boolean value) {
        return new HeldItemInfoSettings(enabled, showName, showDescription, value, showAdditional, showOmitted, maxCharacters, maxLines, maxDescriptionLines, lineSpacing, nameGap, verticalOffset, baseSeconds, extraLineSeconds, background, backgroundColor, chroma, chromaSpeed, chromaSaturation, chromaBrightness, chromaOpacity);
    }

    public HeldItemInfoSettings withShowAdditional(boolean value) {
        return new HeldItemInfoSettings(enabled, showName, showDescription, showEnchantments, value, showOmitted, maxCharacters, maxLines, maxDescriptionLines, lineSpacing, nameGap, verticalOffset, baseSeconds, extraLineSeconds, background, backgroundColor, chroma, chromaSpeed, chromaSaturation, chromaBrightness, chromaOpacity);
    }

    public HeldItemInfoSettings withShowOmitted(boolean value) {
        return new HeldItemInfoSettings(enabled, showName, showDescription, showEnchantments, showAdditional, value, maxCharacters, maxLines, maxDescriptionLines, lineSpacing, nameGap, verticalOffset, baseSeconds, extraLineSeconds, background, backgroundColor, chroma, chromaSpeed, chromaSaturation, chromaBrightness, chromaOpacity);
    }

    public HeldItemInfoSettings withMaxCharacters(int value) {
        return new HeldItemInfoSettings(enabled, showName, showDescription, showEnchantments, showAdditional, showOmitted, value, maxLines, maxDescriptionLines, lineSpacing, nameGap, verticalOffset, baseSeconds, extraLineSeconds, background, backgroundColor, chroma, chromaSpeed, chromaSaturation, chromaBrightness, chromaOpacity);
    }

    public HeldItemInfoSettings withMaxLines(int value) {
        return new HeldItemInfoSettings(enabled, showName, showDescription, showEnchantments, showAdditional, showOmitted, maxCharacters, value, maxDescriptionLines, lineSpacing, nameGap, verticalOffset, baseSeconds, extraLineSeconds, background, backgroundColor, chroma, chromaSpeed, chromaSaturation, chromaBrightness, chromaOpacity);
    }

    public HeldItemInfoSettings withMaxDescriptionLines(int value) {
        return new HeldItemInfoSettings(enabled, showName, showDescription, showEnchantments, showAdditional, showOmitted, maxCharacters, maxLines, value, lineSpacing, nameGap, verticalOffset, baseSeconds, extraLineSeconds, background, backgroundColor, chroma, chromaSpeed, chromaSaturation, chromaBrightness, chromaOpacity);
    }

    public HeldItemInfoSettings withLineSpacing(int value) {
        return new HeldItemInfoSettings(enabled, showName, showDescription, showEnchantments, showAdditional, showOmitted, maxCharacters, maxLines, maxDescriptionLines, value, nameGap, verticalOffset, baseSeconds, extraLineSeconds, background, backgroundColor, chroma, chromaSpeed, chromaSaturation, chromaBrightness, chromaOpacity);
    }

    public HeldItemInfoSettings withNameGap(int value) {
        return new HeldItemInfoSettings(enabled, showName, showDescription, showEnchantments, showAdditional, showOmitted, maxCharacters, maxLines, maxDescriptionLines, lineSpacing, value, verticalOffset, baseSeconds, extraLineSeconds, background, backgroundColor, chroma, chromaSpeed, chromaSaturation, chromaBrightness, chromaOpacity);
    }

    public HeldItemInfoSettings withVerticalOffset(int value) {
        return new HeldItemInfoSettings(enabled, showName, showDescription, showEnchantments, showAdditional, showOmitted, maxCharacters, maxLines, maxDescriptionLines, lineSpacing, nameGap, value, baseSeconds, extraLineSeconds, background, backgroundColor, chroma, chromaSpeed, chromaSaturation, chromaBrightness, chromaOpacity);
    }

    public HeldItemInfoSettings withBaseSeconds(double value) {
        return new HeldItemInfoSettings(enabled, showName, showDescription, showEnchantments, showAdditional, showOmitted, maxCharacters, maxLines, maxDescriptionLines, lineSpacing, nameGap, verticalOffset, value, extraLineSeconds, background, backgroundColor, chroma, chromaSpeed, chromaSaturation, chromaBrightness, chromaOpacity);
    }

    public HeldItemInfoSettings withExtraLineSeconds(double value) {
        return new HeldItemInfoSettings(enabled, showName, showDescription, showEnchantments, showAdditional, showOmitted, maxCharacters, maxLines, maxDescriptionLines, lineSpacing, nameGap, verticalOffset, baseSeconds, value, background, backgroundColor, chroma, chromaSpeed, chromaSaturation, chromaBrightness, chromaOpacity);
    }

    public HeldItemInfoSettings withBackground(HeldItemBackground value) {
        return new HeldItemInfoSettings(enabled, showName, showDescription, showEnchantments, showAdditional, showOmitted, maxCharacters, maxLines, maxDescriptionLines, lineSpacing, nameGap, verticalOffset, baseSeconds, extraLineSeconds, value, backgroundColor, chroma, chromaSpeed, chromaSaturation, chromaBrightness, chromaOpacity);
    }

    public HeldItemInfoSettings withBackgroundColor(ArgbColor value) {
        return new HeldItemInfoSettings(enabled, showName, showDescription, showEnchantments, showAdditional, showOmitted, maxCharacters, maxLines, maxDescriptionLines, lineSpacing, nameGap, verticalOffset, baseSeconds, extraLineSeconds, background, value, chroma, chromaSpeed, chromaSaturation, chromaBrightness, chromaOpacity);
    }

    public HeldItemInfoSettings withChroma(boolean value) {
        return new HeldItemInfoSettings(enabled, showName, showDescription, showEnchantments, showAdditional, showOmitted, maxCharacters, maxLines, maxDescriptionLines, lineSpacing, nameGap, verticalOffset, baseSeconds, extraLineSeconds, background, backgroundColor, value, chromaSpeed, chromaSaturation, chromaBrightness, chromaOpacity);
    }

    public HeldItemInfoSettings withChromaSpeed(double value) {
        return new HeldItemInfoSettings(enabled, showName, showDescription, showEnchantments, showAdditional, showOmitted, maxCharacters, maxLines, maxDescriptionLines, lineSpacing, nameGap, verticalOffset, baseSeconds, extraLineSeconds, background, backgroundColor, chroma, value, chromaSaturation, chromaBrightness, chromaOpacity);
    }

    public HeldItemInfoSettings withChromaSaturation(double value) {
        return new HeldItemInfoSettings(enabled, showName, showDescription, showEnchantments, showAdditional, showOmitted, maxCharacters, maxLines, maxDescriptionLines, lineSpacing, nameGap, verticalOffset, baseSeconds, extraLineSeconds, background, backgroundColor, chroma, chromaSpeed, value, chromaBrightness, chromaOpacity);
    }

    public HeldItemInfoSettings withChromaBrightness(double value) {
        return new HeldItemInfoSettings(enabled, showName, showDescription, showEnchantments, showAdditional, showOmitted, maxCharacters, maxLines, maxDescriptionLines, lineSpacing, nameGap, verticalOffset, baseSeconds, extraLineSeconds, background, backgroundColor, chroma, chromaSpeed, chromaSaturation, value, chromaOpacity);
    }

    public HeldItemInfoSettings withChromaOpacity(double value) {
        return new HeldItemInfoSettings(enabled, showName, showDescription, showEnchantments, showAdditional, showOmitted, maxCharacters, maxLines, maxDescriptionLines, lineSpacing, nameGap, verticalOffset, baseSeconds, extraLineSeconds, background, backgroundColor, chroma, chromaSpeed, chromaSaturation, chromaBrightness, value);
    }
}
