package net.easecation.clientsettings.client;

import me.shedaniel.clothconfig2.gui.entries.TooltipListEntry;
import net.easecation.clientsettings.config.ObsOverlayConfig;
import net.easecation.clientsettings.feature.obsoverlay.ObsOverlayHookStatus;
import net.easecation.clientsettings.feature.obsoverlay.ObsOverlayRuntime;
import net.easecation.clientsettings.feature.obsoverlay.ObsOverlaySettings;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.fml.ModList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Live status avoids presenting a stale privacy result while the settings screen stays open. */
@SuppressWarnings("deprecation")
final class ObsOverlayStatusEntry extends TooltipListEntry<Object> {

    private static final int LINE_HEIGHT = 12;

    private List<FormattedCharSequence> wrappedLines = List.of();
    private int savedWidth = -1;
    private int savedContentHash;

    ObsOverlayStatusEntry() {
        super(
                Component.translatable("option.ecclientsettings.obs_overlay.status"),
                () -> Optional.empty()
        );
    }

    @Override
    public void render(
            GuiGraphics graphics,
            int index,
            int y,
            int x,
            int entryWidth,
            int entryHeight,
            int mouseX,
            int mouseY,
            boolean hovered,
            float partialTick
    ) {
        super.render(graphics, index, y, x, entryWidth, entryHeight, mouseX, mouseY, hovered, partialTick);
        Font font = Minecraft.getInstance().font;
        List<Component> content = currentContent();
        int contentHash = content.hashCode();
        if (savedWidth != entryWidth || savedContentHash != contentHash) {
            List<FormattedCharSequence> updated = new ArrayList<>();
            for (Component line : content) {
                updated.addAll(font.split(line, entryWidth));
            }
            wrappedLines = List.copyOf(updated);
            savedWidth = entryWidth;
            savedContentHash = contentHash;
        }

        int lineY = y + 7;
        for (FormattedCharSequence line : wrappedLines) {
            graphics.drawString(font, line, x, lineY, 0xFFFFFFFF);
            lineY += LINE_HEIGHT;
        }
    }

    @Override
    public int getItemHeight() {
        return savedWidth < 0 ? 84 : 14 + wrappedLines.size() * LINE_HEIGHT;
    }

    @Override
    public Object getValue() {
        return null;
    }

    @Override
    public Optional<Object> getDefaultValue() {
        return Optional.empty();
    }

    @Override
    public List<? extends GuiEventListener> children() {
        return Collections.emptyList();
    }

    @Override
    public List<? extends NarratableEntry> narratables() {
        return Collections.emptyList();
    }

    private static List<Component> currentContent() {
        ObsOverlayHookStatus status = ObsOverlayRuntime.status();
        ObsOverlaySettings settings = ObsOverlayConfig.current();
        ChatFormatting statusColor = switch (status) {
            case READY -> ChatFormatting.GREEN;
            case NOT_INITIALIZED, STOPPED -> ChatFormatting.YELLOW;
            case UNSAFE_CAPTURE_ORDER, UNSUPPORTED, FAILED -> ChatFormatting.RED;
        };
        Component statusValue = Component.translatable(
                "option.ecclientsettings.obs_overlay.status." + status.name().toLowerCase(Locale.ROOT)
        ).withStyle(statusColor);

        List<Component> content = new ArrayList<>();
        content.add(Component.translatable("option.ecclientsettings.obs_overlay.status", statusValue));
        content.add(Component.translatable(
                "option.ecclientsettings.obs_overlay.saved_configuration",
                yesNo(settings.enabled()),
                yesNo(settings.showTestMarker())
        ));
        if (!settings.enabled()) {
            content.add(Component.translatable(
                    "option.ecclientsettings.obs_overlay.configuration_inactive"
            ).withStyle(ChatFormatting.RED));
        } else if (!settings.showTestMarker()) {
            content.add(Component.translatable(
                    "option.ecclientsettings.obs_overlay.marker_disabled_warning"
            ).withStyle(ChatFormatting.GOLD));
        }
        content.add(Component.translatable(
                "option.ecclientsettings.obs_overlay.obs_detected",
                yesNo(ObsOverlayRuntime.obsGameCaptureDetected())
        ));
        content.add(Component.translatable(
                "option.ecclientsettings.obs_overlay.render_mods",
                yesNo(ModList.get().isLoaded("sodium")),
                yesNo(ModList.get().isLoaded("iris")),
                yesNo(ModList.get().isLoaded("immediatelyfast"))
        ));

        String failureDetail = ObsOverlayRuntime.failureDetail();
        if (!failureDetail.isBlank()) {
            content.add(Component.translatable(
                    "option.ecclientsettings.obs_overlay.failure_detail",
                    failureDetail
            ).withStyle(ChatFormatting.RED));
        }
        if (status == ObsOverlayHookStatus.UNSAFE_CAPTURE_ORDER) {
            content.add(Component.translatable(
                    "option.ecclientsettings.obs_overlay.unsafe_order_warning"
            ).withStyle(ChatFormatting.RED));
        }
        if (status == ObsOverlayHookStatus.FAILED) {
            content.add(Component.translatable(
                    "option.ecclientsettings.obs_overlay.failed_recovery"
            ).withStyle(ChatFormatting.RED));
        }
        if (ObsOverlayRuntime.irisWorldCompatibilityUnavailable()) {
            content.add(Component.translatable(
                    "option.ecclientsettings.obs_overlay.iris_world_unavailable"
            ).withStyle(ChatFormatting.RED));
        }
        if (ObsOverlayRuntime.irisShaderPackInUse()) {
            content.add(Component.translatable(
                    settings.failClosed()
                            ? "option.ecclientsettings.obs_overlay.iris_shader_pack_world_disabled"
                            : "option.ecclientsettings.obs_overlay.iris_shader_pack_world_unprotected"
            ).withStyle(settings.failClosed() ? ChatFormatting.GOLD : ChatFormatting.RED));
        }
        return content;
    }

    private static Component yesNo(boolean value) {
        return Component.translatable(value
                        ? "option.ecclientsettings.obs_overlay.yes"
                        : "option.ecclientsettings.obs_overlay.no")
                .withStyle(value ? ChatFormatting.GREEN : ChatFormatting.YELLOW);
    }
}
