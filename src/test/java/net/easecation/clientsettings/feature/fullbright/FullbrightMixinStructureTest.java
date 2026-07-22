package net.easecation.clientsettings.feature.fullbright;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FullbrightMixinStructureTest {

    @Test
    void hasExactlyTwoRequiredBoundedLocalValueInjections() throws IOException {
        ClassNode mixin = readClass("net/easecation/clientsettings/mixin/render/LightTextureMixin.class");
        List<AnnotationNode> injections = mixin.methods.stream()
                .flatMap(method -> annotations(method).stream())
                .filter(annotation -> annotation.desc.equals(
                        "Lorg/spongepowered/asm/mixin/injection/ModifyVariable;"
                ))
                .toList();

        assertEquals(2, injections.size());
        assertEquals(
                Set.of(10, 15),
                injections.stream().map(annotation -> (Integer) value(annotation, "index")).collect(java.util.stream.Collectors.toSet())
        );
        for (AnnotationNode injection : injections) {
            assertEquals(1, value(injection, "require"));
            assertTrue(((List<?>) value(injection, "method")).contains("updateLightTexture(F)V"));
            AnnotationNode at = (AnnotationNode) value(injection, "at");
            assertEquals("LOAD", value(at, "value"));
            AnnotationNode slice = (AnnotationNode) value(injection, "slice");
            assertNotNull(value(slice, "from"));
            assertNotNull(value(slice, "to"));
        }
    }

    @Test
    void implementationDoesNotMutateOptionsOrEffects() throws IOException {
        List<ClassNode> classes = List.of(
                readClass("net/easecation/clientsettings/mixin/render/LightTextureMixin.class"),
                readClass("net/easecation/clientsettings/feature/fullbright/FullbrightController.class")
        );
        List<MethodInsnNode> calls = classes.stream()
                .flatMap(node -> node.methods.stream())
                .flatMap(method -> method.instructions.iterator().hasNext()
                        ? java.util.stream.StreamSupport.stream(
                                java.util.Spliterators.spliteratorUnknownSize(method.instructions.iterator(), 0),
                                false
                        )
                        : java.util.stream.Stream.empty())
                .filter(MethodInsnNode.class::isInstance)
                .map(MethodInsnNode.class::cast)
                .toList();

        assertFalse(calls.stream().anyMatch(call ->
                call.owner.equals("net/minecraft/client/Options")
                        || call.name.equals("addEffect")
                        || call.name.equals("removeEffect")
                        || call.name.equals("removeAllEffects")
        ));
    }

    private ClassNode readClass(String resource) throws IOException {
        try (var input = getClass().getClassLoader().getResourceAsStream(resource)) {
            assertNotNull(input);
            ClassNode node = new ClassNode();
            new ClassReader(input).accept(node, 0);
            return node;
        }
    }

    private static List<AnnotationNode> annotations(MethodNode method) {
        List<AnnotationNode> annotations = new ArrayList<>();
        if (method.visibleAnnotations != null) {
            annotations.addAll(method.visibleAnnotations);
        }
        if (method.invisibleAnnotations != null) {
            annotations.addAll(method.invisibleAnnotations);
        }
        return annotations;
    }

    private static Object value(AnnotationNode annotation, String key) {
        for (int index = 0; index < annotation.values.size(); index += 2) {
            if (key.equals(annotation.values.get(index))) {
                return annotation.values.get(index + 1);
            }
        }
        throw new AssertionError("Missing annotation value: " + key);
    }
}
