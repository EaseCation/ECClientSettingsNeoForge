package net.easecation.clientsettings.profile.store;

import net.easecation.clientsettings.profile.model.ProfileDefinition;
import net.easecation.clientsettings.profile.model.ProfileIndex;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProfileJsonCodecTest {

    private final ProfileJsonCodec codec = new ProfileJsonCodec();

    @Test
    void roundTripsProfileAndIndex() throws IOException {
        ProfileDefinition profile = ProfileDefinition.defaults(false).withName("  Tournament  ");
        ProfileIndex index = new ProfileIndex(1, "default", List.of("default"));

        assertEquals(profile, codec.decodeProfile(codec.encodeProfile(profile)));
        assertEquals(index, codec.decodeIndex(codec.encodeIndex(index)));
    }

    @Test
    void rejectsUnknownFieldsAndFractionalIntegers() {
        String unknown = new String(codec.encodeProfile(ProfileDefinition.defaults(true)), StandardCharsets.UTF_8)
                .replace("\"name\": \"Default\"", "\"name\": \"Default\", \"future\": true");
        String fractionalSchema = "{\"schemaVersion\":1.5,\"activeProfileId\":\"default\","
                + "\"profileOrder\":[\"default\"]}";

        assertThrows(IOException.class, () -> codec.decodeProfile(unknown.getBytes(StandardCharsets.UTF_8)));
        assertThrows(IOException.class, () -> codec.decodeIndex(fractionalSchema.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void distinguishesNewerSchemaFromCorruption() {
        String newer = "{\"schemaVersion\":2,\"activeProfileId\":\"default\","
                + "\"profileOrder\":[\"default\"]}";

        UnsupportedProfileSchemaException exception = assertThrows(
                UnsupportedProfileSchemaException.class,
                () -> codec.decodeIndex(newer.getBytes(StandardCharsets.UTF_8))
        );
        assertEquals(2, exception.schemaVersion());
    }
}
