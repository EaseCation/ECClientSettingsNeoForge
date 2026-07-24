package net.easecation.clientsettings.mixin.obsoverlay;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.easecation.clientsettings.config.ObsOverlayConfig;
import net.easecation.clientsettings.feature.obsoverlay.DeferredNameTagPass;
import net.easecation.clientsettings.feature.obsoverlay.DeferredNameTagPassResult;
import net.easecation.clientsettings.feature.obsoverlay.ObsOverlayRuntime;
import net.easecation.clientsettings.feature.obsoverlay.ObsOverlaySettings;
import net.easecation.clientsettings.feature.obsoverlay.PlayerAliasService;
import net.easecation.clientsettings.feature.obsoverlay.PlayerNameTagMode;
import net.easecation.clientsettings.feature.obsoverlay.PlayerNameTagRenderPlan;
import net.easecation.clientsettings.feature.obsoverlay.PlayerNameTagRenderState;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EntityRenderer.class)
abstract class EntityRendererMixin {

    @ModifyExpressionValue(
            method = "extractRenderState",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;getNameTag(Lnet/minecraft/world/entity/Entity;)Lnet/minecraft/network/chat/Component;"
            )
    )
    private Component ecclientsettings$preparePlayerNameTag(
            @Nullable Component originalName,
            Entity entity,
            EntityRenderState state,
            float partialTick
    ) {
        PlayerNameTagRenderState playerNameState = (PlayerNameTagRenderState) state;
        if (!(entity instanceof Player player)) {
            playerNameState.ecclientsettings$clearPlayerNameTagState();
            return originalName;
        }

        ObsOverlaySettings settings = ObsOverlayConfig.current();
        PlayerNameTagMode mode = settings.effectivePlayerNameTagMode();
        if (mode == PlayerNameTagMode.UNCHANGED) {
            playerNameState.ecclientsettings$clearPlayerNameTagState();
            return originalName;
        }

        Component alias = mode == PlayerNameTagMode.PSEUDONYMIZE
                ? PlayerAliasService.aliasFor(player, settings.playerAliasFormat(), settings.playerAliasColorMode())
                : null;
        playerNameState.ecclientsettings$setPlayerNameTagState(mode, originalName, alias);

        // CanRender listeners must never receive the real name as originalContent in alias mode.
        return alias != null ? alias : originalName;
    }

    @WrapOperation(
            method = "extractRenderState",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/neoforged/bus/api/IEventBus;post(Lnet/neoforged/bus/api/Event;)Lnet/neoforged/bus/api/Event;"
            )
    )
    private Event ecclientsettings$keepAliasAfterCanRenderListeners(
            IEventBus eventBus,
            Event event,
            Operation<Event> original
    ) {
        Event posted = original.call(eventBus, event);
        if (posted instanceof RenderNameTagEvent.CanRender nameTagEvent
                && nameTagEvent.getEntityRenderState() instanceof PlayerNameTagRenderState playerNameState) {
            PlayerNameTagMode mode = playerNameState.ecclientsettings$getPlayerNameTagMode();
            if (mode == PlayerNameTagMode.PSEUDONYMIZE) {
                Component alias = playerNameState.ecclientsettings$getPlayerNameTagAlias();
                if (alias != null) {
                    nameTagEvent.setContent(alias);
                }
            } else if (mode == PlayerNameTagMode.HIDE) {
                // Keep compatible private formatting supplied by CanRender listeners.
                playerNameState.ecclientsettings$setPlayerNameTagState(
                        mode,
                        nameTagEvent.getContent(),
                        null
                );
            }
        }
        return posted;
    }

    @WrapOperation(
            method = "render(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/neoforged/bus/api/IEventBus;post(Lnet/neoforged/bus/api/Event;)Lnet/neoforged/bus/api/Event;"
            )
    )
    private Event ecclientsettings$routeNameTagEvent(
            IEventBus eventBus,
            Event event,
            Operation<Event> original
    ) {
        if (!(event instanceof RenderNameTagEvent.DoRender nameTagEvent)) {
            return original.call(eventBus, event);
        }

        PlayerNameTagRenderPlan plan = ObsOverlayRuntime.playerNameTagRenderPlan(
                nameTagEvent.getEntityRenderState()
        );
        if (plan == PlayerNameTagRenderPlan.SUPPRESS) {
            nameTagEvent.setCanceled(true);
            return nameTagEvent;
        }
        if (plan == PlayerNameTagRenderPlan.CAPTURE_ALIAS_AND_PRIVATE_REAL_NAME
                || plan == PlayerNameTagRenderPlan.ALIAS_EVERYWHERE) {
            // A DoRender listener can reconstruct the identity from PlayerRenderState.name and draw it
            // directly. Skipping that extension point in alias mode is the only reliable fail-closed policy.
            return nameTagEvent;
        }
        if (plan == PlayerNameTagRenderPlan.PRIVATE_REAL_NAME
                && nameTagEvent.getEntityRenderState() instanceof PlayerNameTagRenderState playerNameState) {
            // Include safe changes made by render-state extensions after CanRender completed.
            playerNameState.ecclientsettings$setPlayerNameTagState(
                    PlayerNameTagMode.HIDE,
                    nameTagEvent.getContent(),
                    null
            );
        }
        DeferredNameTagPass pass = ecclientsettings$eventPass(plan);
        if (pass == null) {
            return original.call(eventBus, event);
        }

        Event[] posted = new Event[1];
        DeferredNameTagPassResult result = ecclientsettings$runPass(
                pass,
                nameTagEvent.getMultiBufferSource(),
                () -> posted[0] = original.call(eventBus, event)
        );
        if (result == DeferredNameTagPassResult.COMPLETE) {
            return posted[0];
        }

        if (pass == DeferredNameTagPass.PRIVATE_REAL_NAME) {
            nameTagEvent.setCanceled(true);
        }
        // In alias mode, skipping third-party event rendering still lets vanilla draw a safe alias.
        return result == DeferredNameTagPassResult.BEGIN_FAILED ? nameTagEvent : posted[0];
    }

    @WrapOperation(
            method = "render(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;renderNameTag(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lnet/minecraft/network/chat/Component;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"
            )
    )
    private void ecclientsettings$routeVanillaNameTag(
            EntityRenderer<?, ?> renderer,
            EntityRenderState state,
            Component text,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            Operation<Void> original
    ) {
        PlayerNameTagRenderPlan plan = ObsOverlayRuntime.playerNameTagRenderPlan(state);
        if (plan == PlayerNameTagRenderPlan.SUPPRESS) {
            return;
        }
        if (plan == PlayerNameTagRenderPlan.UNCHANGED) {
            original.call(renderer, state, text, poseStack, buffers, packedLight);
            return;
        }

        PlayerNameTagRenderState playerNameState = (PlayerNameTagRenderState) state;
        if (plan == PlayerNameTagRenderPlan.ALIAS_EVERYWHERE) {
            Component alias = playerNameState.ecclientsettings$getPlayerNameTagAlias();
            if (alias != null) {
                // Ignore the mutable render-state text in the safety fallback; only the generated alias is trusted.
                original.call(renderer, state, alias, poseStack, buffers, packedLight);
            }
            return;
        }

        Component originalName = playerNameState.ecclientsettings$getOriginalPlayerNameTag();
        if (plan == PlayerNameTagRenderPlan.PRIVATE_REAL_NAME) {
            if (originalName != null) {
                ecclientsettings$runPass(
                        DeferredNameTagPass.PRIVATE_REAL_NAME,
                        buffers,
                        () -> original.call(renderer, state, originalName, poseStack, buffers, packedLight)
                );
            }
            return;
        }

        Component alias = playerNameState.ecclientsettings$getPlayerNameTagAlias();
        if (alias == null) {
            return;
        }
        DeferredNameTagPassResult publicResult = ecclientsettings$runPass(
                DeferredNameTagPass.PUBLIC_ALIAS,
                buffers,
                () -> original.call(renderer, state, alias, poseStack, buffers, packedLight)
        );
        if (publicResult == DeferredNameTagPassResult.BEGIN_FAILED) {
            // The alias contains no private identity and is the only safe fallback on the main target.
            original.call(renderer, state, alias, poseStack, buffers, packedLight);
            return;
        }
        if (publicResult != DeferredNameTagPassResult.COMPLETE || originalName == null) {
            return;
        }
        ecclientsettings$runPass(
                DeferredNameTagPass.PRIVATE_REAL_NAME,
                buffers,
                () -> original.call(renderer, state, originalName, poseStack, buffers, packedLight)
        );
    }

    @Unique
    @Nullable
    private static DeferredNameTagPass ecclientsettings$eventPass(PlayerNameTagRenderPlan plan) {
        return plan == PlayerNameTagRenderPlan.PRIVATE_REAL_NAME
                ? DeferredNameTagPass.PRIVATE_REAL_NAME
                : null;
    }

    @Unique
    private static DeferredNameTagPassResult ecclientsettings$runPass(
            DeferredNameTagPass pass,
            MultiBufferSource buffers,
            Runnable draw
    ) {
        if (!ObsOverlayRuntime.beginPlayerNamePass(pass, buffers)) {
            return DeferredNameTagPassResult.BEGIN_FAILED;
        }
        boolean flushed;
        try {
            ObsOverlayRuntime.withDeferredNameTagPass(pass, draw);
        } finally {
            flushed = ObsOverlayRuntime.endWorldComponent(buffers);
        }
        return flushed ? DeferredNameTagPassResult.COMPLETE : DeferredNameTagPassResult.FLUSH_FAILED;
    }
}
