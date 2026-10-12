package wtf.carnation.ui.fatality;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import wtf.carnation.graphics.Renderer;
import wtf.carnation.graphics.canvas.Canvas;
import wtf.carnation.graphics.canvas.Paint;

import java.awt.Color;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * In-game drawing: every call maps onto the frame's Canvas. The transform, opacity and clip are
 * mirrored here as well, because the player model and items are drawn outside the Canvas and
 * need GUI coordinates.
 */
final class CanvasFaDraw extends FaDraw {
    private Canvas canvas;
    private GuiGraphics graphics;
    private boolean snap;
    private float tx, ty, s = 1.0f, alpha = 1.0f;
    private float[] clip;
    private final Deque<float[]> saved = new ArrayDeque<>();
    private final Deque<float[]> savedClips = new ArrayDeque<>();

    /** {@code snap} rounds text to whole units, which only lines up with pixels at scale 1. */
    void begin(Canvas canvas, GuiGraphics graphics, boolean snap) {
        this.canvas = canvas;
        this.graphics = graphics;
        this.snap = snap;
        tx = ty = 0.0f;
        s = alpha = 1.0f;
        clip = null;
        saved.clear();
        savedClips.clear();
    }

    @Override
    void rect(float x, float y, float w, float h, float radius, int argb) {
        if (w <= 0.0f || h <= 0.0f || (argb >>> 24) == 0) return;
        canvas.drawRoundRect(x, y, w, h, Math.max(0.0f, Math.min(radius, Math.min(w, h) * 0.5f)), Paint.fill(argb));
    }

    @Override
    void outline(float x, float y, float w, float h, float radius, float width, int argb) {
        if (w <= 0.0f || h <= 0.0f || (argb >>> 24) == 0) return;
        canvas.drawRoundRect(x, y, w, h, Math.max(0.0f, Math.min(radius, Math.min(w, h) * 0.5f)),
                Paint.NONE.withStroke(width, new Color(argb, true)));
    }

    @Override
    void quad(float x, float y, float w, float h, int topLeft, int topRight, int bottomRight, int bottomLeft) {
        if (w <= 0.0f || h <= 0.0f) return;
        canvas.drawQuad(x, y, w, h, topLeft, topRight, bottomRight, bottomLeft);
    }

    @Override
    void shadow(float x, float y, float w, float h, float radius, float sigma, int argb) {
        if (w <= 0.0f || h <= 0.0f) return;
        canvas.drawRoundRect(x, y, w, h, radius, Paint.shadow(sigma, new Color(argb, true)).ignoringEffectSettings());
    }

    @Override
    void text(String text, float x, float y, FaFont font, int argb) {
        if (text == null || text.isEmpty() || (argb >>> 24) == 0) return;
        float px = snap ? Math.round(x) : x, py = snap ? Math.round(y) : y;
        canvas.drawText(text, px, py, ((FaFont.Game) font).font(), argb);
    }

    @Override
    boolean player(float boxX, float boxY, float boxW, float boxH, float feetX, float feetY, float height,
                   PlayerView view) {
        float[] box = {gx(boxX), gy(boxY), boxW * s, boxH * s};
        float[] bounds = clip == null ? null : clip.clone();
        float fx = gx(feetX), fy = gy(feetY), h = height * s, a = alpha;
        boolean[] drawn = {false};
        canvas.external(() -> drawn[0] = FaPlayerModel.draw(view, box, bounds, fx, fy, h, a));
        return drawn[0];
    }

    @Override
    void item(ItemStack stack, float x, float y, float size) {
        if (graphics == null || stack == null || stack.isEmpty()) return;
        // Items go through GuiGraphics and land after the canvas at the end of the frame; give
        // them the canvas clip so they stay inside the content area.
        boolean clipped = clip != null;
        if (clipped) {
            graphics.enableScissor((int) Math.floor(clip[0]), (int) Math.floor(clip[1]),
                    (int) Math.ceil(clip[0] + clip[2]), (int) Math.ceil(clip[1] + clip[3]));
        }
        try {
            float ix = gx(x), iy = gy(y), size1 = size * s;
            canvas.external(() -> Renderer.item(graphics, stack, ix, iy, size1, false));
        } finally {
            if (clipped) graphics.disableScissor();
        }
    }

    @Override
    Scope push() {
        Canvas.Scope scope = canvas.scoped();
        saved.push(new float[]{tx, ty, s, alpha});
        savedClips.push(clip == null ? new float[0] : clip);
        return () -> {
            scope.close();
            float[] state = saved.pop();
            tx = state[0];
            ty = state[1];
            s = state[2];
            alpha = state[3];
            float[] previous = savedClips.pop();
            clip = previous.length == 0 ? null : previous;
        };
    }

    @Override
    void translate(float x, float y) {
        canvas.translate(x, y);
        tx += x * s;
        ty += y * s;
    }

    @Override
    void scale(float scale) {
        canvas.scale(scale);
        s *= scale;
    }

    @Override
    void opacity(float value) {
        float clamped = Math.max(0.0f, Math.min(1.0f, value));
        canvas.opacity(clamped);
        alpha *= clamped;
    }

    @Override
    Scope clip(float x, float y, float w, float h) {
        Scope scope = push();
        canvas.clipRect(x, y, Math.max(0.0f, w), Math.max(0.0f, h));
        float[] next = {gx(x), gy(y), Math.max(0.0f, w) * s, Math.max(0.0f, h) * s};
        if (clip != null) {
            float left = Math.max(clip[0], next[0]), top = Math.max(clip[1], next[1]);
            float right = Math.min(clip[0] + clip[2], next[0] + next[2]);
            float bottom = Math.min(clip[1] + clip[3], next[1] + next[3]);
            next = new float[]{left, top, Math.max(0.0f, right - left), Math.max(0.0f, bottom - top)};
        }
        clip = next;
        return scope;
    }

    @Override
    void flush() {
        canvas.flush();
    }

    private float gx(float x) {
        return tx + x * s;
    }

    private float gy(float y) {
        return ty + y * s;
    }
}
