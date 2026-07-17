package net.easecation.clientsettings.profile.store;

import net.easecation.clientsettings.ECClientSettings;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

public final class AtomicProfileWriter {

    private final ProfileFileOperations operations;

    public AtomicProfileWriter(ProfileFileOperations operations) {
        this.operations = operations;
    }

    public void write(Path target, byte[] content) throws IOException {
        Path parent = target.getParent();
        operations.createDirectories(parent);
        Path temporary = parent.resolve(target.getFileName() + ".tmp-" + UUID.randomUUID());
        boolean replaced = false;
        IOException failure = null;
        try {
            try (FileChannel channel = operations.openFile(
                    temporary,
                    StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.WRITE
            )) {
                ByteBuffer buffer = ByteBuffer.wrap(content);
                while (buffer.hasRemaining()) {
                    channel.write(buffer);
                }
                channel.force(true);
            }
            try {
                operations.move(
                        temporary,
                        target,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING
                );
            } catch (AtomicMoveNotSupportedException exception) {
                throw new IOException("Atomic replacement is not supported for " + target, exception);
            }
            replaced = true;
            try {
                operations.forceDirectory(parent);
            } catch (IOException | UnsupportedOperationException exception) {
                ECClientSettings.LOGGER.debug("Could not fsync Profile directory {}", parent, exception);
            }
        } catch (IOException exception) {
            failure = exception;
            throw exception;
        } finally {
            if (!replaced) {
                try {
                    operations.deleteIfExists(temporary);
                } catch (IOException cleanupFailure) {
                    if (failure != null) {
                        failure.addSuppressed(cleanupFailure);
                    } else {
                        throw cleanupFailure;
                    }
                }
            }
        }
    }
}
