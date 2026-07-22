package net.easecation.clientsettings.profile.store;

import net.easecation.clientsettings.profile.model.ProfileDefinition;
import net.easecation.clientsettings.profile.model.ProfileIndex;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class ProfileStore {

    private static final DateTimeFormatter RECOVERY_TIMESTAMP = DateTimeFormatter
            .ofPattern("uuuuMMdd-HHmmss-SSS", Locale.ROOT)
            .withZone(ZoneOffset.UTC);

    private final Path rootDirectory;
    private final Path profilesDirectory;
    private final Path indexPath;
    private final ProfileJsonCodec codec;
    private final AtomicProfileWriter writer;
    private final ProfileFileOperations operations;
    private final Clock clock;

    public ProfileStore(Path rootDirectory) {
        this(
                rootDirectory,
                new ProfileJsonCodec(),
                new NioProfileFileOperations(),
                Clock.systemUTC()
        );
    }

    public ProfileStore(
            Path rootDirectory,
            ProfileJsonCodec codec,
            ProfileFileOperations operations,
            Clock clock
    ) {
        this.rootDirectory = rootDirectory;
        this.profilesDirectory = rootDirectory.resolve("profiles");
        this.indexPath = rootDirectory.resolve("profiles.json");
        this.codec = codec;
        this.operations = operations;
        this.writer = new AtomicProfileWriter(operations);
        this.clock = clock;
    }

    public boolean indexExists() {
        return Files.isRegularFile(indexPath);
    }

    public ProfileLoadResult load(ProfileDefinition missingDefault) throws IOException {
        boolean hadPersistedState = hasPersistedState();
        operations.createDirectories(profilesDirectory);
        List<String> warnings = new ArrayList<>();
        ProfileIndex storedIndex = readIndex(warnings);
        Map<String, ProfileDefinition> discovered = readProfiles(warnings);

        ProfileDefinition defaultProfile = discovered.get(ProfileDefinition.DEFAULT_ID);
        if (defaultProfile == null) {
            defaultProfile = missingDefault;
            saveProfile(defaultProfile);
            discovered.put(defaultProfile.id(), defaultProfile);
            if (hadPersistedState) {
                warnings.add("Generated a safe Default Profile because it was missing or invalid");
            }
        }

        List<String> order = recoveredOrder(storedIndex, discovered);
        LinkedHashMap<String, ProfileDefinition> orderedProfiles = new LinkedHashMap<>();
        Set<String> usedNames = new HashSet<>();
        for (String profileId : order) {
            ProfileDefinition profile = discovered.get(profileId);
            ProfileDefinition unique = recoverUniqueName(profile, usedNames);
            if (unique != profile) {
                saveProfile(unique);
                warnings.add("Renamed duplicate Profile '" + profile.name() + "' to '" + unique.name() + "'");
            }
            orderedProfiles.put(profileId, unique);
        }

        String activeId = storedIndex != null && orderedProfiles.containsKey(storedIndex.activeProfileId())
                ? storedIndex.activeProfileId()
                : ProfileDefinition.DEFAULT_ID;
        ProfileIndex recoveredIndex = new ProfileIndex(
                ProfileDefinition.CURRENT_SCHEMA_VERSION,
                activeId,
                List.copyOf(orderedProfiles.keySet())
        );
        if (!recoveredIndex.equals(storedIndex)) {
            saveIndex(recoveredIndex);
            if (hadPersistedState) {
                warnings.add("Rebuilt the Profile index from valid Profile files");
            }
        }
        return new ProfileLoadResult(new ProfileCatalog(recoveredIndex, orderedProfiles), warnings);
    }

    private boolean hasPersistedState() throws IOException {
        if (Files.isRegularFile(indexPath)) {
            return true;
        }
        if (!Files.isDirectory(profilesDirectory)) {
            return false;
        }
        try (var paths = Files.list(profilesDirectory)) {
            return paths.anyMatch(path -> Files.isRegularFile(path)
                    && path.getFileName().toString().endsWith(".json"));
        }
    }

    public void saveProfile(ProfileDefinition profile) throws IOException {
        writer.write(profilePath(profile.id()), codec.encodeProfile(profile));
    }

    public void saveIndex(ProfileIndex index) throws IOException {
        writer.write(indexPath, codec.encodeIndex(index));
    }

    public void create(ProfileDefinition profile, ProfileIndex updatedIndex) throws IOException {
        Path path = profilePath(profile.id());
        if (Files.exists(path)) {
            throw new IOException("Profile already exists: " + profile.id());
        }
        saveProfile(profile);
        saveIndex(updatedIndex);
    }

    public void delete(ProfileDefinition profile, ProfileIndex previousIndex, ProfileIndex updatedIndex) throws IOException {
        if (profile.isDefault()) {
            throw new IllegalArgumentException("Default Profile cannot be deleted");
        }
        saveIndex(updatedIndex);
        Path source = profilePath(profile.id());
        Path recovery = recoveryPath(source, "deleted");
        try {
            operations.move(source, recovery);
        } catch (IOException exception) {
            try {
                saveIndex(previousIndex);
            } catch (IOException rollbackFailure) {
                exception.addSuppressed(rollbackFailure);
            }
            throw exception;
        }
    }

    public Path rootDirectory() {
        return rootDirectory;
    }

    private ProfileIndex readIndex(List<String> warnings) throws IOException {
        if (!Files.isRegularFile(indexPath)) {
            return null;
        }
        try {
            return codec.decodeIndex(Files.readAllBytes(indexPath));
        } catch (UnsupportedProfileSchemaException exception) {
            throw exception;
        } catch (IOException exception) {
            Path recovery = quarantine(indexPath, "broken");
            warnings.add("Preserved invalid Profile index as " + recovery.getFileName());
            return null;
        }
    }

    private Map<String, ProfileDefinition> readProfiles(List<String> warnings) throws IOException {
        LinkedHashMap<String, ProfileDefinition> profiles = new LinkedHashMap<>();
        List<Path> candidates;
        try (var paths = Files.list(profilesDirectory)) {
            candidates = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .toList();
        }
        for (Path path : candidates) {
            try {
                ProfileDefinition profile = codec.decodeProfile(Files.readAllBytes(path));
                if (!profilePath(profile.id()).getFileName().equals(path.getFileName())) {
                    throw new IOException("Profile ID does not match file name");
                }
                if (profiles.putIfAbsent(profile.id(), profile) != null) {
                    throw new IOException("Duplicate Profile ID: " + profile.id());
                }
            } catch (UnsupportedProfileSchemaException exception) {
                throw exception;
            } catch (IOException | IllegalArgumentException exception) {
                Path recovery = quarantine(path, "broken");
                warnings.add("Preserved invalid Profile as " + recovery.getFileName());
            }
        }
        return profiles;
    }

    private List<String> recoveredOrder(ProfileIndex index, Map<String, ProfileDefinition> profiles) {
        List<String> order = new ArrayList<>();
        if (index != null) {
            for (String profileId : index.profileOrder()) {
                if (profiles.containsKey(profileId)) {
                    order.add(profileId);
                }
            }
        }
        profiles.keySet().stream()
                .filter(profileId -> !order.contains(profileId))
                .sorted()
                .forEach(order::add);
        order.remove(ProfileDefinition.DEFAULT_ID);
        order.addFirst(ProfileDefinition.DEFAULT_ID);
        return order;
    }

    private ProfileDefinition recoverUniqueName(ProfileDefinition profile, Set<String> usedNames) {
        String normalized = profile.name().toLowerCase(Locale.ROOT);
        if (usedNames.add(normalized)) {
            return profile;
        }
        int suffix = 1;
        while (true) {
            String candidate = recoveredName(profile.name(), suffix++);
            if (usedNames.add(candidate.toLowerCase(Locale.ROOT))) {
                return profile.withName(candidate);
            }
        }
    }

    private String recoveredName(String original, int suffix) {
        String marker = " (Recovered " + suffix + ")";
        int allowedCodePoints = ProfileDefinition.MAX_NAME_CODE_POINTS - marker.codePointCount(0, marker.length());
        int originalCodePoints = original.codePointCount(0, original.length());
        String prefix = original;
        if (originalCodePoints > allowedCodePoints) {
            prefix = original.substring(0, original.offsetByCodePoints(0, allowedCodePoints));
        }
        return prefix + marker;
    }

    private Path quarantine(Path path, String reason) throws IOException {
        Path target = recoveryPath(path, reason);
        operations.move(path, target);
        return target;
    }

    private Path recoveryPath(Path path, String reason) {
        String timestamp = RECOVERY_TIMESTAMP.format(Instant.now(clock));
        Path candidate = path.resolveSibling(path.getFileName() + "." + reason + "-" + timestamp);
        int suffix = 1;
        while (Files.exists(candidate)) {
            candidate = path.resolveSibling(path.getFileName() + "." + reason + "-" + timestamp + "-" + suffix++);
        }
        return candidate;
    }

    private Path profilePath(String profileId) {
        ProfileDefinition.validateId(profileId);
        return profilesDirectory.resolve(profileId + ".json");
    }
}
