package net.easecation.clientsettings.profile.store;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public final class NioProfileFileOperations implements ProfileFileOperations {

    @Override
    public void createDirectories(Path directory) throws IOException {
        Files.createDirectories(directory);
    }

    @Override
    public FileChannel openFile(Path path, OpenOption... options) throws IOException {
        return FileChannel.open(path, options);
    }

    @Override
    public void move(Path source, Path target, CopyOption... options) throws IOException {
        Files.move(source, target, options);
    }

    @Override
    public void deleteIfExists(Path path) throws IOException {
        Files.deleteIfExists(path);
    }

    @Override
    public void forceDirectory(Path directory) throws IOException {
        try (FileChannel channel = FileChannel.open(directory, StandardOpenOption.READ)) {
            channel.force(true);
        }
    }
}
