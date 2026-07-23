package net.easecation.clientsettings.feature.obsoverlay.nativehook;

import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.textures.GpuTexture;
import org.lwjgl.opengl.ARBDrawBuffersBlend;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL33;
import org.lwjgl.opengl.GL40;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

/** Draws privacy textures after OBS game capture and immediately before the real buffer swap. */
public final class RawOverlayCompositor implements AutoCloseable {

    private static final int MODE_OVER = 0;
    private static final int MODE_REPLACE = 1;

    private static final String VERTEX_SHADER = """
            #version 150 core
            out vec2 overlayUv;
            void main() {
                vec2 positions[3] = vec2[3](vec2(-1.0, -1.0), vec2(3.0, -1.0), vec2(-1.0, 3.0));
                vec2 position = positions[gl_VertexID];
                overlayUv = position * 0.5 + 0.5;
                gl_Position = vec4(position, 0.0, 1.0);
            }
            """;
    private static final String FRAGMENT_SHADER = """
            #version 150 core
            uniform sampler2D OverlayTexture;
            uniform sampler2D OverlayDepthTexture;
            uniform sampler2D SceneDepthTexture;
            uniform int DepthAware;
            uniform int CompositeMode;
            in vec2 overlayUv;
            out vec4 fragColor;
            void main() {
                vec4 overlay = texture(OverlayTexture, overlayUv);
                if (CompositeMode == 1) {
                    fragColor = overlay;
                    return;
                }
                if (overlay.a <= 0.0) {
                    discard;
                }
                if (DepthAware != 0) {
                    float overlayDepth = texture(OverlayDepthTexture, overlayUv).r;
                    float sceneDepth = texture(SceneDepthTexture, overlayUv).r;
                    if (overlayDepth > sceneDepth + 0.000001) {
                        discard;
                    }
                }
                fragColor = overlay;
            }
            """;

    private int program;
    private int vertexArray;
    private int depthAwareUniform;
    private int compositeModeUniform;

    public void composite(
            GpuTexture sceneDepthTexture,
            RenderTarget worldTarget,
            boolean worldDirty,
            RenderTarget guiTarget,
            boolean guiDirty,
            int width,
            int height
    ) {
        drawToBackBuffer(
                null,
                false,
                sceneDepthTexture,
                worldTarget,
                worldDirty,
                guiTarget,
                guiDirty,
                width,
                height
        );
    }

    /** Restores the clean Minecraft frame after OBS capture, then draws private overlays for the local window. */
    public void restoreAndComposite(
            RenderTarget mainTarget,
            boolean restoreMainTarget,
            GpuTexture sceneDepthTexture,
            RenderTarget worldTarget,
            boolean worldDirty,
            RenderTarget guiTarget,
            boolean guiDirty,
            int width,
            int height
    ) {
        drawToBackBuffer(
                mainTarget,
                restoreMainTarget,
                sceneDepthTexture,
                worldTarget,
                worldDirty,
                guiTarget,
                guiDirty,
                width,
                height
        );
    }

    private void drawToBackBuffer(
            RenderTarget replacementTarget,
            boolean replaceBackBuffer,
            GpuTexture sceneDepthTexture,
            RenderTarget worldTarget,
            boolean worldDirty,
            RenderTarget guiTarget,
            boolean guiDirty,
            int width,
            int height
    ) {
        if ((!replaceBackBuffer && !worldDirty && !guiDirty) || width <= 0 || height <= 0) {
            return;
        }
        if (replaceBackBuffer && replacementTarget == null) {
            throw new IllegalArgumentException("A replacement target is required to restore the back buffer");
        }
        ensureInitialized();

        try (MemoryStack stack = MemoryStack.stackPush()) {
            int previousProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
            int previousVertexArray = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
            int previousArrayBuffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
            int previousDrawFramebuffer = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
            int previousReadFramebuffer = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
            if (previousDrawFramebuffer != 0) {
                GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, 0);
            }
            int previousDefaultDrawBuffer = GL11.glGetInteger(GL11.GL_DRAW_BUFFER);
            if (previousDrawFramebuffer != 0) {
                GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, previousDrawFramebuffer);
            }
            int previousActiveTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
            int previousActiveBinding = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            IntBuffer viewport = stack.mallocInt(4);
            GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);

            boolean indexedBlend = supportsIndexedBlend();
            boolean blend = GL30.glIsEnabledi(GL11.GL_BLEND, 0);
            boolean depthTest = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
            boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
            boolean scissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
            boolean stencil = GL11.glIsEnabled(GL11.GL_STENCIL_TEST);
            boolean colorLogic = GL11.glIsEnabled(GL11.GL_COLOR_LOGIC_OP);
            boolean depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
            ByteBuffer colorMask = stack.malloc(4);
            GL30.glGetBooleani_v(GL11.GL_COLOR_WRITEMASK, 0, colorMask);
            int blendSourceRgb = getBlendParameter(indexedBlend, GL14.GL_BLEND_SRC_RGB);
            int blendDestinationRgb = getBlendParameter(indexedBlend, GL14.GL_BLEND_DST_RGB);
            int blendSourceAlpha = getBlendParameter(indexedBlend, GL14.GL_BLEND_SRC_ALPHA);
            int blendDestinationAlpha = getBlendParameter(indexedBlend, GL14.GL_BLEND_DST_ALPHA);
            int blendEquationRgb = getBlendParameter(indexedBlend, GL20.GL_BLEND_EQUATION_RGB);
            int blendEquationAlpha = getBlendParameter(indexedBlend, GL20.GL_BLEND_EQUATION_ALPHA);

            int[] previousTextures = new int[3];
            int[] previousSamplers = new int[3];
            for (int unit = 0; unit < previousTextures.length; unit++) {
                GL13.glActiveTexture(GL13.GL_TEXTURE0 + unit);
                previousTextures[unit] = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
                previousSamplers[unit] = GL11.glGetInteger(GL33.GL_SAMPLER_BINDING);
            }
            try {
                GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, 0);
                GL11.glDrawBuffer(GL11.GL_BACK);
                GL11.glViewport(0, 0, width, height);
                GL11.glDisable(GL11.GL_DEPTH_TEST);
                GL11.glDisable(GL11.GL_CULL_FACE);
                GL11.glDisable(GL11.GL_SCISSOR_TEST);
                GL11.glDisable(GL11.GL_STENCIL_TEST);
                GL11.glDisable(GL11.GL_COLOR_LOGIC_OP);
                GL11.glDepthMask(false);
                GL30.glColorMaski(0, true, true, true, true);
                GL20.glUseProgram(program);
                GL30.glBindVertexArray(vertexArray);

                if (replaceBackBuffer) {
                    // The clean main target must replace every back-buffer pixel, including alpha-zero pixels.
                    GL30.glDisablei(GL11.GL_BLEND, 0);
                    draw(replacementTarget, null, false, MODE_REPLACE);
                }

                if (worldDirty || guiDirty) {
                    GL30.glEnablei(GL11.GL_BLEND, 0);
                    // GUI and immediate world passes accumulate premultiplied color into transparent targets.
                    setBlendFunction(
                            indexedBlend,
                            GL11.GL_ONE,
                            GL11.GL_ONE_MINUS_SRC_ALPHA,
                            GL11.GL_ONE,
                            GL11.GL_ONE_MINUS_SRC_ALPHA
                    );
                    setBlendEquation(indexedBlend, GL14.GL_FUNC_ADD, GL14.GL_FUNC_ADD);
                }
                if (worldDirty) {
                    draw(worldTarget, sceneDepthTexture, true, MODE_OVER);
                }
                if (guiDirty) {
                    draw(guiTarget, sceneDepthTexture, false, MODE_OVER);
                }
            } finally {
                for (int unit = 0; unit < previousTextures.length; unit++) {
                    GL13.glActiveTexture(GL13.GL_TEXTURE0 + unit);
                    GL11.glBindTexture(GL11.GL_TEXTURE_2D, previousTextures[unit]);
                    GL33.glBindSampler(unit, previousSamplers[unit]);
                }
                GL13.glActiveTexture(previousActiveTexture);
                if (previousActiveTexture < GL13.GL_TEXTURE0
                        || previousActiveTexture >= GL13.GL_TEXTURE0 + previousTextures.length) {
                    GL11.glBindTexture(GL11.GL_TEXTURE_2D, previousActiveBinding);
                }
                GL20.glUseProgram(previousProgram);
                GL30.glBindVertexArray(previousVertexArray);
                GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, previousArrayBuffer);
                GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, 0);
                GL11.glDrawBuffer(previousDefaultDrawBuffer);
                GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, previousDrawFramebuffer);
                GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, previousReadFramebuffer);
                GL11.glViewport(viewport.get(0), viewport.get(1), viewport.get(2), viewport.get(3));
                setBlendFunction(
                        indexedBlend,
                        blendSourceRgb, blendDestinationRgb, blendSourceAlpha, blendDestinationAlpha
                );
                setBlendEquation(indexedBlend, blendEquationRgb, blendEquationAlpha);
                setEnabledIndexed(GL11.GL_BLEND, 0, blend);
                setEnabled(GL11.GL_DEPTH_TEST, depthTest);
                setEnabled(GL11.GL_CULL_FACE, cull);
                setEnabled(GL11.GL_SCISSOR_TEST, scissor);
                setEnabled(GL11.GL_STENCIL_TEST, stencil);
                setEnabled(GL11.GL_COLOR_LOGIC_OP, colorLogic);
                GL11.glDepthMask(depthMask);
                GL30.glColorMaski(
                        0,
                        colorMask.get(0) != 0,
                        colorMask.get(1) != 0,
                        colorMask.get(2) != 0,
                        colorMask.get(3) != 0
                );
            }
        }
    }

    private void draw(RenderTarget target, GpuTexture sceneDepthTexture, boolean depthAware, int compositeMode) {
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL33.glBindSampler(0, 0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId(target.getColorTexture()));
        boolean useDepth = depthAware
                && target.getDepthTexture() != null
                && sceneDepthTexture != null;
        if (useDepth) {
            GL13.glActiveTexture(GL13.GL_TEXTURE1);
            GL33.glBindSampler(1, 0);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId(target.getDepthTexture()));
            GL13.glActiveTexture(GL13.GL_TEXTURE2);
            GL33.glBindSampler(2, 0);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId(sceneDepthTexture));
        }
        GL20.glUniform1i(depthAwareUniform, useDepth ? 1 : 0);
        GL20.glUniform1i(compositeModeUniform, compositeMode);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 3);
    }

    private static int textureId(GpuTexture texture) {
        if (!(texture instanceof GlTexture glTexture)) {
            throw new IllegalStateException("OBS overlay requires the Minecraft OpenGL texture backend");
        }
        return glTexture.glId();
    }

    private void ensureInitialized() {
        if (program != 0) {
            return;
        }
        int vertexShader = compile(GL20.GL_VERTEX_SHADER, VERTEX_SHADER);
        int fragmentShader = compile(GL20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);
        program = GL20.glCreateProgram();
        GL20.glAttachShader(program, vertexShader);
        GL20.glAttachShader(program, fragmentShader);
        GL20.glLinkProgram(program);
        if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            String log = GL20.glGetProgramInfoLog(program);
            GL20.glDeleteProgram(program);
            program = 0;
            throw new IllegalStateException("Could not link OBS overlay compositor: " + log);
        }
        GL20.glDetachShader(program, vertexShader);
        GL20.glDetachShader(program, fragmentShader);
        GL20.glDeleteShader(vertexShader);
        GL20.glDeleteShader(fragmentShader);
        int previousProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        GL20.glUseProgram(program);
        GL20.glUniform1i(GL20.glGetUniformLocation(program, "OverlayTexture"), 0);
        GL20.glUniform1i(GL20.glGetUniformLocation(program, "OverlayDepthTexture"), 1);
        GL20.glUniform1i(GL20.glGetUniformLocation(program, "SceneDepthTexture"), 2);
        depthAwareUniform = GL20.glGetUniformLocation(program, "DepthAware");
        compositeModeUniform = GL20.glGetUniformLocation(program, "CompositeMode");
        GL20.glUseProgram(previousProgram);
        vertexArray = GL30.glGenVertexArrays();
    }

    private static int compile(int type, String source) {
        int shader = GL20.glCreateShader(type);
        GL20.glShaderSource(shader, source);
        GL20.glCompileShader(shader);
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            String log = GL20.glGetShaderInfoLog(shader);
            GL20.glDeleteShader(shader);
            throw new IllegalStateException("Could not compile OBS overlay compositor: " + log);
        }
        return shader;
    }

    private static void setEnabled(int capability, boolean enabled) {
        if (enabled) {
            GL11.glEnable(capability);
        } else {
            GL11.glDisable(capability);
        }
    }

    private static void setEnabledIndexed(int capability, int index, boolean enabled) {
        if (enabled) {
            GL30.glEnablei(capability, index);
        } else {
            GL30.glDisablei(capability, index);
        }
    }

    private static boolean supportsIndexedBlend() {
        return GL.getCapabilities().OpenGL40 || GL.getCapabilities().GL_ARB_draw_buffers_blend;
    }

    private static int getBlendParameter(boolean indexed, int parameter) {
        return indexed ? GL30.glGetIntegeri(parameter, 0) : GL11.glGetInteger(parameter);
    }

    private static void setBlendFunction(
            boolean indexed,
            int sourceRgb,
            int destinationRgb,
            int sourceAlpha,
            int destinationAlpha
    ) {
        if (!indexed) {
            GL14.glBlendFuncSeparate(sourceRgb, destinationRgb, sourceAlpha, destinationAlpha);
        } else if (GL.getCapabilities().OpenGL40) {
            GL40.glBlendFuncSeparatei(0, sourceRgb, destinationRgb, sourceAlpha, destinationAlpha);
        } else {
            ARBDrawBuffersBlend.glBlendFuncSeparateiARB(
                    0, sourceRgb, destinationRgb, sourceAlpha, destinationAlpha
            );
        }
    }

    private static void setBlendEquation(boolean indexed, int rgb, int alpha) {
        if (!indexed) {
            GL20.glBlendEquationSeparate(rgb, alpha);
        } else if (GL.getCapabilities().OpenGL40) {
            GL40.glBlendEquationSeparatei(0, rgb, alpha);
        } else {
            ARBDrawBuffersBlend.glBlendEquationSeparateiARB(0, rgb, alpha);
        }
    }

    @Override
    public void close() {
        if (vertexArray != 0) {
            GL30.glDeleteVertexArrays(vertexArray);
            vertexArray = 0;
        }
        if (program != 0) {
            GL20.glDeleteProgram(program);
            program = 0;
            depthAwareUniform = 0;
            compositeModeUniform = 0;
        }
    }
}
