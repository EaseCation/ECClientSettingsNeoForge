package net.easecation.clientsettings;

import net.easecation.clientsettings.client.ClientSettingsEvents;
import net.easecation.clientsettings.client.ClientSettingsKeyMappings;
import net.easecation.clientsettings.config.ClientSettingsConfig;
import net.easecation.clientsettings.window.WindowAppearanceEvents;
import net.neoforged.api.distmarker.Dist;
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
        modEventBus.addListener(ClientSettingsKeyMappings::register);

        NeoForge.EVENT_BUS.addListener(ClientSettingsEvents::onClientTick);
        NeoForge.EVENT_BUS.addListener(ClientSettingsEvents::onScreenKeyPressed);
        NeoForge.EVENT_BUS.addListener(ClientSettingsEvents::onScreenInit);
        NeoForge.EVENT_BUS.addListener(WindowAppearanceEvents::onModEvent);
        NeoForge.EVENT_BUS.addListener(WindowAppearanceEvents::onDisconnected);

        LOGGER.info("EaseCation client settings initialized");
    }
}

