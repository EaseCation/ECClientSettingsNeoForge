package net.easecation.clientsettings.feature.blockoutline;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class BlockOutlineStructureTest {

    @Test
    void featureDoesNotRegisterAMixin() throws IOException {
        try (var input = getClass().getClassLoader().getResourceAsStream("ecclientsettings.mixins.json")) {
            assertNotNull(input);
            String mixinConfiguration = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            assertFalse(mixinConfiguration.toLowerCase().contains("blockoutline"));
        }
    }
}
