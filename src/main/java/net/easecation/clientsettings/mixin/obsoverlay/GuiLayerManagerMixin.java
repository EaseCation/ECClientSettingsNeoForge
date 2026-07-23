package net.easecation.clientsettings.mixin.obsoverlay;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import net.easecation.clientsettings.feature.obsoverlay.ObsOverlayComponent;
import net.easecation.clientsettings.feature.obsoverlay.ObsOverlayRuntime;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.GuiLayerManager;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GuiLayerManager.class)
abstract class GuiLayerManagerMixin {

    @Unique
    private static final ResourceLocation EC_HUD_LAYER =
            ResourceLocation.fromNamespaceAndPath("ecclientsettings", "profile_hud");

    @Unique
    private int ecclientsettings$openSensitiveLayerScopes;

    @WrapMethod(method = "renderInner")
    private void ecclientsettings$closeAbandonedLayerScopes(
            GuiGraphics graphics,
            DeltaTracker deltaTracker,
            Operation<Void> original
    ) {
        int baseline = ecclientsettings$openSensitiveLayerScopes;
        try {
            original.call(graphics, deltaTracker);
        } finally {
            while (ecclientsettings$openSensitiveLayerScopes > baseline) {
                ecclientsettings$closeSensitiveLayerScope();
            }
        }
    }

    @WrapOperation(
            method = "renderInner",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/neoforged/bus/api/IEventBus;post(Lnet/neoforged/bus/api/Event;)Lnet/neoforged/bus/api/Event;"
            )
    )
    private Event ecclientsettings$routeSensitiveLayerEvents(
            IEventBus eventBus,
            Event event,
            Operation<Event> original
    ) {
        if (event instanceof RenderGuiLayerEvent.Pre pre) {
            return beginSensitiveLayerEvent(eventBus, pre, original);
        }
        if (event instanceof RenderGuiLayerEvent.Post post) {
            return endSensitiveLayerEvent(eventBus, post, original);
        }
        return original.call(eventBus, event);
    }

    private Event beginSensitiveLayerEvent(
            IEventBus eventBus,
            RenderGuiLayerEvent.Pre event,
            Operation<Event> original
    ) {
        ObsOverlayComponent component = componentForName(event.getName());
        if (component == null) {
            return original.call(eventBus, event);
        }
        ObsOverlayRuntime.beginComponent(component);
        ecclientsettings$openSensitiveLayerScopes++;
        boolean continueRendering = false;
        try {
            Event posted = original.call(eventBus, event);
            continueRendering = posted instanceof RenderGuiLayerEvent.Pre postedPre
                    && !postedPre.isCanceled();
            return posted;
        } finally {
            if (!continueRendering) {
                ecclientsettings$closeSensitiveLayerScope();
            }
        }
    }

    private Event endSensitiveLayerEvent(
            IEventBus eventBus,
            RenderGuiLayerEvent.Post event,
            Operation<Event> original
    ) {
        if (componentForName(event.getName()) == null) {
            return original.call(eventBus, event);
        }
        try {
            return original.call(eventBus, event);
        } finally {
            ecclientsettings$closeSensitiveLayerScope();
        }
    }

    @Unique
    private void ecclientsettings$closeSensitiveLayerScope() {
        if (ecclientsettings$openSensitiveLayerScopes > 0) {
            ecclientsettings$openSensitiveLayerScopes--;
            ObsOverlayRuntime.endComponent();
        }
    }

    private static ObsOverlayComponent componentForName(ResourceLocation name) {
        if (name.equals(VanillaGuiLayers.DEBUG_OVERLAY)) {
            return ObsOverlayComponent.DEBUG_MENU;
        }
        if (name.equals(VanillaGuiLayers.CHAT)) {
            return ObsOverlayComponent.CHAT;
        }
        if (name.equals(EC_HUD_LAYER)) {
            return ObsOverlayComponent.EC_HUD;
        }
        if (name.equals(VanillaGuiLayers.TAB_LIST)) {
            return ObsOverlayComponent.PLAYER_LIST;
        }
        if (name.equals(VanillaGuiLayers.SUBTITLE_OVERLAY)) {
            return ObsOverlayComponent.SUBTITLES;
        }
        if (name.equals(VanillaGuiLayers.SCOREBOARD_SIDEBAR)) {
            return ObsOverlayComponent.SCOREBOARD;
        }
        if (name.equals(VanillaGuiLayers.OVERLAY_MESSAGE)) {
            return ObsOverlayComponent.ACTION_BAR;
        }
        if (name.equals(VanillaGuiLayers.TITLE)) {
            return ObsOverlayComponent.TITLE;
        }
        if (name.equals(VanillaGuiLayers.EFFECTS)) {
            return ObsOverlayComponent.STATUS_EFFECTS;
        }
        if (isMainHud(name)) {
            return ObsOverlayComponent.MAIN_HUD;
        }
        return null;
    }

    private static boolean isMainHud(ResourceLocation name) {
        return name.equals(VanillaGuiLayers.HOTBAR)
                || name.equals(VanillaGuiLayers.PLAYER_HEALTH)
                || name.equals(VanillaGuiLayers.ARMOR_LEVEL)
                || name.equals(VanillaGuiLayers.FOOD_LEVEL)
                || name.equals(VanillaGuiLayers.VEHICLE_HEALTH)
                || name.equals(VanillaGuiLayers.AIR_LEVEL)
                || name.equals(VanillaGuiLayers.CONTEXTUAL_INFO_BAR_BACKGROUND)
                || name.equals(VanillaGuiLayers.EXPERIENCE_LEVEL)
                || name.equals(VanillaGuiLayers.CONTEXTUAL_INFO_BAR)
                || name.equals(VanillaGuiLayers.SELECTED_ITEM_NAME)
                || name.equals(VanillaGuiLayers.SPECTATOR_TOOLTIP);
    }
}
