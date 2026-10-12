package wtf.carnation.ui.fatality;

import wtf.carnation.module.value.Value;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** Label with a small dark box on the right; checked shows a crisp white tick. */
class FaCheckRow extends FaRow {
    private final BooleanSupplier getter;
    private final Consumer<Boolean> setter;
    private final FaMotion check;

    FaCheckRow(Value<?> value, String label, BooleanSupplier getter, Consumer<Boolean> setter) {
        super(value, label);
        this.getter = getter;
        this.setter = setter;
        this.check = new FaMotion(getter.getAsBoolean() ? 1.0f : 0.0f);
    }

    @Override
    protected void draw(FaContext ctx, float x, float centerY, float w, float alpha) {
        float t = check.toward(getter.getAsBoolean() ? 1.0f : 0.0f, ctx.dt, 22.0f);
        float size = FaTheme.CHECK;
        float boxX = right() - size;
        float used = accessory(ctx, boxX - 6.0f, centerY, alpha);
        drawLabel(ctx, x, centerY, boxX - used - x - FaTheme.ROW_PAD - 6.0f, alpha);
        int box = FaTheme.mix(FaTheme.FIELD, FaTheme.FIELD_HOVER, hover.value());
        ctx.draw.rect(boxX, centerY - size * 0.5f, size, size, 2.0f, FaTheme.alpha(box, alpha));
        if (t > 0.01f) {
            // The tick grows in from 70% so toggling reads as a small pop rather than a blink.
            // Scale the canvas, not the font size: every distinct size would build a new font atlas.
            try (var scope = ctx.draw.push()) {
                ctx.draw.translate(boxX + size * 0.5f, centerY);
                ctx.draw.scale(0.7f + 0.3f * t);
                ctx.draw.icon(FaTheme.I_CHECK, 0.0f, 0.0f, 10.0f, FaTheme.alpha(FaTheme.TICK, alpha * t));
            }
        }
    }

    /** Something drawn left of the box, ending at {@code right}; returns the width it took. */
    protected float accessory(FaContext ctx, float right, float centerY, float alpha) {
        return 0.0f;
    }

    @Override
    boolean mouseClicked(FaContext ctx, double mx, double my, int button) {
        if (button != 0 || !hit(mx, my)) return false;
        setter.accept(!getter.getAsBoolean());
        return true;
    }
}
