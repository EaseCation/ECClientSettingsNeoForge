package net.easecation.clientsettings.feature.blockoutline;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.easecation.clientsettings.ECClientSettings;
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

public final class BlockOutlineRenderer {

    private static final BlockOutlineController CONTROLLER = new BlockOutlineController();

    private BlockOutlineRenderer() {
    }

    public static void onRenderHighlight(RenderHighlightEvent.Block event) {
        try {
            boolean rendered = CONTROLLER.tryRender(
                    ProfileServices.active().features().blockOutline(),
                    color -> render(event, color)
            );
            if (rendered) {
                event.setCanceled(true);
            }
        } catch (RuntimeException exception) {
            ECClientSettings.LOGGER.error("Could not render custom block outline; using vanilla fallback", exception);
        }
    }

    private static boolean render(RenderHighlightEvent.Block event, int color) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        Camera camera = event.getCamera();
        Entity cameraEntity = camera == null ? null : camera.getEntity();
        if (level == null || cameraEntity == null) {
            return false;
        }

        BlockPos position = event.getTarget().getBlockPos();
        BlockState state = level.getBlockState(position);
        if (state.isAir() || !level.getWorldBorder().isWithinBounds(position)) {
            return false;
        }
        boolean translucentPass = ClientHooks.isInTranslucentBlockOutlinePass(level, position, state);
        if (translucentPass != event.isForTranslucentBlocks()) {
            return false;
        }

        VoxelShape shape = state.getShape(level, position, CollisionContext.of(cameraEntity));
        if (shape.isEmpty()) {
            return false;
        }
        MultiBufferSource source = event.getMultiBufferSource();
        if (!(source instanceof MultiBufferSource.BufferSource buffers)) {
            return false;
        }

        Vec3 cameraPosition = camera.getPosition();
        double offsetX = position.getX() - cameraPosition.x;
        double offsetY = position.getY() - cameraPosition.y;
        double offsetZ = position.getZ() - cameraPosition.z;
        if (minecraft.options.highContrastBlockOutline().get()) {
            VertexConsumer secondary = buffers.getBuffer(RenderType.secondaryBlockOutline());
            ShapeRenderer.renderShape(
                    event.getPoseStack(), secondary, shape, offsetX, offsetY, offsetZ, 0xFF000000
            );
        }
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        ShapeRenderer.renderShape(event.getPoseStack(), lines, shape, offsetX, offsetY, offsetZ, color);
        buffers.endLastBatch();
        return true;
    }
}
