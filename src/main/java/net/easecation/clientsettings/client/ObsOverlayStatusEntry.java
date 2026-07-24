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
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
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

    private final boolean technicalDetails;
    private final NarratableEntry narrationEntry = new NarratableEntry() {
        @Override
        public NarrationPriority narrationPriority() {
            if (ObsOverlayStatusEntry.this.isFocused()) {
                return NarrationPriority.FOCUSED;
            }
            return hovered ? NarrationPriority.HOVERED : NarrationPriority.NONE;
        }

        @Override
        public void updateNarration(NarrationElementOutput output) {
            output.add(NarratedElementType.TITLE, currentContent().toArray(Component[]::new));
        }
    };
    private List<FormattedCharSequence> wrappedLines = List.of();
    private int savedWidth = -1;
    private int savedContentHash;
    private boolean hovered;

    ObsOverlayStatusEntry(boolean technicalDetails) {
        super(
                Component.translatable(technicalDetails
                        ? "option.ecclientsettings.obs_overlay.technical_status"
                        : "option.ecclientsettings.obs_overlay.summary"),
                () -> Optional.empty()
        );
        this.technicalDetails = technicalDetails;
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
        this.hovered = hovered;
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
        if (savedWidth < 0) {
            return technicalDetails ? 84 : 72;
        }
        return 14 + wrappedLines.size() * LINE_HEIGHT;
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
        return List.of(narrationEntry);
    }

    private List<Component> currentContent() {
        return technicalDetails ? technicalContent() : summaryContent();
    }

    private static List<Component> summaryContent() {
        ObsOverlayHookStatus status = ObsOverlayRuntime.status();
        ObsOverlaySettings settings = ObsOverlayConfig.current();
        List<Component> content = new ArrayList<>();
        boolean blockingStatus = status == ObsOverlayHookStatus.UNSAFE_CAPTURE_ORDER
                || status == ObsOverlayHookStatus.UNSUPPORTED
                || status == ObsOverlayHookStatus.FAILED;

        if (!settings.enabled() && !blockingStatus) {
            content.add(Component.translatable(
                    "option.ecclientsettings.obs_overlay.summary.current",
                    Component.translatable("option.ecclientsettings.obs_overlay.summary.disabled")
                            .withStyle(ChatFormatting.YELLOW)
            ));
            content.add(Component.translatable(
                    "option.ecclientsettings.obs_overlay.summary.action.enable"
            ).withStyle(ChatFormatting.GRAY));
            return content;
        }

        ChatFormatting statusColor = switch (status) {
            case READY -> ChatFormatting.GOLD;
            case NOT_INITIALIZED, STOPPED -> ChatFormatting.YELLOW;
            case UNSAFE_CAPTURE_ORDER, UNSUPPORTED, FAILED -> ChatFormatting.RED;
        };
        Component statusValue = Component.translatable(
                "option.ecclientsettings.obs_overlay.summary.status." + status.name().toLowerCase(Locale.ROOT)
        ).withStyle(statusColor);
        content.add(Component.translatable(
                "option.ecclientsettings.obs_overlay.summary.current",
                statusValue
        ));
        if (settings.enabled()) {
            content.add(Component.translatable(
                    "option.ecclientsettings.obs_overlay.summary.player_names",
                    Component.translatable(
                            "option.ecclientsettings.obs_overlay.player_name_tags.mode."
                                    + settings.playerNameTagMode().name().toLowerCase(Locale.ROOT)
                    )
            ));
        } else {
            content.add(Component.translatable(
                    "option.ecclientsettings.obs_overlay.summary.master_disabled"
            ).withStyle(ChatFormatting.YELLOW));
        }

        if (status == ObsOverlayHookStatus.READY) {
            if (ObsOverlayRuntime.irisShaderPackInUse() && !settings.failClosed()) {
                content.add(Component.translatable(
                        "option.ecclientsettings.obs_overlay.summary.action.shader_unsafe"
                ).withStyle(ChatFormatting.RED));
            } else if (!settings.failClosed()) {
                content.add(Component.translatable(
                        "option.ecclientsettings.obs_overlay.summary.action.safe_fallback_off"
                ).withStyle(ChatFormatting.RED));
            } else if (ObsOverlayRuntime.irisShaderPackInUse()
                    || ObsOverlayRuntime.irisWorldCompatibilityUnavailable()) {
                content.add(Component.translatable(
                        "option.ecclientsettings.obs_overlay.summary.action.safe_fallback"
                ).withStyle(ChatFormatting.GOLD));
            }

            if (settings.showTestMarker()) {
                content.add(Component.translatable(
                        "option.ecclientsettings.obs_overlay.summary.action.verify"
                ).withStyle(ChatFormatting.GOLD));
            } else {
                content.add(Component.translatable(
                        "option.ecclientsettings.obs_overlay.summary.action.enable_marker"
                ).withStyle(ChatFormatting.GOLD));
            }
        } else {
            String actionKey = switch (status) {
                case NOT_INITIALIZED, STOPPED -> "wait";
                case UNSAFE_CAPTURE_ORDER -> "restart_order";
                case UNSUPPORTED -> "unsupported";
                case FAILED -> "restart_failed";
                case READY -> throw new IllegalStateException("READY handled above");
            };
            content.add(Component.translatable(
                    "option.ecclientsettings.obs_overlay.summary.action." + actionKey
            ).withStyle(statusColor));
        }
        return content;
    }

    private static List<Component> technicalContent() {
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
                yesNo(settings.showTestMarker()),
                Component.translatable(
                        "option.ecclientsettings.obs_overlay.player_name_tags.mode."
                                + settings.playerNameTagMode().name().toLowerCase(Locale.ROOT)
                )
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
