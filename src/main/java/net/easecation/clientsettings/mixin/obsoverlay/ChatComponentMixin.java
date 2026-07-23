package net.easecation.clientsettings.mixin.obsoverlay;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.easecation.clientsettings.feature.obsoverlay.ObsOverlayComponent;
import net.easecation.clientsettings.feature.obsoverlay.ObsOverlayRuntime;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ChatComponent.class)
abstract class ChatComponentMixin {

    @WrapMethod(method = "render")
    private void ecclientsettings$protectChat(
            GuiGraphics graphics,
            int ticks,
            int mouseX,
            int mouseY,
            boolean focused,
            Operation<Void> original
    ) {
        ObsOverlayRuntime.beginComponent(ObsOverlayComponent.CHAT);
        try {
            original.call(graphics, ticks, mouseX, mouseY, focused);
        } finally {
            ObsOverlayRuntime.endComponent();
        }
    }
}
