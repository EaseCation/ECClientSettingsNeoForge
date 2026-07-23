package net.easecation.clientsettings.feature.obsoverlay;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import net.easecation.clientsettings.ECClientSettings;
import net.easecation.clientsettings.feature.obsoverlay.nativehook.RawOverlayCompositor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.state.GuiItemRenderState;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.gui.render.state.GuiTextRenderState;
import net.minecraft.client.gui.render.state.pip.PictureInPictureRenderState;
import net.minecraft.client.gui.render.state.GuiElementRenderState;
import net.neoforged.neoforge.client.gui.PictureInPictureRendererRegistration;

import java.util.ArrayDeque;
import java.util.List;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

final class ObsOverlayRenderer implements AutoCloseable {

    private final Minecraft minecraft;
    private final GuiRenderState overlayGuiState = new GuiRenderState();
    private final GuiRenderer overlayGuiRenderer;
    private final TextureTarget guiTarget;
    private final TextureTarget worldTarget;
    private final TextureTarget publicAliasTarget;
    private final TextureTarget discardTarget;
    private final RawOverlayCompositor compositor = new RawOverlayCompositor();
    private final ArrayDeque<WorldRedirect> worldRedirects = new ArrayDeque<>();
    private boolean guiDirty;
    private boolean worldDirty;
    private boolean publicAliasDirty;
    private boolean guiTargetPrepared;
    private boolean worldTargetPrepared;
    private boolean publicAliasTargetPrepared;
    private boolean discardTargetPrepared;
    private boolean worldDepthInitialized;
    private boolean publicAliasDepthInitialized;
    private boolean publicFramePrepared;
    private boolean publicAliasPresented;
    private boolean sceneDepthReady;
    private boolean depthCopyWarningLogged;
    private GpuTexture sceneDepthTexture;

    ObsOverlayRenderer(
            Minecraft minecraft,
            List<PictureInPictureRendererRegistration<?>> pictureInPictureRenderers
    ) {
        this.minecraft = minecraft;
        int width = Math.max(1, minecraft.getMainRenderTarget().width);
        int height = Math.max(1, minecraft.getMainRenderTarget().height);
        guiTarget = new TextureTarget("EC OBS GUI overlay", width, height, true);
        worldTarget = new TextureTarget("EC OBS world overlay", width, height, true);
        publicAliasTarget = new TextureTarget("EC OBS public player aliases", width, height, true);
        discardTarget = new TextureTarget("EC OBS discarded content", 1, 1, true);
        overlayGuiRenderer = new GuiRenderer(
                overlayGuiState,
                minecraft.renderBuffers().bufferSource(),
                pictureInPictureRenderers
        );
    }

    void beginFrame() {
        if (!worldRedirects.isEmpty()) {
            ECClientSettings.LOGGER.warn("Recovering {} unclosed OBS world overlay redirect(s)", worldRedirects.size());
            while (!worldRedirects.isEmpty()) {
                restore(worldRedirects.pop());
            }
        }
        int width = Math.max(1, minecraft.getMainRenderTarget().width);
        int height = Math.max(1, minecraft.getMainRenderTarget().height);
        ensureSize(guiTarget, width, height);
        ensureSize(worldTarget, width, height);
        ensureSize(publicAliasTarget, width, height);
        guiDirty = false;
        worldDirty = false;
        publicAliasDirty = false;
        guiTargetPrepared = false;
        worldTargetPrepared = false;
        publicAliasTargetPrepared = false;
        discardTargetPrepared = false;
        worldDepthInitialized = false;
        publicAliasDepthInitialized = false;
        publicFramePrepared = false;
        publicAliasPresented = false;
        sceneDepthReady = false;
    }

    WorldGraphicsState captureWorldGraphicsState() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer viewport = stack.mallocInt(4);
            IntBuffer scissorBox = stack.mallocInt(4);
            ByteBuffer colorMask = stack.malloc(4);
            FloatBuffer clearColor = stack.mallocFloat(4);
            DoubleBuffer clearDepth = stack.mallocDouble(1);
            GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
            GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX, scissorBox);
            GL11.glGetBooleanv(GL11.GL_COLOR_WRITEMASK, colorMask);
            GL11.glGetFloatv(GL11.GL_COLOR_CLEAR_VALUE, clearColor);
            GL11.glGetDoublev(GL11.GL_DEPTH_CLEAR_VALUE, clearDepth);
            return new WorldGraphicsState(
                    GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING),
                    GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING),
                    viewport.get(0), viewport.get(1), viewport.get(2), viewport.get(3),
                    GL11.glIsEnabled(GL11.GL_SCISSOR_TEST),
                    scissorBox.get(0), scissorBox.get(1), scissorBox.get(2), scissorBox.get(3),
                    GL11.glIsEnabled(GL11.GL_DEPTH_TEST),
                    GL11.glGetInteger(GL11.GL_DEPTH_FUNC),
                    GL11.glIsEnabled(GL11.GL_BLEND),
                    GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB),
                    GL11.glGetInteger(GL14.GL_BLEND_DST_RGB),
                    GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA),
                    GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA),
                    GL11.glIsEnabled(GL11.GL_CULL_FACE),
                    GL11.glGetInteger(GL11.GL_POLYGON_MODE),
                    GL11.glIsEnabled(GL11.GL_POLYGON_OFFSET_FILL),
                    GL11.glGetFloat(GL11.GL_POLYGON_OFFSET_FACTOR),
                    GL11.glGetFloat(GL11.GL_POLYGON_OFFSET_UNITS),
                    GL11.glIsEnabled(GL11.GL_COLOR_LOGIC_OP),
                    GL11.glGetInteger(GL11.GL_LOGIC_OP_MODE),
                    GL11.glIsEnabled(GL11.GL_STENCIL_TEST),
                    GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK),
                    colorMask.get(0) != 0, colorMask.get(1) != 0,
                    colorMask.get(2) != 0, colorMask.get(3) != 0,
                    clearColor.get(0), clearColor.get(1), clearColor.get(2), clearColor.get(3),
                    clearDepth.get(0)
            );
        }
    }

    void restoreWorldGraphicsState(WorldGraphicsState state) {
        state.restore();
    }

    void beginWorld(WorldDestination worldDestination, WorldGraphicsState graphicsState) {
        GpuTextureView previousColor = RenderSystem.outputColorTextureOverride;
        GpuTextureView previousDepth = RenderSystem.outputDepthTextureOverride;
        TextureTarget destination = switch (worldDestination) {
            case PRIVATE_OVERLAY -> worldTarget;
            case PUBLIC_ALIAS -> publicAliasTarget;
            case DISCARD -> discardTarget;
        };
        boolean composite = worldDestination != WorldDestination.DISCARD;
        GpuTextureView sourceColor = previousColor != null
                ? previousColor
                : minecraft.getMainRenderTarget().getColorTextureView();
        if (composite && sourceColor != null) {
            boolean resized = ensureSize(destination, sourceColor.getWidth(0), sourceColor.getHeight(0));
            if (resized) {
                if (worldDestination == WorldDestination.PUBLIC_ALIAS) {
                    publicAliasTargetPrepared = false;
                    publicAliasDepthInitialized = false;
                } else {
                    worldTargetPrepared = false;
                    worldDepthInitialized = false;
                }
            }
        }
        prepareWorldTarget(destination, worldDestination);
        GpuTextureView sourceDepth = previousDepth != null
                ? previousDepth
                : minecraft.getMainRenderTarget().getDepthTextureView();
        boolean depthInitialized = worldDestination == WorldDestination.PUBLIC_ALIAS
                ? publicAliasDepthInitialized
                : worldDepthInitialized;
        if (composite && !depthInitialized) {
            copyDepthIfCompatible(sourceDepth, destination);
            if (worldDestination == WorldDestination.PUBLIC_ALIAS) {
                publicAliasDepthInitialized = true;
            } else {
                worldDepthInitialized = true;
            }
        }
        worldRedirects.push(new WorldRedirect(previousColor, previousDepth, graphicsState));
        RenderSystem.outputColorTextureOverride = destination.getColorTextureView();
        RenderSystem.outputDepthTextureOverride = destination.getDepthTextureView();
        if (worldDestination == WorldDestination.PUBLIC_ALIAS) {
            publicAliasDirty = true;
        } else if (worldDestination == WorldDestination.PRIVATE_OVERLAY) {
            worldDirty = true;
        }
    }

    void endWorld() {
        if (worldRedirects.isEmpty()) {
            ECClientSettings.LOGGER.warn("OBS world overlay redirect ended without a matching begin");
            return;
        }
        restore(worldRedirects.pop());
    }

    void submit(GuiItemRenderState state) {
        prepareGuiTarget();
        guiDirty = true;
        overlayGuiState.submitItem(state);
    }

    void submit(GuiTextRenderState state) {
        prepareGuiTarget();
        guiDirty = true;
        overlayGuiState.submitText(state);
    }

    void submit(PictureInPictureRenderState state) {
        prepareGuiTarget();
        guiDirty = true;
        overlayGuiState.submitPicturesInPictureState(state);
    }

    void submit(GuiElementRenderState state) {
        prepareGuiTarget();
        guiDirty = true;
        overlayGuiState.submitGuiElement(state);
    }

    void nextGuiStratum() {
        overlayGuiState.nextStratum();
    }

    void renderGui(GpuBufferSlice fogBuffer) {
        if (guiDirty) {
            overlayGuiRenderer.render(fogBuffer);
            overlayGuiRenderer.incrementFrameNumber();
        } else {
            overlayGuiState.reset();
        }
    }

    void backupSceneDepth() {
        if (!worldDirty && !publicAliasDirty) {
            return;
        }
        RenderTarget mainTarget = minecraft.getMainRenderTarget();
        GpuTextureView source = mainTarget.getDepthTextureView();
        if (source == null) {
            return;
        }
        ensureSceneDepthTexture(source);
        try {
            RenderSystem.getDevice().createCommandEncoder().copyTextureToTexture(
                    source.texture(),
                    sceneDepthTexture,
                    0,
                    0,
                    0,
                    0,
                    0,
                    source.getWidth(0),
                    source.getHeight(0)
            );
            sceneDepthReady = true;
        } catch (RuntimeException exception) {
            warnDepthCopyFailure(exception);
        }
    }

    RenderTarget guiTargetFor(GuiRenderer renderer, RenderTarget original) {
        return renderer == overlayGuiRenderer ? guiTarget : original;
    }

    boolean preparePublicFrame() {
        int width = minecraft.getWindow().getWidth();
        int height = minecraft.getWindow().getHeight();
        if (width <= 0 || height <= 0) {
            publicFramePrepared = false;
            publicAliasPresented = false;
            return false;
        }
        boolean aliasDrawn = publicAliasDirty;
        // Every swap starts from a known clean frame. This also covers minimized frames and the
        // recursive second swap performed while toggling fullscreen.
        publicAliasPresented = true;
        compositor.restoreAndComposite(
                minecraft.getMainRenderTarget(),
                true,
                sceneDepthReady ? sceneDepthTexture : null,
                publicAliasTarget,
                aliasDrawn,
                guiTarget,
                false,
                width,
                height
        );
        publicFramePrepared = true;
        // A successful clean-only replace needs no second restore after OBS capture. On an
        // exception the earlier true value forces the native callback to scrub the buffer again.
        publicAliasPresented = aliasDrawn;
        return true;
    }

    boolean composite(boolean allowPrivateOverlays) {
        boolean prepared = publicFramePrepared;
        boolean restorePublicAlias = publicAliasPresented;
        publicFramePrepared = false;
        publicAliasPresented = false;
        if (!allowPrivateOverlays || !prepared) {
            if (restorePublicAlias || allowPrivateOverlays) {
                compositor.restoreAndComposite(
                        minecraft.getMainRenderTarget(),
                        true,
                        null,
                        worldTarget,
                        false,
                        guiTarget,
                        false,
                        minecraft.getWindow().getWidth(),
                        minecraft.getWindow().getHeight()
                );
            }
            return prepared;
        }
        compositor.restoreAndComposite(
                minecraft.getMainRenderTarget(),
                restorePublicAlias,
                sceneDepthReady ? sceneDepthTexture : null,
                worldTarget,
                worldDirty,
                guiTarget,
                guiDirty,
                minecraft.getWindow().getWidth(),
                minecraft.getWindow().getHeight()
        );
        return true;
    }

    private boolean copyDepthIfCompatible(GpuTextureView sourceView, RenderTarget destination) {
        if (sourceView == null || sourceView == destination.getDepthTextureView()) {
            return false;
        }
        GpuTexture source = sourceView.texture();
        GpuTexture target = destination.getDepthTexture();
        if (target == null
                || source.getFormat() != target.getFormat()
                || sourceView.getWidth(0) != target.getWidth(0)
                || sourceView.getHeight(0) != target.getHeight(0)) {
            return false;
        }
        try {
            RenderSystem.getDevice().createCommandEncoder().copyTextureToTexture(
                    source,
                    target,
                    0,
                    0,
                    0,
                    0,
                    0,
                    target.getWidth(0),
                    target.getHeight(0)
            );
            return true;
        } catch (RuntimeException exception) {
            warnDepthCopyFailure(exception);
            return false;
        }
    }

    private void ensureSceneDepthTexture(GpuTextureView source) {
        GpuTexture sourceTexture = source.texture();
        if (sceneDepthTexture != null
                && sceneDepthTexture.getFormat() == sourceTexture.getFormat()
                && sceneDepthTexture.getWidth(0) == source.getWidth(0)
                && sceneDepthTexture.getHeight(0) == source.getHeight(0)) {
            return;
        }
        destroySceneDepthTexture();
        sceneDepthTexture = RenderSystem.getDevice().createTexture(
                () -> "EC OBS completed scene depth",
                15,
                sourceTexture.getFormat(),
                source.getWidth(0),
                source.getHeight(0),
                1,
                1
        );
        sceneDepthTexture.setTextureFilter(FilterMode.NEAREST, false);
        sceneDepthTexture.setAddressMode(AddressMode.CLAMP_TO_EDGE);
    }

    private void warnDepthCopyFailure(RuntimeException exception) {
        if (!depthCopyWarningLogged) {
            depthCopyWarningLogged = true;
            ECClientSettings.LOGGER.warn(
                    "Could not copy the active depth texture for OBS world overlays; shader depth may differ",
                    exception
            );
        }
    }

    private void destroySceneDepthTexture() {
        if (sceneDepthTexture != null) {
            sceneDepthTexture.close();
            sceneDepthTexture = null;
        }
    }

    private void prepareGuiTarget() {
        if (!guiTargetPrepared) {
            clear(guiTarget);
            guiTargetPrepared = true;
        }
    }

    private void prepareWorldTarget(TextureTarget target, WorldDestination destination) {
        boolean prepared = switch (destination) {
            case PRIVATE_OVERLAY -> worldTargetPrepared;
            case PUBLIC_ALIAS -> publicAliasTargetPrepared;
            case DISCARD -> discardTargetPrepared;
        };
        if (prepared) {
            return;
        }
        clear(target);
        switch (destination) {
            case PRIVATE_OVERLAY -> worldTargetPrepared = true;
            case PUBLIC_ALIAS -> publicAliasTargetPrepared = true;
            case DISCARD -> discardTargetPrepared = true;
        }
    }

    private static boolean ensureSize(TextureTarget target, int width, int height) {
        int safeWidth = Math.max(1, width);
        int safeHeight = Math.max(1, height);
        if (target.width != safeWidth || target.height != safeHeight) {
            target.resize(safeWidth, safeHeight);
            return true;
        }
        return false;
    }

    private static void clear(TextureTarget target) {
        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        if (target.getDepthTexture() == null) {
            encoder.clearColorTexture(target.getColorTexture(), 0);
        } else {
            encoder.clearColorAndDepthTextures(target.getColorTexture(), 0, target.getDepthTexture(), 1.0);
        }
    }

    private static void restore(WorldRedirect redirect) {
        RenderSystem.outputColorTextureOverride = redirect.color();
        RenderSystem.outputDepthTextureOverride = redirect.depth();
        redirect.graphicsState().restore();
    }

    @Override
    public void close() {
        while (!worldRedirects.isEmpty()) {
            restore(worldRedirects.pop());
        }
        overlayGuiRenderer.close();
        guiTarget.destroyBuffers();
        worldTarget.destroyBuffers();
        publicAliasTarget.destroyBuffers();
        discardTarget.destroyBuffers();
        destroySceneDepthTexture();
        compositor.close();
    }

    private record WorldRedirect(
            GpuTextureView color,
            GpuTextureView depth,
            WorldGraphicsState graphicsState
    ) {
    }

    enum WorldDestination {
        PRIVATE_OVERLAY,
        PUBLIC_ALIAS,
        DISCARD
    }

    record WorldGraphicsState(
            int drawFramebuffer,
            int readFramebuffer,
            int viewportX,
            int viewportY,
            int viewportWidth,
            int viewportHeight,
            boolean scissorEnabled,
            int scissorX,
            int scissorY,
            int scissorWidth,
            int scissorHeight,
            boolean depthTestEnabled,
            int depthFunction,
            boolean blendEnabled,
            int blendSourceRgb,
            int blendDestinationRgb,
            int blendSourceAlpha,
            int blendDestinationAlpha,
            boolean cullEnabled,
            int polygonMode,
            boolean polygonOffsetEnabled,
            float polygonOffsetFactor,
            float polygonOffsetUnits,
            boolean colorLogicEnabled,
            int colorLogicOperation,
            boolean stencilTestEnabled,
            boolean depthMask,
            boolean colorMaskRed,
            boolean colorMaskGreen,
            boolean colorMaskBlue,
            boolean colorMaskAlpha,
            float clearColorRed,
            float clearColorGreen,
            float clearColorBlue,
            float clearColorAlpha,
            double clearDepth
    ) {
        void restore() {
            GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, drawFramebuffer);
            GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readFramebuffer);
            GlStateManager._viewport(viewportX, viewportY, viewportWidth, viewportHeight);
            GlStateManager._scissorBox(scissorX, scissorY, scissorWidth, scissorHeight);
            if (scissorEnabled) {
                GlStateManager._enableScissorTest();
            } else {
                GlStateManager._disableScissorTest();
            }
            GlStateManager._depthFunc(depthFunction);
            setDepthTest(depthTestEnabled);
            GlStateManager._blendFuncSeparate(
                    blendSourceRgb, blendDestinationRgb, blendSourceAlpha, blendDestinationAlpha
            );
            setBlend(blendEnabled);
            setCull(cullEnabled);
            GlStateManager._polygonMode(GL11.GL_FRONT_AND_BACK, polygonMode);
            GlStateManager._polygonOffset(polygonOffsetFactor, polygonOffsetUnits);
            setPolygonOffset(polygonOffsetEnabled);
            GlStateManager._logicOp(colorLogicOperation);
            setColorLogic(colorLogicEnabled);
            setStencilTest(stencilTestEnabled);
            GlStateManager._depthMask(depthMask);
            GlStateManager._colorMask(colorMaskRed, colorMaskGreen, colorMaskBlue, colorMaskAlpha);
            GL11.glClearColor(clearColorRed, clearColorGreen, clearColorBlue, clearColorAlpha);
            GL11.glClearDepth(clearDepth);
        }

        private static void setDepthTest(boolean enabled) {
            if (enabled) {
                GlStateManager._enableDepthTest();
            } else {
                GlStateManager._disableDepthTest();
            }
        }

        private static void setBlend(boolean enabled) {
            if (enabled) {
                GlStateManager._enableBlend();
            } else {
                GlStateManager._disableBlend();
            }
        }

        private static void setCull(boolean enabled) {
            if (enabled) {
                GlStateManager._enableCull();
            } else {
                GlStateManager._disableCull();
            }
        }

        private static void setPolygonOffset(boolean enabled) {
            if (enabled) {
                GlStateManager._enablePolygonOffset();
            } else {
                GlStateManager._disablePolygonOffset();
            }
        }

        private static void setColorLogic(boolean enabled) {
            if (enabled) {
                GlStateManager._enableColorLogicOp();
            } else {
                GlStateManager._disableColorLogicOp();
            }
        }

        private static void setStencilTest(boolean enabled) {
            if (enabled) {
                GlStateManager._enableStencilTest();
            } else {
                GlStateManager._disableStencilTest();
            }
        }
    }
}
