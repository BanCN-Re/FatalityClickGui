package wtf.carnation.ui.fatality;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.ScissorState;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.CachedOrthoProjectionMatrixBuffer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Pose;
import org.joml.Matrix4fStack;
import org.joml.Quaternionf;
import wtf.carnation.Client;
import wtf.carnation.graphics.gpu.LazyFramebuffer;
import wtf.carnation.graphics.gpu.RenderDevice;
import wtf.carnation.graphics.gpu.StreamingUniformBuffer;

import java.util.OptionalInt;

/**
 * The FA preview's player: the local player rendered offscreen the way vanilla renders entities
 * into GUI pictures, then composited straight into the window with the ESP look applied. Doing
 * it immediately, instead of through GuiGraphics, keeps it in the window's draw order, so popups
 * cover it and the halo can be built from the model's real silhouette.
 */
final class FaPlayerModel {
    private static final Identifier VERTEX_SHADER = Identifier.fromNamespaceAndPath("client", "screen_triangle");
    private static final Identifier FRAGMENT_SHADER = Identifier.fromNamespaceAndPath("client", "fa_model");
    private static final int UNIFORM_BYTES = 4 * 16;
    private static final LazyFramebuffer TARGET = new LazyFramebuffer("FA Preview Model", true);

    private static CachedOrthoProjectionMatrixBuffer projection;
    private static RenderPipeline pipeline;
    private static StreamingUniformBuffer uniforms;
    private static boolean failed;

    private FaPlayerModel() {
    }

    /**
     * Draws into {@code box}, all in GUI coordinates. {@code clip} (x, y, w, h) bounds the
     * composite and may be null. Returns false when there is nothing to draw.
     */
    static boolean draw(FaDraw.PlayerView view, float[] box, float[] clip, float feetX, float feetY,
                        float height, float alpha) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || failed || mc.getWindow() == null) return false;
        RenderTarget main = RenderDevice.mainTarget();
        float sx = main.width / (float) mc.getWindow().getGuiScaledWidth();
        float sy = main.height / (float) mc.getWindow().getGuiScaledHeight();
        int x0 = (int) Math.floor(box[0] * sx), y0 = (int) Math.floor(box[1] * sy);
        int x1 = (int) Math.ceil((box[0] + box[2]) * sx), y1 = (int) Math.ceil((box[1] + box[3]) * sy);
        int width = x1 - x0, height1 = y1 - y0;
        if (width < 4 || height1 < 4 || width > 4096 || height1 > 4096) return false;

        int[] scissor = {x0, main.height - y1, width, height1};
        if (clip != null) {
            int cx0 = (int) Math.floor(clip[0] * sx), cy0 = (int) Math.floor(clip[1] * sy);
            int cx1 = (int) Math.ceil((clip[0] + clip[2]) * sx), cy1 = (int) Math.ceil((clip[1] + clip[3]) * sy);
            scissor = intersect(scissor, new int[]{cx0, main.height - cy1, cx1 - cx0, cy1 - cy0});
            if (scissor[2] <= 0 || scissor[3] <= 0) return true;
        }

        try {
            RenderTarget target = TARGET.ensure(width, height1);
            if (target.getColorTexture() == null || target.getDepthTexture() == null) return false;
            renderModel(mc, player, target, view, (feetX * sx) - x0, (feetY * sy) - y0, height * sy);
            composite(main, target, view, new int[]{x0, main.height - y1, width, height1}, scissor, sx, alpha);
            return true;
        } catch (RuntimeException failure) {
            // A broken preview must never take the GUI down with it; draw the stand-in from now on.
            failed = true;
            Client.logger.warn("FA preview model failed; disabling it for this session.", failure);
            return false;
        }
    }

    private static void renderModel(Minecraft mc, LocalPlayer player, RenderTarget target, FaDraw.PlayerView view,
                                    float feetX, float feetY, float modelHeight) {
        CommandEncoder encoder = RenderDevice.commandEncoder();
        encoder.clearColorAndDepthTextures(target.getColorTexture(), 0, target.getDepthTexture(), 1.0);

        EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
        EntityRenderer<? super LocalPlayer, ?> renderer = dispatcher.getRenderer(player);
        EntityRenderState state = renderer.createRenderState(player,
                mc.getDeltaTracker().getGameTimeDeltaPartialTick(false));
        state.lightCoords = 0xF000F0;
        state.shadowPieces.clear();
        state.outlineColor = 0;
        state.nameTag = null;
        float blocks = 1.8f;
        if (state instanceof LivingEntityRenderState living) {
            living.bodyRot = 180.0f + view.yaw();
            living.yRot = view.headYaw();
            living.xRot = living.pose == Pose.FALL_FLYING ? 0.0f : view.headPitch();
            living.boundingBoxWidth /= living.scale;
            living.boundingBoxHeight /= living.scale;
            living.scale = 1.0f;
            if (living.boundingBoxHeight > 0.1f) blocks = living.boundingBoxHeight;
        }
        float pixelsPerBlock = modelHeight / blocks;

        if (projection == null) projection = new CachedOrthoProjectionMatrixBuffer("FA Preview", -1000.0f, 1000.0f, true);
        RenderSystem.backupProjectionMatrix();
        Matrix4fStack modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        modelView.identity();
        // A screen-space scissor left on for render-type draws would cut the offscreen model.
        ScissorState scissor = RenderSystem.getScissorStateForRenderTypeDraws();
        int[] previousScissor = scissor.enabled()
                ? new int[]{scissor.x(), scissor.y(), scissor.width(), scissor.height()} : null;
        if (previousScissor != null) RenderSystem.disableScissorForRenderTypeDraws();
        try {
            RenderSystem.setProjectionMatrix(projection.getBuffer(target.width, target.height), ProjectionType.ORTHOGRAPHIC);
            RenderSystem.outputColorTextureOverride = target.getColorTextureView();
            RenderSystem.outputDepthTextureOverride = target.getDepthTextureView();
            mc.gameRenderer.getLighting().setupFor(Lighting.Entry.ENTITY_IN_UI);

            PoseStack pose = new PoseStack();
            pose.translate(feetX, feetY, 0.0f);
            pose.scale(pixelsPerBlock, pixelsPerBlock, -pixelsPerBlock);
            // GUI space points down; turning the model upside down puts its head up again.
            pose.mulPose(new Quaternionf().rotateZ((float) Math.PI));
            CameraRenderState camera = new CameraRenderState();
            camera.orientation = new Quaternionf().rotateY((float) Math.PI);
            FeatureRenderDispatcher features = mc.gameRenderer.getFeatureRenderDispatcher();
            dispatcher.submit(state, camera, 0.0, 0.0, 0.0, pose, features.getSubmitNodeStorage());
            features.renderAllFeatures();
            mc.renderBuffers().bufferSource().endBatch();
        } finally {
            RenderSystem.outputColorTextureOverride = null;
            RenderSystem.outputDepthTextureOverride = null;
            if (previousScissor != null) {
                RenderSystem.enableScissorForRenderTypeDraws(previousScissor[0], previousScissor[1],
                        previousScissor[2], previousScissor[3]);
            }
            modelView.popMatrix();
            RenderSystem.restoreProjectionMatrix();
            // The frame's GUI pass starts from item lighting; leave it that way.
            mc.gameRenderer.getLighting().setupFor(Lighting.Entry.ITEMS_3D);
        }
    }

    private static void composite(RenderTarget main, RenderTarget model, FaDraw.PlayerView view, int[] rect,
                                  int[] scissor, float pixelsPerUnit, float alpha) {
        ensureProgram();
        if (pipeline == null || uniforms == null || main.getColorTextureView() == null) return;
        CommandEncoder encoder = RenderDevice.commandEncoder();
        Std140Builder builder = Std140Builder.intoBuffer(uniforms.data());
        builder.putVec4(rect[0], rect[1], rect[2], rect[3]);
        putColor(builder, view.colorA());
        putColor(builder, view.colorB());
        builder.putVec4(view.esp(), view.glowRadius() * pixelsPerUnit, Math.max(0.0f, Math.min(1.0f, alpha)),
                Math.max(1.0f, pixelsPerUnit));
        GpuBufferSlice slice = uniforms.upload(encoder);
        try (RenderPass pass = RenderDevice.beginPass(encoder, () -> "FA Preview Model", main, OptionalInt.empty(), false)) {
            pass.setPipeline(pipeline);
            pass.enableScissor(scissor[0], scissor[1], scissor[2], scissor[3]);
            RenderDevice.bindDefaultUniforms(pass);
            pass.setUniform("FaModel", slice);
            pass.bindTexture("ModelSampler", model.getColorTextureView(), RenderDevice.sampler(FilterMode.LINEAR));
            pass.draw(0, 3);
        }
    }

    private static void putColor(Std140Builder builder, int argb) {
        builder.putVec4(((argb >> 16) & 255) / 255.0f, ((argb >> 8) & 255) / 255.0f, (argb & 255) / 255.0f,
                ((argb >>> 24) & 255) / 255.0f);
    }

    private static int[] intersect(int[] a, int[] b) {
        int x = Math.max(a[0], b[0]), y = Math.max(a[1], b[1]);
        int right = Math.min(a[0] + a[2], b[0] + b[2]), top = Math.min(a[1] + a[3], b[1] + b[3]);
        return new int[]{x, y, Math.max(0, right - x), Math.max(0, top - y)};
    }

    private static void ensureProgram() {
        if (uniforms == null) uniforms = new StreamingUniformBuffer("FA Preview Model", UNIFORM_BYTES);
        if (pipeline == null) {
            pipeline = RenderDevice.postProcessPipeline(
                    Identifier.fromNamespaceAndPath("client", "pipeline/fa_model"),
                    VERTEX_SHADER, FRAGMENT_SHADER, BlendFunction.TRANSLUCENT_PREMULTIPLIED_ALPHA,
                    new String[]{"FaModel"}, new String[]{"ModelSampler"});
        }
    }
}
