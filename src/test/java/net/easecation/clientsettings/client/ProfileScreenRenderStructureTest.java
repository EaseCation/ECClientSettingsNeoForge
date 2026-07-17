package net.easecation.clientsettings.client;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

import java.io.IOException;
import java.util.List;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void customProfileTextColorsAreOpaque() throws IOException {
        for (String resource : List.of(
                "net/easecation/clientsettings/client/ProfileManagementScreen.class",
                "net/easecation/clientsettings/client/ProfileManagementScreen$ProfileEntry.class",
                "net/easecation/clientsettings/client/ProfileNameScreen.class"
        )) {
            ClassNode screen = readClass(resource);
            List<MethodInsnNode> textCalls = screen.methods.stream()
                    .flatMap(method -> StreamSupport.stream(
                            java.util.Spliterators.spliteratorUnknownSize(method.instructions.iterator(), 0),
                            false
                    ))
                    .filter(MethodInsnNode.class::isInstance)
                    .map(MethodInsnNode.class::cast)
                    .filter(call -> call.owner.equals("net/minecraft/client/gui/GuiGraphics"))
                    .filter(call -> call.name.equals("drawString") || call.name.equals("drawCenteredString"))
                    .toList();

            assertFalse(textCalls.isEmpty(), resource + " must contain a text draw call");
            for (MethodInsnNode call : textCalls) {
                int color = integerConstantBefore(call);
                assertTrue(
                        (color >>> 24) != 0,
                        resource + " must pass an ARGB color with non-zero alpha to " + call.name
                );
            }
        }
    }

    private int integerConstantBefore(AbstractInsnNode instruction) {
        AbstractInsnNode previous = instruction.getPrevious();
        while (previous != null && previous.getOpcode() < 0) {
            previous = previous.getPrevious();
        }
        assertNotNull(previous);
        if (previous instanceof LdcInsnNode ldc && ldc.cst instanceof Integer value) {
            return value;
        }
        if (previous instanceof IntInsnNode integer) {
            return integer.operand;
        }
        if (previous instanceof InsnNode) {
            return switch (previous.getOpcode()) {
                case Opcodes.ICONST_M1 -> -1;
                case Opcodes.ICONST_0 -> 0;
                case Opcodes.ICONST_1 -> 1;
                case Opcodes.ICONST_2 -> 2;
                case Opcodes.ICONST_3 -> 3;
                case Opcodes.ICONST_4 -> 4;
                case Opcodes.ICONST_5 -> 5;
                default -> throw new AssertionError("Expected integer color constant before text draw call");
            };
        }
        throw new AssertionError("Expected integer color constant before text draw call");
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
