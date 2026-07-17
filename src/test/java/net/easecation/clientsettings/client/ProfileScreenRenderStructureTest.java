package net.easecation.clientsettings.client;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;

import java.io.IOException;
import java.util.List;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ProfileScreenRenderStructureTest {

    @Test
    void customProfileScreensDoNotRenderTheirBackgroundTwice() throws IOException {
        for (String resource : List.of(
                "net/easecation/clientsettings/client/ProfileManagementScreen.class",
                "net/easecation/clientsettings/client/ProfileNameScreen.class"
        )) {
            ClassNode screen = readClass(resource);
            boolean callsRenderBackground = screen.methods.stream()
                    .filter(method -> method.name.equals("render"))
                    .flatMap(method -> StreamSupport.stream(
                            java.util.Spliterators.spliteratorUnknownSize(method.instructions.iterator(), 0),
                            false
                    ))
                    .filter(MethodInsnNode.class::isInstance)
                    .map(MethodInsnNode.class::cast)
                    .anyMatch(call -> call.name.equals("renderBackground"));

            assertFalse(
                    callsRenderBackground,
                    resource + " must rely on Screen.renderWithTooltip for its single background pass"
            );
        }
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
