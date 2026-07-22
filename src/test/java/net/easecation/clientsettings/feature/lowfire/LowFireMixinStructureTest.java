package net.easecation.clientsettings.feature.lowfire;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LowFireMixinStructureTest {

    private static final String FIRE_TARGET = "Lnet/minecraft/client/renderer/ScreenEffectRenderer;"
            + "renderFire(Lcom/mojang/blaze3d/vertex/PoseStack;"
            + "Lnet/minecraft/client/renderer/MultiBufferSource;)V";

    @Test
    void registersOneRequiredRedirectForTheExactFireCall() throws Exception {
        ClassNode mixin = readMixinClass();
        List<AnnotationNode> redirects = mixin.methods.stream()
                .flatMap(method -> annotations(method).stream())
                .filter(annotation -> annotation.desc.equals(
                        "Lorg/spongepowered/asm/mixin/injection/Redirect;"
                ))
                .toList();

        assertEquals(1, redirects.size());
        AnnotationNode redirect = redirects.getFirst();
        assertEquals(1, annotationValue(redirect, "require"));
        assertTrue(((List<?>) annotationValue(redirect, "method")).contains("renderScreenEffect"));
        AnnotationNode at = (AnnotationNode) annotationValue(redirect, "at");
        assertEquals("INVOKE", annotationValue(at, "value"));
        assertEquals(FIRE_TARGET, annotationValue(at, "target"));
    }

    @Test
    void mixinConfigurationContainsOneLowFireEntry() throws IOException {
        try (var input = getClass().getClassLoader().getResourceAsStream("ecclientsettings.mixins.json")) {
            assertNotNull(input);
            String configuration = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            assertEquals(1, occurrences(configuration, "render.ScreenEffectRendererMixin"));
        }
    }

    private static int occurrences(String value, String needle) {
        int count = 0;
        int index = 0;
        while ((index = value.indexOf(needle, index)) >= 0) {
            count++;
            index += needle.length();
        }
        return count;
    }

    private ClassNode readMixinClass() throws IOException {
        String resource = "net/easecation/clientsettings/mixin/render/ScreenEffectRendererMixin.class";
        try (var input = getClass().getClassLoader().getResourceAsStream(resource)) {
            assertNotNull(input);
            ClassNode node = new ClassNode();
            new ClassReader(input).accept(node, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
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

    private static Object annotationValue(AnnotationNode annotation, String key) {
        for (int index = 0; index < annotation.values.size(); index += 2) {
            if (key.equals(annotation.values.get(index))) {
                return annotation.values.get(index + 1);
            }
        }
        throw new AssertionError("Missing annotation value: " + key);
    }
}
