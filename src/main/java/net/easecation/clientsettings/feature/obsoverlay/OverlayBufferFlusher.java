package net.easecation.clientsettings.feature.obsoverlay;

import net.easecation.clientsettings.ECClientSettings;
import net.minecraft.client.renderer.MultiBufferSource;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.IdentityHashMap;
import java.util.Set;

public final class OverlayBufferFlusher {

    private static final String IRIS_UNFLUSHABLE_WRAPPER =
            "net.irisshaders.batchedentityrendering.impl.FullyBufferedMultiBufferSource$UnflushableWrapper";
    private static final Set<String> VERIFIED_FLUSHABLE_OVERRIDES = Set.of(
            "net.irisshaders.batchedentityrendering.impl.FullyBufferedMultiBufferSource",
            "net.irisshaders.batchedentityrendering.impl.OldFullyBufferedMultiBufferSource",
            "net.raphimc.immediatelyfast.feature.core.BatchableBufferSource"
    );
    private static volatile boolean warned;

    private OverlayBufferFlusher() {
    }

    public static boolean flush(MultiBufferSource source) {
        if (!(source instanceof MultiBufferSource.BufferSource buffers)) {
            if (!warned) {
                warned = true;
                ECClientSettings.LOGGER.warn(
                        "Skipping an experimental OBS world overlay because {} cannot be safely flushed",
                        source.getClass().getName()
                );
            }
            return false;
        }
        try {
            verifiedFlushTarget(buffers).endBatch();
            return true;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            if (!warned) {
                warned = true;
                ECClientSettings.LOGGER.warn(
                        "Could not flush a buffered renderer for OBS world overlay compatibility", exception
                );
            }
            return false;
        }
    }

    private static MultiBufferSource.BufferSource verifiedFlushTarget(MultiBufferSource.BufferSource buffers)
            throws ReflectiveOperationException {
        IdentityHashMap<MultiBufferSource.BufferSource, Boolean> visited = new IdentityHashMap<>();
        MultiBufferSource.BufferSource current = buffers;
        while (visited.put(current, Boolean.TRUE) == null) {
            Class<?> type = current.getClass();
            // A subclass can redirect getBuffer while inheriting an endBatch that flushes the wrong storage.
            if (type == MultiBufferSource.BufferSource.class
                    || VERIFIED_FLUSHABLE_OVERRIDES.contains(type.getName())) {
                return current;
            }
            Field delegate = findVerifiedDelegate(type);
            if (delegate == null) {
                throw new IllegalStateException(
                        "Unverified BufferSource.endBatch override: " + type.getName()
                );
            }
            delegate.setAccessible(true);
            Object value = delegate.get(current);
            if (!(value instanceof MultiBufferSource.BufferSource source)) {
                throw new IllegalStateException("BufferSource wrapper delegate is unavailable: " + type.getName());
            }
            current = source;
        }
        throw new IllegalStateException("BufferSource wrapper cycle detected: " + buffers.getClass().getName());
    }

    private static Field findVerifiedDelegate(Class<?> type) {
        if (IRIS_UNFLUSHABLE_WRAPPER.equals(type.getName())) {
            try {
                return type.getDeclaredField("wrapped");
            } catch (NoSuchFieldException ignored) {
                return null;
            }
        }
        Field candidate = null;
        int instanceFieldCount = 0;
        for (Field field : type.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            instanceFieldCount++;
            if (MultiBufferSource.BufferSource.class.isAssignableFrom(field.getType())) {
                candidate = field;
            }
        }
        return instanceFieldCount == 1 ? candidate : null;
    }
}
