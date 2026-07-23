package net.easecation.clientsettings.feature.obsoverlay.nativehook;

import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Locale;

final class NativeLibraryExtractor {

    private static final String MINHOOK_X64_SHA256 =
            "bddd6adaee8ab13eabaa7c73c97718cee1437db2054ca713ec7cc86e8002a300";
    private static final String MINHOOK_X86_SHA256 =
            "d1db9afdc79dcd34f77d1eb825c4f95e37e7f72ca7bd0e717e69d275fd94093e";

    private NativeLibraryExtractor() {
    }

    static Path extractMinHook() throws IOException {
        String architecture = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
        if (architecture.contains("aarch") || architecture.contains("arm")) {
            throw new IOException("OBS overlay supports Windows x64/x86 only");
        }
        boolean x64 = architecture.equals("amd64")
                || architecture.equals("x86_64")
                || architecture.equals("x64");
        String resource = x64 ? "/native/obs-overlay/MinHook.x64.dll" : "/native/obs-overlay/MinHook.x86.dll";
        byte[] bundled;
        try (InputStream input = NativeLibraryExtractor.class.getResourceAsStream(resource)) {
            if (input == null) {
                throw new IOException("Bundled MinHook library is missing: " + resource);
            }
            bundled = input.readAllBytes();
        }
        String expectedHash = x64 ? MINHOOK_X64_SHA256 : MINHOOK_X86_SHA256;
        String actualHash = sha256(bundled);
        if (!expectedHash.equals(actualHash)) {
            throw new IOException("Bundled MinHook library failed its SHA-256 integrity check");
        }

        Path directory = FMLPaths.GAMEDIR.get().resolve("native").resolve("ecclientsettings");
        Files.createDirectories(directory);
        Path target = directory.resolve(x64
                ? "MinHook-1.3.3-obs-overlay-bddd6ada-x64.dll"
                : "MinHook-1.3.3-obs-overlay-d1db9afd-x86.dll");
        if (Files.isRegularFile(target) && Arrays.equals(Files.readAllBytes(target), bundled)) {
            return target;
        }

        Path temporary = Files.createTempFile(directory, "minhook-", ".tmp");
        try {
            Files.write(temporary, bundled);
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
        return target;
    }

    private static String sha256(byte[] bytes) throws IOException {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IOException("SHA-256 is unavailable", exception);
        }
    }
}
