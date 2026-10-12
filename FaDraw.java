package wtf.carnation.ui.fatality;

import net.minecraft.world.item.ItemStack;

/**
 * Drawing vocabulary of the FA interface. The game implementation draws through the Canvas;
 * keeping widgets on this narrow surface also lets the layout be rendered offline for review.
 */
abstract class FaDraw {
    /** Restores transform, opacity and clip when closed. */
    interface Scope extends AutoCloseable {
        @Override
        void close();
    }

    abstract void rect(float x, float y, float w, float h, float radius, int argb);

    abstract void outline(float x, float y, float w, float h, float radius, float width, int argb);

    /** Four-corner gradient: top-left, top-right, bottom-right, bottom-left. */
    abstract void quad(float x, float y, float w, float h, int topLeft, int topRight, int bottomRight, int bottomLeft);

    abstract void shadow(float x, float y, float w, float h, float radius, float sigma, int argb);

    /** {@code y} is the top of the text box. */
    abstract void text(String text, float x, float y, FaFont font, int argb);

    /**
     * How the preview player is posed and which ESP look surrounds it. {@code esp} is one of
     * {@link #ESP_NONE}, {@link #ESP_GLOW} or {@link #ESP_OUTLINE}; colors are ARGB.
     */
    record PlayerView(float yaw, float headYaw, float headPitch, int esp, int colorA, int colorB, float glowRadius) {
    }

    static final int ESP_NONE = 0;
    static final int ESP_GLOW = 1;
    static final int ESP_OUTLINE = 2;

    /**
     * The local player standing with its feet at ({@code feetX}, {@code feetY}), {@code height}
     * tall, drawn within the box. Returns false when no player can be shown.
     */
    abstract boolean player(float boxX, float boxY, float boxW, float boxH, float feetX, float feetY, float height,
                            PlayerView view);

    abstract void item(ItemStack stack, float x, float y, float size);

    abstract Scope push();

    abstract void translate(float x, float y);

    abstract void scale(float scale);

    abstract void opacity(float alpha);

    /** Clip to a rectangle (intersected with the current clip) until the scope closes. */
    abstract Scope clip(float x, float y, float w, float h);

    /** Submits pending work before something draws outside this vocabulary. */
    void flush() {
    }

    // ── Conveniences ───────────────────────────────────────────────────────

    final void rect(float x, float y, float w, float h, int argb) {
        rect(x, y, w, h, 0.0f, argb);
    }

    final void vertical(float x, float y, float w, float h, int top, int bottom) {
        quad(x, y, w, h, top, top, bottom, bottom);
    }

    final void horizontal(float x, float y, float w, float h, int left, int right) {
        quad(x, y, w, h, left, right, right, left);
    }

    final void textMid(String text, float x, float centerY, FaFont font, int argb) {
        text(text, x, centerY - font.height() * 0.5f, font, argb);
    }

    final void textRightMid(String text, float right, float centerY, FaFont font, int argb) {
        textMid(text, right - font.width(text), centerY, font, argb);
    }

    final void textCenter(String text, float centerX, float centerY, FaFont font, int argb) {
        textMid(text, centerX - font.width(text) * 0.5f, centerY, font, argb);
    }

    final void icon(String glyph, float centerX, float centerY, float size, int argb) {
        FaFont font = FaTheme.icon(size);
        text(glyph, centerX - font.width(glyph) * 0.5f, centerY - font.height() * 0.5f, font, argb);
    }
}
