package net.easecation.clientsettings;

import net.easecation.clientsettings.client.ClientSettingsEvents;
import net.easecation.clientsettings.client.ClientSettingsKeyMappings;
import net.easecation.clientsettings.client.input.ClientInputDispatcher;
import net.easecation.clientsettings.config.ClientSettingsConfig;
import net.easecation.clientsettings.config.ObsOverlayConfig;
import net.easecation.clientsettings.feature.blockoutline.BlockOutlineRenderer;
import net.easecation.clientsettings.feature.hud.HudRenderer;
import net.easecation.clientsettings.feature.hud.keystrokes.KeystrokesInputTracker;
import net.easecation.clientsettings.feature.hitcolor.HitColorRuntime;
import net.easecation.clientsettings.feature.obsoverlay.ObsOverlayGuiLayer;
import net.easecation.clientsettings.feature.obsoverlay.ObsOverlayRuntime;
import net.easecation.clientsettings.feature.timechanger.TimeChangerRuntime;
import net.easecation.clientsettings.feature.zoom.ZoomEvents;
import net.easecation.clientsettings.window.WindowAppearanceEvents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(value = ECClientSettings.MOD_ID, dist = Dist.CLIENT)
public final class ECClientSettings {

    public static final String MOD_ID = "ecclientsettings";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public ECClientSettings(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.CLIENT, ClientSettingsConfig.SPEC);
        modContainer.registerConfig(ModConfig.Type.CLIENT, ObsOverlayConfig.SPEC, ObsOverlayConfig.FILE_NAME);
        modEventBus.addListener(ClientSettingsKeyMappings::register);
        modEventBus.addListener(HudRenderer::register);
        modEventBus.addListener(ObsOverlayGuiLayer::register);

        NeoForge.EVENT_BUS.addListener(ClientInputDispatcher::onClientTick);
        NeoForge.EVENT_BUS.addListener(TimeChangerRuntime::onClientTick);
        NeoForge.EVENT_BUS.addListener(TimeChangerRuntime::onLoggingOut);
        NeoForge.EVENT_BUS.addListener(ZoomEvents::onClientTick);
        NeoForge.EVENT_BUS.addListener(ZoomEvents::onComputeFov);
        NeoForge.EVENT_BUS.addListener(ZoomEvents::onCalculatePlayerTurn);
        NeoForge.EVENT_BUS.addListener(ZoomEvents::onMouseScroll);
        NeoForge.EVENT_BUS.addListener(ZoomEvents::onLoggingOut);
        NeoForge.EVENT_BUS.addListener(ZoomEvents::onClone);
        NeoForge.EVENT_BUS.addListener(BlockOutlineRenderer::onRenderHighlight);
        NeoForge.EVENT_BUS.addListener(HitColorRuntime::onClientTick);
        NeoForge.EVENT_BUS.addListener(HitColorRuntime::onClientStopping);
        NeoForge.EVENT_BUS.addListener(KeystrokesInputTracker::onMouseButton);
        NeoForge.EVENT_BUS.addListener(KeystrokesInputTracker::onLoggingOut);
        NeoForge.EVENT_BUS.addListener(KeystrokesInputTracker::onClientStopping);
        NeoForge.EVENT_BUS.addListener(ClientSettingsEvents::onScreenKeyPressed);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, ClientSettingsEvents::onScreenInit);
        NeoForge.EVENT_BUS.addListener(WindowAppearanceEvents::onModEvent);
        NeoForge.EVENT_BUS.addListener(WindowAppearanceEvents::onDisconnected);
        NeoForge.EVENT_BUS.addListener(ObsOverlayRuntime::onLoggingOut);
        NeoForge.EVENT_BUS.addListener(ObsOverlayRuntime::onClientStopping);

        LOGGER.info("EaseCation client settings initialized");
    }
}
