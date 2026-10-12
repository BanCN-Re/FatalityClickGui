package wtf.carnation.ui.fatality;

import wtf.carnation.module.Group;
import wtf.carnation.module.value.Value;

import java.util.Locale;

/**
 * One line of a group box: a label on the left and its control on the right. Rows remember
 * where they were drawn last frame so input between frames hits exactly what is on screen.
 */
abstract class FaRow {
    protected final Value<?> value;
    protected final String label;
    private final FaMotion visibility;
    protected final FaMotion hover = new FaMotion(0.0f);
    protected float x, y, w;
    protected boolean shown;

    protected FaRow(Value<?> value, String label) {
        this.value = value;
        this.label = label;
        this.visibility = new FaMotion(visible() ? 1.0f : 0.0f);
    }

    boolean visible() {
        return value == null || value.isAvailable();
    }

    /** Settled height, used for column balancing. */
    float targetHeight() {
        return visible() ? FaTheme.ROW_H : 0.0f;
    }

    /** Animated height, used for drawing. */
    float height(float dt) {
        return visibility.toward(visible() ? 1.0f : 0.0f, dt, 18.0f) * FaTheme.ROW_H;
    }

    float visibility() {
        return visibility.value();
    }

    final void render(FaContext ctx, float x, float y, float w, float alpha) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.shown = alpha > 0.5f && visibility.value() > 0.9f;
        boolean hovered = ctx.hovered(x, y, w, FaTheme.ROW_H);
        hover.toward(hovered ? 1.0f : 0.0f, ctx.dt, 16.0f);
        draw(ctx, x, y + FaTheme.ROW_H * 0.5f, w, alpha);
    }

    /** Draws the row; {@code centerY} is the row's vertical middle. */
    protected abstract void draw(FaContext ctx, float x, float centerY, float w, float alpha);

    protected void drawLabel(FaContext ctx, float x, float centerY, float maxWidth, float alpha) {
        FaFont font = FaTheme.label();
        int color = privileged() ? FaTheme.ORANGE : FaTheme.mix(FaTheme.LABEL, FaTheme.LABEL_HOVER, hover.value());
        ctx.draw.textMid(FaTheme.fit(label, font, maxWidth), x + FaTheme.ROW_PAD, centerY, font,
                FaTheme.alpha(color, alpha));
    }

    /** Fatality marks risky features orange; here that is anything outside the default user group. */
    protected boolean privileged() {
        return value != null && value.getGroup() != Group.User;
    }

    protected float right() {
        return x + w - FaTheme.ROW_PAD;
    }

    protected float centerY() {
        return y + FaTheme.ROW_H * 0.5f;
    }

    protected boolean hit(double mx, double my) {
        return shown && FaContext.inside(mx, my, x, y, w, FaTheme.ROW_H);
    }

    boolean mouseClicked(FaContext ctx, double mx, double my, int button) {
        return false;
    }

    void mouseDragged(FaContext ctx, double mx, double my) {
    }

    void mouseReleased() {
    }

    boolean mouseScrolled(FaContext ctx, double mx, double my, double amount) {
        return false;
    }

    boolean matches(String query) {
        return label.toLowerCase(Locale.ROOT).contains(query);
    }

    /** Field geometry shared by controls that sit in a dark field on the right. */
    protected float fieldX(float width) {
        return right() - width;
    }

    protected float fieldY() {
        return centerY() - FaTheme.FIELD_H * 0.5f;
    }

    /** Sliders are a slimmer track than dropdown fields but keep the full field as hit area. */
    protected float sliderY() {
        return centerY() - FaTheme.SLIDER_H * 0.5f;
    }

    protected static String formatNumber(double value, double increment) {
        if (increment >= 1.0 && value == Math.rint(value)) return String.valueOf((long) value);
        String text = String.format(Locale.ROOT, "%.2f", value);
        return text.replaceAll("0+$", "").replaceAll("\\.$", "");
    }
}
