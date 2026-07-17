package net.easecation.clientsettings.feature.hitcolor;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HitColorStructureTest {

    private static final String ACCESSOR_CLASS =
            "net/easecation/clientsettings/mixin/render/OverlayTextureAccessor.class";

    @Test
    void mixinIsOnlyAReadOnlyOverlayTextureAccessor() throws IOException {
        ClassNode accessor = readClass(ACCESSOR_CLASS);
        assertTrue((accessor.access & Opcodes.ACC_INTERFACE) != 0);
        assertEquals(1, accessor.methods.size());

        MethodNode method = accessor.methods.getFirst();
        assertEquals("ecclientsettings$getTexture", method.name);
        assertEquals("()Lnet/minecraft/client/renderer/texture/DynamicTexture;", method.desc);
        assertEquals(1, annotationCount(annotations(method), "Lorg/spongepowered/asm/mixin/gen/Accessor;"));
        assertEquals(0, injectorAnnotationCount(annotations(method)));

        AnnotationNode mixin = annotation(annotations(accessor), "Lorg/spongepowered/asm/mixin/Mixin;");
        assertNotNull(mixin);
        assertEquals(
                List.of(Type.getObjectType("net/minecraft/client/renderer/texture/OverlayTexture")),
                annotationValue(mixin, "value")
        );
    }

    @Test
    void configurationRegistersTheAccessorExactlyOnce() throws IOException {
        try (var input = getClass().getClassLoader().getResourceAsStream("ecclientsettings.mixins.json")) {
            assertNotNull(input);
            String configuration = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            assertEquals(1, occurrences(configuration, "render.OverlayTextureAccessor"));
        }
    }

    @Test
    void hitColorClassesDoNotReferenceEntityRenderOrAttackState() throws IOException {
        for (String resource : List.of(
                ACCESSOR_CLASS,
                "net/easecation/clientsettings/feature/hitcolor/HitColorController.class",
                "net/easecation/clientsettings/feature/hitcolor/HitColorRuntime.class"
        )) {
            String constants = classBytes(resource);
            assertFalse(constants.contains("LivingEntityRenderer"));
            assertFalse(constants.contains("LivingEntityRenderState"));
            assertFalse(constants.contains("hurtTime"));
            assertFalse(constants.contains("deathTime"));
            assertFalse(constants.contains("Attack"));
        }
    }

    private ClassNode readClass(String resource) throws IOException {
        try (var input = getClass().getClassLoader().getResourceAsStream(resource)) {
            assertNotNull(input);
            ClassNode node = new ClassNode();
            new ClassReader(input).accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            return node;
        }
    }

    private String classBytes(String resource) throws IOException {
        try (var input = getClass().getClassLoader().getResourceAsStream(resource)) {
            assertNotNull(input);
            return new String(input.readAllBytes(), StandardCharsets.ISO_8859_1);
        }
    }

    private static AnnotationNode annotation(List<AnnotationNode> annotations, String descriptor) {
        if (annotations == null) {
            return null;
        }
        return annotations.stream().filter(candidate -> candidate.desc.equals(descriptor)).findFirst().orElse(null);
    }

    private static List<AnnotationNode> annotations(ClassNode node) {
        List<AnnotationNode> annotations = new ArrayList<>();
        if (node.visibleAnnotations != null) {
            annotations.addAll(node.visibleAnnotations);
        }
        if (node.invisibleAnnotations != null) {
            annotations.addAll(node.invisibleAnnotations);
        }
        return annotations;
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

    private static Object annotationValue(AnnotationNode annotation, String key) {
        for (int index = 0; index < annotation.values.size(); index += 2) {
            if (key.equals(annotation.values.get(index))) {
                return annotation.values.get(index + 1);
            }
        }
        throw new AssertionError("Missing annotation value: " + key);
    }

    private static long annotationCount(List<AnnotationNode> annotations, String descriptor) {
        return annotations == null ? 0 : annotations.stream().filter(annotation -> annotation.desc.equals(descriptor)).count();
    }

    private static long injectorAnnotationCount(List<AnnotationNode> annotations) {
        if (annotations == null) {
            return 0;
        }
        return annotations.stream()
                .filter(annotation -> annotation.desc.contains("/injection/")
                        && !annotation.desc.endsWith("/At;"))
                .count();
    }

    private static int occurrences(String value, String needle) {
        return (value.length() - value.replace(needle, "").length()) / needle.length();
    }
}
