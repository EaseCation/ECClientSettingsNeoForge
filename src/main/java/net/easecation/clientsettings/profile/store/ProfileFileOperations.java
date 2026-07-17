package net.easecation.clientsettings.profile.store;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.CopyOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;

public interface ProfileFileOperations {

    void createDirectories(Path directory) throws IOException;

    FileChannel openFile(Path path, OpenOption... options) throws IOException;

    void move(Path source, Path target, CopyOption... options) throws IOException;

    void deleteIfExists(Path path) throws IOException;

    void forceDirectory(Path directory) throws IOException;
}
