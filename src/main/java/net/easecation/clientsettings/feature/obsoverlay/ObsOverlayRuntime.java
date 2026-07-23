package net.easecation.clientsettings.feature.obsoverlay;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.easecation.clientsettings.ECClientSettings;
import net.easecation.clientsettings.config.ObsOverlayConfig;
import net.easecation.clientsettings.feature.obsoverlay.nativehook.ObsOverlayHook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.state.GuiElementRenderState;
import net.minecraft.client.gui.render.state.GuiItemRenderState;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.gui.render.state.GuiTextRenderState;
import net.minecraft.client.gui.render.state.pip.PictureInPictureRenderState;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CommandBlockEditScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.SignText;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.event.lifecycle.ClientStoppingEvent;
import net.neoforged.neoforge.client.gui.PictureInPictureRendererRegistration;
import org.lwjgl.glfw.GLFWNativeWin32;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Locale;

public final class ObsOverlayRuntime {

    private static final ArrayDeque<CaptureFrame> CAPTURE_STACK = new ArrayDeque<>();
    private static final ArrayDeque<MultiBufferSource> FAILED_WORLD_FLUSHES = new ArrayDeque<>();
    private static volatile ObsOverlayRenderer renderer;
    private static volatile ObsOverlayHook hook;
    private static volatile ObsOverlayHookStatus status = ObsOverlayHookStatus.NOT_INITIALIZED;
    private static volatile String failureDetail = "";
    private static volatile GuiRenderState mainGuiState;
    private static volatile List<PictureInPictureRendererRegistration<?>> pictureInPictureRenderers;

    private ObsOverlayRuntime() {
    }

    public static synchronized void initialize(Minecraft minecraft) {
        if (status != ObsOverlayHookStatus.NOT_INITIALIZED) {
            return;
        }
        ObsOverlayConfig.refresh();
        try {
            if (!ObsOverlayHook.supportedPlatform()) {
                status = ObsOverlayHookStatus.UNSUPPORTED;
                failureDetail = "Windows x64/x86 required";
                return;
            }
            List<PictureInPictureRendererRegistration<?>> registrations = pictureInPictureRenderers;
            if (registrations == null) {
                throw new IllegalStateException("GUI picture-in-picture registrations were not captured");
            }
            renderer = new ObsOverlayRenderer(minecraft, registrations);
            long windowHandle = GLFWNativeWin32.glfwGetWin32Window(minecraft.getWindow().getWindow());
            hook = ObsOverlayHook.install(windowHandle, renderer::composite, ObsOverlayRuntime::onCompositorFailure);
            status = hook.unsafeCaptureOrder()
                    ? ObsOverlayHookStatus.UNSAFE_CAPTURE_ORDER
                    : ObsOverlayHookStatus.READY;
            boolean viaBedrockLoaded = ModList.get().isLoaded("viabedrockutility");
            boolean viaBedrockDeferredNamesReady = ViaBedrockCompatibility.deferredNameTagHookAvailable();
            ECClientSettings.LOGGER.info(
                    "OBS privacy overlay initialized: status={}, Sodium={}, Iris={}, ImmediatelyFast={}, ViaBedrockUtility={}, ViaBedrockDeferredNames={}",
                    status,
                    ModList.get().isLoaded("sodium"),
                    ModList.get().isLoaded("iris"),
                    ModList.get().isLoaded("immediatelyfast"),
                    viaBedrockLoaded,
                    viaBedrockLoaded ? viaBedrockDeferredNamesReady : "not-installed"
            );
        } catch (Exception | LinkageError exception) {
            status = ObsOverlayHookStatus.FAILED;
            failureDetail = safeMessage(exception);
            ECClientSettings.LOGGER.error("Could not initialize OBS privacy overlay", exception);
        }
    }

    public static void capturePictureInPictureRenderers(
            List<PictureInPictureRendererRegistration<?>> registrations
    ) {
        pictureInPictureRenderers = List.copyOf(registrations);
    }

    public static void beginFrame(GuiRenderState state) {
        mainGuiState = state;
        if (!CAPTURE_STACK.isEmpty()) {
            ECClientSettings.LOGGER.warn("Clearing {} unclosed OBS GUI capture frame(s)", CAPTURE_STACK.size());
            CAPTURE_STACK.clear();
        }
        ObsOverlayRenderer current = renderer;
        if (current != null) {
            if (!retryFailedWorldFlushes()) {
                return;
            }
            current.beginFrame();
        }
    }

    public static void beginComponent(ObsOverlayComponent component) {
        if (component.group() == ObsOverlayComponent.Group.WORLD) {
            throw new IllegalArgumentException("World OBS components require a buffer source: " + component);
        }
        ObsOverlaySettings settings = ObsOverlayConfig.current();
        CaptureMode mode = componentMode(component, settings);
        CAPTURE_STACK.push(new CaptureFrame(mode, false));
    }

    public static void endComponent() {
        endCapture();
    }

    public static boolean beginWorldComponent(ObsOverlayComponent component, MultiBufferSource buffers) {
        if (component.group() != ObsOverlayComponent.Group.WORLD) {
            throw new IllegalArgumentException("Expected a world OBS overlay component: " + component);
        }
        ObsOverlaySettings settings = ObsOverlayConfig.current();
        CaptureMode mode = componentMode(component, settings);
        if (mode == CaptureMode.INHERIT) {
            CAPTURE_STACK.push(new CaptureFrame(CaptureMode.INHERIT, false));
            return true;
        }
        IrisCompatibility.WorldRedirectDecision irisDecision = IrisCompatibility.worldRedirectDecision();
        if (irisDecision != IrisCompatibility.WorldRedirectDecision.ALLOW) {
            if (!settings.failClosed()) {
                CAPTURE_STACK.push(new CaptureFrame(CaptureMode.INHERIT, false));
                return true;
            }
            return false;
        }

        ObsOverlayRenderer current = renderer;
        if (current == null) {
            if (settings.failClosed()) {
                return false;
            }
            CAPTURE_STACK.push(new CaptureFrame(CaptureMode.INHERIT, false));
            return true;
        }
        ObsOverlayRenderer.WorldGraphicsState graphicsState = current.captureWorldGraphicsState();
        if (!OverlayBufferFlusher.flush(buffers)) {
            current.restoreWorldGraphicsState(graphicsState);
            onWorldFlushFailure(false);
            if (settings.failClosed()) {
                return false;
            }
            CAPTURE_STACK.push(new CaptureFrame(CaptureMode.INHERIT, false));
            return true;
        }
        try {
            current.beginWorld(mode == CaptureMode.OVERLAY, graphicsState);
        } catch (RuntimeException | Error throwable) {
            current.restoreWorldGraphicsState(graphicsState);
            throw throwable;
        }
        CAPTURE_STACK.push(new CaptureFrame(mode, true));
        return true;
    }

    public static boolean beginDeferredNameTags(MultiBufferSource buffers) {
        ObsOverlaySettings settings = ObsOverlayConfig.current();
        ObsOverlayComponent component = settings.protects(ObsOverlayComponent.NAME_TAGS)
                ? ObsOverlayComponent.NAME_TAGS
                : ObsOverlayComponent.SNEAKING_NAME_TAGS;
        return beginWorldComponent(component, buffers);
    }

    public static void markViaBedrockDeferredNameTagHookApplied() {
        ViaBedrockCompatibility.markDeferredNameTagHookApplied();
    }

    public static boolean allowNameTagRendering(ObsOverlayComponent component) {
        if (ViaBedrockCompatibility.deferredNameTagHookAvailable()) {
            return true;
        }
        ObsOverlaySettings settings = ObsOverlayConfig.current();
        if (!settings.protects(component)) {
            return true;
        }
        markProtectionFailed(
                "ViaBedrockUtility deferred name-tag protection did not apply",
                new IllegalStateException("ViaBedrockUtility deferred name-tag hook unavailable")
        );
        return !settings.failClosed();
    }

    public static boolean suspendImmediatelyFastSignTextCache(SignText text) {
        return ObsOverlayConfig.current().protects(ObsOverlayComponent.SIGN_TEXT)
                && ImmediatelyFastCompatibility.suspendSignTextCache(text);
    }

    public static void restoreImmediatelyFastSignTextCache(SignText text, boolean restore) {
        ImmediatelyFastCompatibility.restoreSignTextCache(text, restore);
    }

    public static void endWorldComponent(MultiBufferSource buffers) {
        CaptureFrame frame = CAPTURE_STACK.peek();
        if (frame != null && frame.worldRedirect()) {
            boolean flushed = OverlayBufferFlusher.flush(buffers);
            if (!flushed && ObsOverlayConfig.current().failClosed()) {
                // Keep the protected target bound until beginFrame restores it; delayed vertices cannot leak
                // into the main target in the remainder of this frame.
                CAPTURE_STACK.pop();
                FAILED_WORLD_FLUSHES.addLast(buffers);
                onWorldFlushFailure(true);
                return;
            }
        }
        endCapture();
    }

    public static void beginScreen(Screen screen) {
        ObsOverlaySettings settings = ObsOverlayConfig.current();
        boolean protectChatInput = screen instanceof ChatScreen
                && settings.protects(ObsOverlayComponent.CHAT_INPUT);
        CaptureMode mode = settings.enabled() && (protectChatInput || isProtectedScreen(screen, settings))
                ? protectedMode(settings)
                : CaptureMode.INHERIT;
        CAPTURE_STACK.push(new CaptureFrame(mode, false));
    }

    public static void endScreen() {
        endCapture();
    }

    public static void beginTestMarker() {
        ObsOverlaySettings settings = ObsOverlayConfig.current();
        CAPTURE_STACK.push(new CaptureFrame(settings.enabled() ? protectedMode(settings) : CaptureMode.INHERIT, false));
    }

    public static void endTestMarker() {
        endCapture();
    }

    public static boolean redirect(GuiRenderState source, GuiItemRenderState state) {
        CaptureMode mode = guiMode(source);
        if (mode == CaptureMode.OVERLAY && renderer != null) {
            renderer.submit(state);
        }
        return mode != CaptureMode.INHERIT;
    }

    public static boolean redirect(GuiRenderState source, GuiTextRenderState state) {
        CaptureMode mode = guiMode(source);
        if (mode == CaptureMode.OVERLAY && renderer != null) {
            renderer.submit(state);
        }
        return mode != CaptureMode.INHERIT;
    }

    public static boolean redirect(GuiRenderState source, PictureInPictureRenderState state) {
        CaptureMode mode = guiMode(source);
        if (mode == CaptureMode.OVERLAY && renderer != null) {
            renderer.submit(state);
        }
        return mode != CaptureMode.INHERIT;
    }

    public static boolean redirect(GuiRenderState source, GuiElementRenderState state) {
        CaptureMode mode = guiMode(source);
        if (mode == CaptureMode.OVERLAY && renderer != null) {
            renderer.submit(state);
        }
        return mode != CaptureMode.INHERIT;
    }

    public static void redirectNextStratum(GuiRenderState source) {
        CaptureMode mode = guiMode(source);
        if (mode == CaptureMode.OVERLAY && renderer != null) {
            renderer.nextGuiStratum();
        }
    }

    public static boolean suppressBlur(GuiRenderState source) {
        return guiMode(source) != CaptureMode.INHERIT;
    }

    public static boolean suppressCapturedScreenBlur() {
        for (CaptureFrame frame : CAPTURE_STACK) {
            if (frame.mode() != CaptureMode.INHERIT) {
                return true;
            }
        }
        return false;
    }

    public static void renderGuiOverlay(GpuBufferSlice fogBuffer) {
        ObsOverlayRenderer current = renderer;
        if (current != null) {
            current.renderGui(fogBuffer);
        }
    }

    public static void backupSceneDepth() {
        ObsOverlayRenderer current = renderer;
        if (current != null) {
            current.backupSceneDepth();
        }
    }

    public static RenderTarget guiRenderTarget(GuiRenderer guiRenderer, RenderTarget original) {
        ObsOverlayRenderer current = renderer;
        return current == null ? original : current.guiTargetFor(guiRenderer, original);
    }

    public static ObsOverlayHookStatus status() {
        return status;
    }

    public static String failureDetail() {
        return failureDetail;
    }

    public static boolean protectionReady() {
        return status == ObsOverlayHookStatus.READY && renderer != null;
    }

    public static boolean obsGameCaptureDetected() {
        ObsOverlayHook current = hook;
        return current != null && current.isObsCaptureLoaded();
    }

    public static boolean irisWorldCompatibilityUnavailable() {
        return IrisCompatibility.unavailable();
    }

    public static boolean irisShaderPackInUse() {
        return IrisCompatibility.shaderPackInUse();
    }

    public static void onSettingsChanged(ObsOverlaySettings settings) {
        if (settings.enabled() && status == ObsOverlayHookStatus.UNSAFE_CAPTURE_ORDER) {
            ECClientSettings.LOGGER.warn(
                    "OBS overlay enabled after OBS game capture was already attached; fail-closed behavior is {}",
                    settings.failClosed()
            );
        }
    }

    public static synchronized void stop() {
        ObsOverlayHook currentHook = hook;
        hook = null;
        if (currentHook != null) {
            currentHook.close();
        }
        ObsOverlayRenderer currentRenderer = renderer;
        renderer = null;
        if (currentRenderer != null) {
            retryFailedWorldFlushes();
            currentRenderer.close();
        }
        CAPTURE_STACK.clear();
        FAILED_WORLD_FLUSHES.clear();
        mainGuiState = null;
        pictureInPictureRenderers = null;
        status = ObsOverlayHookStatus.STOPPED;
    }

    public static void onClientStopping(ClientStoppingEvent event) {
        stop();
    }

    private static CaptureMode componentMode(ObsOverlayComponent component, ObsOverlaySettings settings) {
        if (!settings.protects(component)) {
            return CaptureMode.INHERIT;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (settings.autoHides(component)
                && minecraft.screen != null
                && !(minecraft.screen instanceof ChatScreen)) {
            return CaptureMode.DISCARD;
        }
        return protectedMode(settings);
    }

    private static CaptureMode protectedMode(ObsOverlaySettings settings) {
        if (protectionReady()) {
            return CaptureMode.OVERLAY;
        }
        return settings.failClosed() ? CaptureMode.DISCARD : CaptureMode.INHERIT;
    }

    private static CaptureMode guiMode(GuiRenderState source) {
        if (source != mainGuiState) {
            return CaptureMode.INHERIT;
        }
        for (CaptureFrame frame : CAPTURE_STACK) {
            if (frame.mode() != CaptureMode.INHERIT) {
                return frame.mode();
            }
        }
        return CaptureMode.INHERIT;
    }

    private static void endCapture() {
        if (CAPTURE_STACK.isEmpty()) {
            ECClientSettings.LOGGER.warn("OBS overlay capture ended without a matching begin");
            return;
        }
        CaptureFrame frame = CAPTURE_STACK.pop();
        if (frame.worldRedirect() && renderer != null) {
            renderer.endWorld();
        }
    }

    private static boolean retryFailedWorldFlushes() {
        while (!FAILED_WORLD_FLUSHES.isEmpty()) {
            MultiBufferSource buffers = FAILED_WORLD_FLUSHES.peekFirst();
            if (!OverlayBufferFlusher.flush(buffers)) {
                return false;
            }
            FAILED_WORLD_FLUSHES.removeFirst();
        }
        return true;
    }

    private static boolean isProtectedScreen(Screen screen, ObsOverlaySettings settings) {
        if (settings.hideAllInGameScreens() && Minecraft.getInstance().level != null) {
            return true;
        }
        if (screen instanceof InventoryScreen && settings.protects(ObsOverlayScreen.INVENTORY)) {
            return true;
        }
        if (screen instanceof CreativeModeInventoryScreen && settings.protects(ObsOverlayScreen.CREATIVE_INVENTORY)) {
            return true;
        }
        if (screen instanceof PauseScreen && settings.protects(ObsOverlayScreen.PAUSE_MENU)) {
            return true;
        }
        if (screen instanceof CommandBlockEditScreen && settings.protects(ObsOverlayScreen.COMMAND_BLOCK)) {
            return true;
        }
        if (!settings.customHandledScreensEnabled() || !(screen instanceof AbstractContainerScreen<?> container)) {
            return false;
        }
        ResourceLocation id = BuiltInRegistries.MENU.getKey(container.getMenu().getType());
        if (id == null) {
            return false;
        }
        String fullId = id.toString().toLowerCase(Locale.ROOT);
        String path = id.getPath().toLowerCase(Locale.ROOT);
        return settings.customHandledScreenIds().stream()
                .map(value -> value.toLowerCase(Locale.ROOT))
                .anyMatch(value -> value.equals(fullId) || value.equals(path));
    }

    private static void onCompositorFailure(Throwable throwable) {
        markProtectionFailed("OBS overlay compositor failed; privacy controls are now fail-closed", throwable);
    }

    private static void onWorldFlushFailure(boolean protectedTargetBound) {
        String action = protectedTargetBound
                ? "keeping the protected target bound until the next frame"
                : "skipping protected world rendering under the configured safety policy";
        markProtectionFailed(
                "OBS overlay could not flush protected world buffers; " + action,
                new IllegalStateException("Protected world buffer flush failed")
        );
    }

    private static void markProtectionFailed(String logMessage, Throwable throwable) {
        if (status == ObsOverlayHookStatus.FAILED) {
            return;
        }
        status = ObsOverlayHookStatus.FAILED;
        failureDetail = safeMessage(throwable);
        ECClientSettings.LOGGER.error(logMessage, throwable);
    }

    private static String safeMessage(Throwable throwable) {
        String message = throwable.getMessage();
        return message == null || message.isBlank() ? throwable.getClass().getSimpleName() : message;
    }

    private enum CaptureMode {
        INHERIT,
        OVERLAY,
        DISCARD
    }

    private record CaptureFrame(CaptureMode mode, boolean worldRedirect) {
    }
}
