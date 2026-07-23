package net.easecation.clientsettings.mixin.obsoverlay;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.easecation.clientsettings.feature.obsoverlay.ObsOverlayComponent;
import net.easecation.clientsettings.feature.obsoverlay.ObsOverlayRuntime;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayDeque;

@Mixin(EntityRenderer.class)
abstract class EntityRendererMixin {

    @Unique
    private final ArrayDeque<MultiBufferSource> ecclientsettings$nameTagScopes = new ArrayDeque<>();

    @WrapMethod(
            method = "render(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"
    )
    private void ecclientsettings$guardNameTagScopes(
            EntityRenderState state,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            Operation<Void> original
    ) {
        int baseline = ecclientsettings$nameTagScopes.size();
        try {
            original.call(state, poseStack, buffers, packedLight);
        } finally {
            while (ecclientsettings$nameTagScopes.size() > baseline) {
                ecclientsettings$closeNameTagScope();
            }
        }
    }

    @WrapOperation(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/neoforged/bus/api/IEventBus;post(Lnet/neoforged/bus/api/Event;)Lnet/neoforged/bus/api/Event;"
            )
    )
    private Event ecclientsettings$protectNameTagEvent(
            IEventBus eventBus,
            Event event,
            Operation<Event> original
    ) {
        if (!(event instanceof RenderNameTagEvent.DoRender nameTagEvent)) {
            return original.call(eventBus, event);
        }
        ObsOverlayComponent component = componentFor(nameTagEvent.getEntityRenderState());
        if (!ObsOverlayRuntime.allowNameTagRendering(component)
                || !ObsOverlayRuntime.beginWorldComponent(component, nameTagEvent.getMultiBufferSource())) {
            nameTagEvent.setCanceled(true);
            return nameTagEvent;
        }

        ecclientsettings$nameTagScopes.push(nameTagEvent.getMultiBufferSource());
        boolean continueRendering = false;
        try {
            Event posted = original.call(eventBus, event);
            continueRendering = posted instanceof RenderNameTagEvent.DoRender postedNameTag
                    && !postedNameTag.isCanceled();
            return posted;
        } finally {
            if (!continueRendering) {
                ecclientsettings$closeNameTagScope();
            }
        }
    }

    @WrapMethod(
            method = "renderNameTag(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lnet/minecraft/network/chat/Component;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"
    )
    private void ecclientsettings$protectDirectNameTagCall(
            EntityRenderState state,
            Component text,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            Operation<Void> original
    ) {
        if (!ecclientsettings$nameTagScopes.isEmpty()) {
            original.call(state, text, poseStack, buffers, packedLight);
            return;
        }
        ObsOverlayComponent component = componentFor(state);
        if (!ObsOverlayRuntime.allowNameTagRendering(component)
                || !ObsOverlayRuntime.beginWorldComponent(component, buffers)) {
            return;
        }
        try {
            original.call(state, text, poseStack, buffers, packedLight);
        } finally {
            ObsOverlayRuntime.endWorldComponent(buffers);
        }
    }

    @Unique
    private void ecclientsettings$closeNameTagScope() {
        ObsOverlayRuntime.endWorldComponent(ecclientsettings$nameTagScopes.pop());
    }

    private static ObsOverlayComponent componentFor(EntityRenderState state) {
        return state.isDiscrete
                ? ObsOverlayComponent.SNEAKING_NAME_TAGS
                : ObsOverlayComponent.NAME_TAGS;
    }
}
