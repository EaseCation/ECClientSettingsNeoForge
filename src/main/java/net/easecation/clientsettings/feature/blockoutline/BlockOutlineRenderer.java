package net.easecation.clientsettings.feature.blockoutline;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.easecation.clientsettings.ECClientSettings;
import net.easecation.clientsettings.profile.model.BlockOutlineSettings;
import net.easecation.clientsettings.profile.runtime.ProfileServices;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;
import org.joml.Matrix4f;

public final class BlockOutlineRenderer {

    private static final BlockOutlineController CONTROLLER = new BlockOutlineController();
    private static final double FILL_EPSILON = 0.00005D;

    private BlockOutlineRenderer() {
    }

    public static void onRenderHighlight(RenderHighlightEvent.Block event) {
        try {
            BlockOutlineSettings settings = ProfileServices.active().features().blockOutline();
            if (!settings.enabled() && !settings.fillEnabled()) {
                return;
            }
            RenderContext context = resolve(event);
            if (context == null) {
                return;
            }
            boolean rendered = CONTROLLER.tryRender(
                    settings,
                    color -> renderOutline(context, color),
                    color -> renderFill(context, color)
            );
            if (rendered) {
                context.buffers().endLastBatch();
                event.setCanceled(true);
            }
        } catch (RuntimeException exception) {
            ECClientSettings.LOGGER.error("Could not render custom block selection; using vanilla fallback", exception);
        }
    }

    private static RenderContext resolve(RenderHighlightEvent.Block event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        Camera camera = event.getCamera();
        Entity cameraEntity = camera == null ? null : camera.getEntity();
        if (level == null || cameraEntity == null) {
            return null;
        }

        BlockPos position = event.getTarget().getBlockPos();
        BlockState state = level.getBlockState(position);
        if (state.isAir() || !level.getWorldBorder().isWithinBounds(position)) {
            return null;
        }
        boolean translucentPass = ClientHooks.isInTranslucentBlockOutlinePass(level, position, state);
        if (translucentPass != event.isForTranslucentBlocks()) {
            return null;
        }

        VoxelShape shape = state.getShape(level, position, CollisionContext.of(cameraEntity));
        if (shape.isEmpty()) {
            return null;
        }
        MultiBufferSource source = event.getMultiBufferSource();
        if (!(source instanceof MultiBufferSource.BufferSource buffers)) {
            return null;
        }

        Vec3 cameraPosition = camera.getPosition();
        return new RenderContext(
                event.getPoseStack(),
                buffers,
                shape,
                position.getX() - cameraPosition.x,
                position.getY() - cameraPosition.y,
                position.getZ() - cameraPosition.z
        );
    }

    private static boolean renderOutline(RenderContext context, int color) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.highContrastBlockOutline().get()) {
            VertexConsumer secondary = context.buffers().getBuffer(RenderType.secondaryBlockOutline());
            ShapeRenderer.renderShape(
                    context.poseStack(),
                    secondary,
                    context.shape(),
                    context.offsetX(),
                    context.offsetY(),
                    context.offsetZ(),
                    0xFF000000
            );
        }
        VertexConsumer lines = context.buffers().getBuffer(RenderType.lines());
        ShapeRenderer.renderShape(
                context.poseStack(),
                lines,
                context.shape(),
                context.offsetX(),
                context.offsetY(),
                context.offsetZ(),
                color
        );
        return true;
    }

    private static boolean renderFill(RenderContext context, int color) {
        VertexConsumer fill = context.buffers().getBuffer(RenderType.debugStructureQuads());
        Matrix4f matrix = context.poseStack().last().pose();
        context.shape().forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> {
            float x1 = (float) (minX - FILL_EPSILON + context.offsetX());
            float y1 = (float) (minY - FILL_EPSILON + context.offsetY());
            float z1 = (float) (minZ - FILL_EPSILON + context.offsetZ());
            float x2 = (float) (maxX + FILL_EPSILON + context.offsetX());
            float y2 = (float) (maxY + FILL_EPSILON + context.offsetY());
            float z2 = (float) (maxZ + FILL_EPSILON + context.offsetZ());
            addQuad(fill, matrix, x2, y1, z1, x1, y1, z1, x1, y2, z1, x2, y2, z1, color);
            addQuad(fill, matrix, x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2, color);
            addQuad(fill, matrix, x1, y2, z2, x1, y2, z1, x1, y1, z1, x1, y1, z2, color);
            addQuad(fill, matrix, x2, y2, z1, x2, y2, z2, x2, y1, z2, x2, y1, z1, color);
            addQuad(fill, matrix, x2, y1, z1, x2, y1, z2, x1, y1, z2, x1, y1, z1, color);
            addQuad(fill, matrix, x2, y2, z2, x2, y2, z1, x1, y2, z1, x1, y2, z2, color);
        });
        return true;
    }

    private static void addQuad(
            VertexConsumer buffer,
            Matrix4f matrix,
            float x1,
            float y1,
            float z1,
            float x2,
            float y2,
            float z2,
            float x3,
            float y3,
            float z3,
            float x4,
            float y4,
            float z4,
            int color
    ) {
        buffer.addVertex(matrix, x1, y1, z1).setColor(color);
        buffer.addVertex(matrix, x2, y2, z2).setColor(color);
        buffer.addVertex(matrix, x3, y3, z3).setColor(color);
        buffer.addVertex(matrix, x4, y4, z4).setColor(color);
    }

    private record RenderContext(
            PoseStack poseStack,
            MultiBufferSource.BufferSource buffers,
            VoxelShape shape,
            double offsetX,
            double offsetY,
            double offsetZ
    ) {
    }
}
