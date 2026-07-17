package net.easecation.clientsettings.feature.timechanger;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimeChangerMixinStructureTest {

    private static final String TIME_TARGET = "Lnet/minecraft/client/multiplayer/ClientLevel;setTimeFromServer(JJZ)V";

    @Test
    void usesOneRequiredArgumentAdapterAtTheExactClientTimeCall() throws IOException {
        ClassNode mixin = readMixin();
        List<AnnotationNode> injections = mixin.methods.stream()
                .flatMap(method -> annotations(method).stream())
                .filter(annotation -> annotation.desc.equals(
                        "Lorg/spongepowered/asm/mixin/injection/ModifyArgs;"
                ))
                .toList();

        assertEquals(1, injections.size());
        AnnotationNode injection = injections.getFirst();
        assertEquals(1, value(injection, "require"));
        assertTrue(((List<?>) value(injection, "method")).contains(
                "handleSetTime(Lnet/minecraft/network/protocol/game/ClientboundSetTimePacket;)V"
        ));
        AnnotationNode at = (AnnotationNode) value(injection, "at");
        assertEquals("INVOKE", value(at, "value"));
        assertEquals(TIME_TARGET, value(at, "target"));
    }

    private ClassNode readMixin() throws IOException {
        String resource = "net/easecation/clientsettings/mixin/client/ClientPacketListenerMixin.class";
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

    private static Object value(AnnotationNode annotation, String key) {
        for (int index = 0; index < annotation.values.size(); index += 2) {
            if (key.equals(annotation.values.get(index))) {
                return annotation.values.get(index + 1);
            }
        }
        throw new AssertionError("Missing annotation value: " + key);
    }
}
