package net.easecation.clientsettings.mixin.obsoverlay;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.easecation.clientsettings.ECClientSettings;
import net.easecation.clientsettings.feature.obsoverlay.ObsOverlayRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;

/** Keeps ViaBedrockUtility's deferred name-tag replay inside the privacy target. */
@Pseudo
@Mixin(targets = "org.oryxel.viabedrockutility.renderer.DeferredNameTag", remap = false)
abstract class ViaBedrockDeferredNameTagMixin {

    @Unique
    private static boolean ecclientsettings$queueClearWarningLogged;
    @Unique
    private static volatile Field ecclientsettings$queueField;

    // VBU's production obfuscation currently maps flush() to vb(); support both artifacts explicitly.
    @WrapMethod(method = {"flush()V", "vb()V"}, remap = false, require = 1)
    private static void ecclientsettings$protectDeferredNameTags(Operation<Void> original) {
        MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        if (!ObsOverlayRuntime.beginDeferredNameTags(buffers)) {
            ecclientsettings$clearQueueFailClosed();
            return;
        }
        try {
            original.call();
        } finally {
            ObsOverlayRuntime.endWorldComponent(buffers);
        }
    }

    @Unique
    private static void ecclientsettings$clearQueueFailClosed() {
        try {
            Class<?> type = Class.forName(
                    "org.oryxel.viabedrockutility.renderer.DeferredNameTag",
                    false,
                    ViaBedrockDeferredNameTagMixin.class.getClassLoader()
            );
            Field queueField = ecclientsettings$findQueueField(type);
            Object value = queueField.get(null);
            if (value instanceof List<?> queue) {
                queue.clear();
                return;
            }
            throw new IllegalStateException("ViaBedrockUtility deferred name-tag queue is not a List");
        } catch (ReflectiveOperationException | RuntimeException exception) {
            if (!ecclientsettings$queueClearWarningLogged) {
                ecclientsettings$queueClearWarningLogged = true;
                ECClientSettings.LOGGER.error(
                        "Could not clear ViaBedrockUtility deferred name tags while OBS protection is fail-closed",
                        exception
                );
            }
        }
    }

    @Unique
    private static Field ecclientsettings$findQueueField(Class<?> type) throws ReflectiveOperationException {
        Field cached = ecclientsettings$queueField;
        if (cached != null) {
            return cached;
        }
        for (String knownName : List.of("QUEUE", "va")) {
            try {
                return ecclientsettings$cacheQueueField(type.getDeclaredField(knownName));
            } catch (NoSuchFieldException ignored) {
                // Fall through to the structural lookup for future VBU obfuscation mappings.
            }
        }
        Field candidate = null;
        for (Field field : type.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) && List.class.isAssignableFrom(field.getType())) {
                if (candidate != null) {
                    throw new NoSuchFieldException("Multiple static List fields found in " + type.getName());
                }
                candidate = field;
            }
        }
        if (candidate == null) {
            throw new NoSuchFieldException("No static List field found in " + type.getName());
        }
        return ecclientsettings$cacheQueueField(candidate);
    }

    @Unique
    private static Field ecclientsettings$cacheQueueField(Field field) {
        field.setAccessible(true);
        ecclientsettings$queueField = field;
        return field;
    }
}
