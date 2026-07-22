package net.easecation.clientsettings.feature.hud;

import net.easecation.clientsettings.ECClientSettings;
import net.easecation.clientsettings.client.HudEditorScreen;
import net.easecation.clientsettings.profile.model.HudSettings;
import net.easecation.clientsettings.profile.model.HudWidgetId;
import net.easecation.clientsettings.profile.model.HudWidgetSettings;
import net.easecation.clientsettings.profile.model.HudWidgetStyle;
import net.easecation.clientsettings.profile.runtime.ProfileServices;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public final class HudRenderer {

    private static final ResourceLocation LAYER_ID =
            ResourceLocation.fromNamespaceAndPath(ECClientSettings.MOD_ID, "profile_hud");

    private HudRenderer() {
    }

    public static void register(RegisterGuiLayersEvent event) {
        event.registerBelow(VanillaGuiLayers.CHAT, LAYER_ID, HudRenderer::renderLayer);
    }

    public static HudSize previewSize(HudWidgetId id) {
        return previewSize(id, HudSettings.DEFAULT);
    }

    public static HudSize previewSize(HudWidgetId id, HudSettings settings) {
        HudWidgetSettings widgetSettings = settings.widget(id);
        return HudPanel.outerSize(
                HudWidgetRegistry.widget(id).previewSize(settings),
                widgetSettings.style()
        );
    }

    public static HudBounds previewBounds(
            HudWidgetId id,
            HudSettings settings,
            int viewportWidth,
            int viewportHeight
    ) {
        return HudLayout.bounds(
                settings.widget(id),
                viewportWidth,
                viewportHeight,
                previewSize(id, settings)
        );
    }

    public static HudBounds renderPreview(
            GuiGraphics graphics,
            HudWidgetId id,
            HudSettings settings
    ) {
        Objects.requireNonNull(graphics, "graphics");
        HudRenderContext context = new HudRenderContext(
                Minecraft.getInstance(), graphics, HudRenderMode.PREVIEW, 0.0F, settings
        );
        return renderWidget(
                context,
                HudWidgetRegistry.widget(id),
                settings.widget(id),
                graphics.guiWidth(),
                graphics.guiHeight()
        );
    }

    public static Map<HudWidgetId, HudBounds> renderPreviews(GuiGraphics graphics, HudSettings settings) {
        Objects.requireNonNull(settings, "settings");
        EnumMap<HudWidgetId, HudBounds> bounds = new EnumMap<>(HudWidgetId.class);
        for (HudWidget widget : HudWidgetRegistry.widgets()) {
            bounds.put(widget.id(), renderPreview(graphics, widget.id(), settings));
        }
        return Collections.unmodifiableMap(bounds);
    }

    public static HudSize renderPreviewAt(
            GuiGraphics graphics,
            HudWidgetId id,
            HudSettings settings,
            int x,
            int y,
            double scale
    ) {
        if (!Double.isFinite(scale) || scale <= 0.0) {
            throw new IllegalArgumentException("preview scale must be finite and positive");
        }
        HudWidget widget = HudWidgetRegistry.widget(id);
        HudWidgetSettings widgetSettings = settings.widget(id);
        HudRenderContext context = new HudRenderContext(
                Minecraft.getInstance(), graphics, HudRenderMode.PREVIEW, 0.0F, settings
        ).withStyle(widgetSettings.style());
        HudSize contentSize = widget.previewSize(settings);
        HudSize outerSize = HudPanel.outerSize(contentSize, widgetSettings.style());
        graphics.pose().pushMatrix();
        try {
            graphics.pose().translate(x, y);
            graphics.pose().scale((float) scale, (float) scale);
            HudPanel.draw(graphics, outerSize, widgetSettings.style());
            int contentInset = HudPanel.contentInset(widgetSettings.style());
            graphics.pose().translate(contentInset, contentInset);
            widget.render(context, contentSize);
        } finally {
            graphics.pose().popMatrix();
        }
        return outerSize;
    }

    private static void renderLayer(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui
                || minecraft.player == null
                || minecraft.level == null
                || minecraft.screen instanceof HudEditorScreen) {
            return;
        }

        HudSettings settings = ProfileServices.active().features().hud();
        HudRenderContext context = new HudRenderContext(
                minecraft,
                graphics,
                HudRenderMode.LIVE,
                deltaTracker.getGameTimeDeltaPartialTick(false),
                settings
        );
        for (HudWidget widget : HudWidgetRegistry.widgets()) {
            HudWidgetSettings widgetSettings = settings.widget(widget.id());
            if (widgetSettings.enabled() && widget.shouldRender(context)) {
                renderWidget(context, widget, widgetSettings, graphics.guiWidth(), graphics.guiHeight());
            }
        }
    }

    private static HudBounds renderWidget(
            HudRenderContext context,
            HudWidget widget,
            HudWidgetSettings settings,
            int viewportWidth,
            int viewportHeight
    ) {
        HudRenderContext widgetContext = context.withStyle(settings.style());
        HudSize contentSize = context.preview()
                ? widget.previewSize(context.hudSettings())
                : widget.measure(widgetContext);
        HudSize outerSize = HudPanel.outerSize(contentSize, settings.style());
        HudBounds bounds = HudLayout.bounds(settings, viewportWidth, viewportHeight, outerSize);
        double scale = HudLayout.fittedScale(settings.scale(), viewportWidth, viewportHeight, outerSize);
        if (bounds.width() == 0 || bounds.height() == 0 || scale == 0.0) {
            return bounds;
        }

        context.graphics().pose().pushMatrix();
        try {
            context.graphics().pose().translate(bounds.x(), bounds.y());
            context.graphics().pose().scale((float) scale, (float) scale);
            HudPanel.draw(context.graphics(), outerSize, settings.style());
            int contentInset = HudPanel.contentInset(settings.style());
            context.graphics().pose().translate(contentInset, contentInset);
            widget.render(widgetContext, contentSize);
        } finally {
            context.graphics().pose().popMatrix();
        }
        return bounds;
    }
}
