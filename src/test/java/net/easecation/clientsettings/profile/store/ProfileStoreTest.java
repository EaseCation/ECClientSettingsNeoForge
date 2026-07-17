package net.easecation.clientsettings.profile.store;

import net.easecation.clientsettings.profile.model.ProfileDefinition;
import net.easecation.clientsettings.profile.model.ProfileFeatures;
import net.easecation.clientsettings.profile.model.ProfileIndex;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProfileStoreTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-07-17T12:34:56.789Z"), ZoneOffset.UTC);

    @TempDir
    Path temporaryDirectory;

    @Test
    void createsDefaultAndRecoversOrphanInDeterministicOrder() throws IOException {
        ProfileStore store = store();
        ProfileDefinition orphan = new ProfileDefinition(
                1,
                "62bd28b8-35ee-4cf4-a0ea-bd6637fca074",
                "Tournament",
                ProfileFeatures.DEFAULT
        );
        Files.createDirectories(temporaryDirectory.resolve("profiles"));
        Files.write(
                temporaryDirectory.resolve("profiles").resolve(orphan.id() + ".json"),
                new ProfileJsonCodec().encodeProfile(orphan)
        );

        ProfileLoadResult result = store.load(ProfileDefinition.defaults(false));

        assertEquals(List.of("default", orphan.id()), result.catalog().index().profileOrder());
        assertFalse(result.catalog().activeProfile().features().forceSprint().enabled());
        assertTrue(Files.isRegularFile(temporaryDirectory.resolve("profiles.json")));
    }

    @Test
    void quarantinesCorruptIndexAndProfileThenGeneratesDefault() throws IOException {
        Files.createDirectories(temporaryDirectory.resolve("profiles"));
        Files.writeString(temporaryDirectory.resolve("profiles.json"), "not json", StandardCharsets.UTF_8);
        Files.writeString(temporaryDirectory.resolve("profiles/default.json"), "[]", StandardCharsets.UTF_8);

        ProfileLoadResult result = store().load(ProfileDefinition.defaults(true));

        assertEquals("default", result.catalog().index().activeProfileId());
        assertEquals(4, result.warnings().size());
        try (var paths = Files.walk(temporaryDirectory)) {
            List<String> names = paths.map(path -> path.getFileName().toString()).toList();
            assertTrue(names.stream().anyMatch(name -> name.startsWith("profiles.json.broken-")));
            assertTrue(names.stream().anyMatch(name -> name.startsWith("default.json.broken-")));
        }
    }

    @Test
    void recoversDuplicateNamesWithoutChangingIds() throws IOException {
        ProfileStore store = store();
        store.load(ProfileDefinition.defaults(true));
        ProfileDefinition duplicate = new ProfileDefinition(
                1,
                "62bd28b8-35ee-4cf4-a0ea-bd6637fca074",
                "default",
                ProfileFeatures.DEFAULT
        );
        Files.write(
                temporaryDirectory.resolve("profiles").resolve(duplicate.id() + ".json"),
                new ProfileJsonCodec().encodeProfile(duplicate)
        );

        ProfileLoadResult result = store.load(ProfileDefinition.defaults(true));

        assertEquals("default (Recovered 1)", result.catalog().profile(duplicate.id()).name());
        assertEquals(duplicate.id(), result.catalog().profile(duplicate.id()).id());
    }

    @Test
    void missingActiveProfileFallsBackToDefaultAndRepairsIndex() throws IOException {
        ProfileStore store = store();
        ProfileLoadResult initial = store.load(ProfileDefinition.defaults(true));
        String missingId = "62bd28b8-35ee-4cf4-a0ea-bd6637fca074";
        ProfileIndex stale = new ProfileIndex(1, missingId, List.of("default", missingId));
        store.saveIndex(stale);

        ProfileLoadResult recovered = store.load(ProfileDefinition.defaults(false));

        assertEquals("default", recovered.catalog().index().activeProfileId());
        assertEquals(List.of("default"), recovered.catalog().index().profileOrder());
        assertTrue(initial.catalog().activeProfile().features().forceSprint().enabled());
    }

    @Test
    void newerSchemaIsUntouchedAndNeverRewritten() throws IOException {
        Path index = temporaryDirectory.resolve("profiles.json");
        Files.createDirectories(temporaryDirectory);
        String newer = "{\"schemaVersion\":2,\"activeProfileId\":\"default\","
                + "\"profileOrder\":[\"default\"]}";
        Files.writeString(index, newer, StandardCharsets.UTF_8);

        assertThrows(
                UnsupportedProfileSchemaException.class,
                () -> store().load(ProfileDefinition.defaults(true))
        );
        assertEquals(newer, Files.readString(index, StandardCharsets.UTF_8));
        try (var paths = Files.list(temporaryDirectory)) {
            assertFalse(paths.anyMatch(path -> path.getFileName().toString().contains("broken")));
        }
    }

    private ProfileStore store() {
        return new ProfileStore(
                temporaryDirectory,
                new ProfileJsonCodec(),
                new NioProfileFileOperations(),
                CLOCK
        );
    }
}
