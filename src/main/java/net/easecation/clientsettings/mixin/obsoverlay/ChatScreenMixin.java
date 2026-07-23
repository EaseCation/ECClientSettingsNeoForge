package net.easecation.clientsettings.mixin.obsoverlay;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.easecation.clientsettings.feature.obsoverlay.ObsOverlayComponent;
import net.easecation.clientsettings.feature.obsoverlay.ObsOverlayRuntime;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ChatScreen.class)
abstract class ChatScreenMixin {

    @WrapMethod(method = "render")
    private void ecclientsettings$protectChatInput(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick,
            Operation<Void> original
    ) {
        ObsOverlayRuntime.beginComponent(ObsOverlayComponent.CHAT_INPUT);
        try {
            original.call(graphics, mouseX, mouseY, partialTick);
        } finally {
            ObsOverlayRuntime.endComponent();
        }
    }
}
