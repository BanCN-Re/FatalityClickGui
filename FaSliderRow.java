package wtf.carnation.ui.fatality;

import org.lwjgl.glfw.GLFW;
import wtf.carnation.module.value.impl.NumberValue;

/**
 * Number field with an accent fill from the left and the value centered on top; the value turns
 * white once the fill reaches it. Drag to set, scroll to step, right-click to type a value.
 */
final class FaSliderRow extends FaRow implements FaKeySink {
    static final float FILL_INSET = 1.5f;

    private final NumberValue number;
    private final FaMotion fill;
    private final FaTextInput entry = new FaTextInput(16);
    private boolean dragging;
    private boolean editing;

    FaSliderRow(NumberValue number) {
        super(number, number.getDisplayName());
        this.number = number;
        this.fill = new FaMotion(fraction());
        entry.onSubmit(this::applyEntry).onCancel(() -> editing = false);
    }

    private float fraction() {
        double min = number.getMin(), max = number.getMax();
        return max <= min ? 0.0f : (float) ((number.getValue() - min) / (max - min));
    }

    @Override
    protected void draw(FaContext ctx, float x, float centerY, float w, float alpha) {
        float fw = FaTheme.FIELD_W, fh = FaTheme.SLIDER_H;
        float fx = fieldX(fw), fy = sliderY();
        drawLabel(ctx, x, centerY, fx - x - FaTheme.ROW_PAD - 6.0f, alpha);
        boolean hot = dragging || ctx.hovered(fx, fieldY(), fw, FaTheme.FIELD_H);
        ctx.draw.rect(fx, fy, fw, fh, FaTheme.FIELD_RADIUS,
                FaTheme.alpha(hot ? FaTheme.FIELD_HOVER : FaTheme.FIELD, alpha));
        FaFont font = FaTheme.field();
        if (editing) {
            entry.render(ctx.draw, fx + 6.0f, centerY, fw - 12.0f, font, FaTheme.alpha(FaTheme.LABEL, alpha),
                    true, "", 0);
            ctx.draw.outline(fx, fy, fw, fh, FaTheme.FIELD_RADIUS, 0.5f, FaTheme.alpha(FaTheme.ACCENT_DIM, alpha));
            return;
        }
        float t = Math.max(0.0f, Math.min(1.0f, fill.toward(fraction(), ctx.dt, dragging ? 40.0f : 18.0f)));
        // The fill sits inset in the track, like a bar inside a groove.
        float inset = FILL_INSET, innerW = fw - inset * 2.0f, innerH = fh - inset * 2.0f;
        float fillW = innerW * t;
        if (fillW > 0.25f) {
            ctx.draw.rect(fx + inset, fy + inset, Math.max(fillW, Math.min(innerW, innerH * 0.6f)), innerH,
                    FaTheme.FIELD_RADIUS - 0.5f, FaTheme.alpha(FaTheme.ACCENT, alpha));
        }
        String text = formatNumber(number.getValue(), number.getInc());
        float textX = fx + (fw - font.width(text)) * 0.5f;
        int grey = FaTheme.alpha(hot ? FaTheme.FIELD_TEXT_HOVER : FaTheme.FIELD_TEXT, alpha);
        int color = valueColor(fx + inset + fillW - textX, grey, FaTheme.alpha(0xFFFFFFFF, alpha));
        ctx.draw.textMid(text, textX, centerY, font, color);
    }

    /**
     * Grey while the text sits on the empty track, white as soon as the fill reaches under it.
     * One color for the whole value reads better than a glyph split between two; {@code cover}
     * is how far the fill reaches into the text, eased over a few units.
     */
    static int valueColor(float cover, int grey, int white) {
        return FaTheme.mix(grey, white, cover / 4.0f);
    }

    @Override
    boolean mouseClicked(FaContext ctx, double mx, double my, int button) {
        float fx = fieldX(FaTheme.FIELD_W), fy = fieldY();
        if (!shown || !FaContext.inside(mx, my, fx, fy, FaTheme.FIELD_W, FaTheme.FIELD_H)) return false;
        if (button == 1) {
            editing = true;
            entry.set(formatNumber(number.getValue(), number.getInc()));
            ctx.gui.focus(this);
            entry.focused();
            return true;
        }
        if (button != 0) return false;
        if (editing) return true;
        dragging = true;
        apply(mx);
        return true;
    }

    @Override
    void mouseDragged(FaContext ctx, double mx, double my) {
        if (dragging) apply(mx);
    }

    @Override
    void mouseReleased() {
        dragging = false;
    }

    @Override
    boolean mouseScrolled(FaContext ctx, double mx, double my, double amount) {
        if (!shown || !FaContext.inside(mx, my, fieldX(FaTheme.FIELD_W), fieldY(), FaTheme.FIELD_W, FaTheme.FIELD_H)) {
            return false;
        }
        double step = number.getInc() > 0 ? number.getInc() : (number.getMax() - number.getMin()) / 100.0;
        set(number.getValue() + Math.signum(amount) * step);
        return true;
    }

    private void apply(double mx) {
        float fx = fieldX(FaTheme.FIELD_W) + FILL_INSET;
        double t = Math.max(0.0, Math.min(1.0, (mx - fx) / (FaTheme.FIELD_W - FILL_INSET * 2.0f)));
        set(number.getMin() + t * (number.getMax() - number.getMin()));
    }

    private void set(double raw) {
        double min = number.getMin(), max = number.getMax(), inc = number.getInc();
        double snapped = inc > 0 ? Math.round((raw - min) / inc) * inc + min : raw;
        // Rounding the step keeps 0.1-increments from drifting into 0.30000000000000004.
        snapped = Math.round(snapped * 1e6) / 1e6;
        number.setValue(Math.max(min, Math.min(max, snapped)));
    }

    private void applyEntry(String text) {
        try {
            set(Double.parseDouble(text.trim().replace(',', '.')));
        } catch (NumberFormatException ignored) {
        }
        editing = false;
    }

    @Override
    public boolean keyPressed(int key, int modifiers) {
        if (!editing) return false;
        boolean used = entry.keyPressed(key, modifiers);
        if (!editing) return true;
        return used || key == GLFW.GLFW_KEY_TAB;
    }

    @Override
    public boolean charTyped(char character) {
        return editing && (Character.isDigit(character) || character == '.' || character == '-' || character == ',')
                && entry.charTyped(character);
    }

    @Override
    public void focusLost() {
        if (editing) applyEntry(entry.text());
    }

    boolean editing() {
        return editing;
    }
}
