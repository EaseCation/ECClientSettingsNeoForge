package net.easecation.clientsettings.profile.store;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AtomicProfileWriterTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void failedAtomicMoveKeepsPreviousFile() throws IOException {
        Path target = temporaryDirectory.resolve("profile.json");
        Files.writeString(target, "previous", StandardCharsets.UTF_8);
        NioProfileFileOperations delegate = new NioProfileFileOperations();
        ProfileFileOperations failingMove = new ProfileFileOperations() {
            @Override
            public void createDirectories(Path directory) throws IOException {
                delegate.createDirectories(directory);
            }

            @Override
            public FileChannel openFile(Path path, OpenOption... options) throws IOException {
                return delegate.openFile(path, options);
            }

            @Override
            public void move(Path source, Path destination, CopyOption... options) throws IOException {
                throw new IOException("simulated move failure");
            }

            @Override
            public void deleteIfExists(Path path) throws IOException {
                delegate.deleteIfExists(path);
            }

            @Override
            public void forceDirectory(Path directory) throws IOException {
                delegate.forceDirectory(directory);
            }
        };

        AtomicProfileWriter writer = new AtomicProfileWriter(failingMove);
        assertThrows(IOException.class, () -> writer.write(target, "replacement".getBytes(StandardCharsets.UTF_8)));

        assertEquals("previous", Files.readString(target, StandardCharsets.UTF_8));
        try (var paths = Files.list(temporaryDirectory)) {
            assertEquals(List.of(target), paths.toList());
        }
    }

    @Test
    void failedWriteOpenKeepsPreviousFile() throws IOException {
        Path target = temporaryDirectory.resolve("profile.json");
        Files.writeString(target, "previous", StandardCharsets.UTF_8);
        NioProfileFileOperations delegate = new NioProfileFileOperations();
        ProfileFileOperations failingWrite = new ProfileFileOperations() {
            @Override
            public void createDirectories(Path directory) throws IOException {
                delegate.createDirectories(directory);
            }

            @Override
            public FileChannel openFile(Path path, OpenOption... options) throws IOException {
                throw new IOException("simulated write-open failure");
            }

            @Override
            public void move(Path source, Path destination, CopyOption... options) throws IOException {
                delegate.move(source, destination, options);
            }

            @Override
            public void deleteIfExists(Path path) throws IOException {
                delegate.deleteIfExists(path);
            }

            @Override
            public void forceDirectory(Path directory) throws IOException {
                delegate.forceDirectory(directory);
            }
        };

        assertThrows(
                IOException.class,
                () -> new AtomicProfileWriter(failingWrite).write(
                        target,
                        "replacement".getBytes(StandardCharsets.UTF_8)
                )
        );
        assertEquals("previous", Files.readString(target, StandardCharsets.UTF_8));
    }
}
