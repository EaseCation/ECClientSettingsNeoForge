package net.easecation.clientsettings.feature.zoom;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ZoomStructureTest {

    @Test
    void zoomRegistersNoMixin() throws IOException {
        try (var input = getClass().getClassLoader().getResourceAsStream("ecclientsettings.mixins.json")) {
            assertNotNull(input);
            String configuration = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            assertFalse(configuration.toLowerCase().contains("zoom"));
        }
    }

    @Test
    void zoomNeverCallsOptionSetters() throws IOException {
        List<ClassNode> classes = List.of(
                readClass("net/easecation/clientsettings/feature/zoom/ZoomController.class"),
                readClass("net/easecation/clientsettings/feature/zoom/ZoomEvents.class")
        );
        boolean optionMutation = classes.stream()
                .flatMap(node -> node.methods.stream())
                .flatMap(method -> java.util.stream.StreamSupport.stream(
                        java.util.Spliterators.spliteratorUnknownSize(method.instructions.iterator(), 0),
                        false
                ))
                .filter(MethodInsnNode.class::isInstance)
                .map(MethodInsnNode.class::cast)
                .anyMatch(call -> call.owner.equals("net/minecraft/client/OptionInstance") && call.name.equals("set"));
        assertFalse(optionMutation);
    }

    private ClassNode readClass(String resource) throws IOException {
        try (var input = getClass().getClassLoader().getResourceAsStream(resource)) {
            assertNotNull(input);
            ClassNode node = new ClassNode();
            new ClassReader(input).accept(node, 0);
            return node;
        }
    }
}
