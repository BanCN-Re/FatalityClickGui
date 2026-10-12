package wtf.carnation.ui.fatality;

import wtf.carnation.module.value.impl.BoundsNumberValue;

import java.util.Locale;

/** Range field: the accent band spans low..high; dragging moves whichever end is nearer. */
final class FaRangeRow extends FaRow {
    private final BoundsNumberValue bounds;
    private final FaMotion low;
    private final FaMotion high;
    private int dragging = -1;

    FaRangeRow(BoundsNumberValue bounds) {
        super(bounds, bounds.getDisplayName());
        this.bounds = bounds;
        this.low = new FaMotion(fraction(bounds.getLow()));
        this.high = new FaMotion(fraction(bounds.getHigh()));
    }

    private float fraction(double value) {
        double span = bounds.getMax() - bounds.getMin();
        return span <= 0 ? 0.0f : (float) ((value - bounds.getMin()) / span);
    }

    @Override
    protected void draw(FaContext ctx, float x, float centerY, float w, float alpha) {
        float fw = FaTheme.FIELD_W, fh = FaTheme.SLIDER_H;
        float fx = fieldX(fw), fy = sliderY();
        drawLabel(ctx, x, centerY, fx - x - FaTheme.ROW_PAD - 6.0f, alpha);
        boolean hot = dragging >= 0 || ctx.hovered(fx, fieldY(), fw, FaTheme.FIELD_H);
        ctx.draw.rect(fx, fy, fw, fh, FaTheme.FIELD_RADIUS,
                FaTheme.alpha(hot ? FaTheme.FIELD_HOVER : FaTheme.FIELD, alpha));
        float a = low.toward(fraction(bounds.getLow()), ctx.dt, 24.0f);
        float b = high.toward(fraction(bounds.getHigh()), ctx.dt, 24.0f);
        float inset = FaSliderRow.FILL_INSET, innerW = fw - inset * 2.0f, innerH = fh - inset * 2.0f;
        float bandX = fx + inset + innerW * Math.min(a, b);
        float bandW = Math.max(innerH * 0.6f, innerW * Math.abs(b - a));
        bandW = Math.min(bandW, fx + fw - inset - bandX);
        ctx.draw.rect(bandX, fy + inset, bandW, innerH, FaTheme.FIELD_RADIUS - 0.5f, FaTheme.alpha(FaTheme.ACCENT, alpha));

        FaFont font = FaTheme.field();
        String text = format(bounds.getLow()) + " - " + format(bounds.getHigh());
        float textX = fx + (fw - font.width(text)) * 0.5f;
        int grey = FaTheme.alpha(hot ? FaTheme.FIELD_TEXT_HOVER : FaTheme.FIELD_TEXT, alpha);
        int white = FaTheme.alpha(0xFFFFFFFF, alpha);
        // Ranges are often narrow; only a band reaching the middle of the text turns it white.
        float center = textX + font.width(text) * 0.5f;
        float cover = Math.min(center - bandX, bandX + bandW - center);
        ctx.draw.textMid(text, textX, centerY, font, FaSliderRow.valueColor(cover, grey, white));
    }

    private String format(double value) {
        return switch (bounds.getRepresentation()) {
            case INT, MILLISECONDS -> String.valueOf(Math.round(value));
            case DOUBLE, DECIMAL -> String.format(Locale.ROOT, "%.2f", value);
        };
    }

    @Override
    boolean mouseClicked(FaContext ctx, double mx, double my, int button) {
        float fx = fieldX(FaTheme.FIELD_W);
        if (button != 0 || !shown || !FaContext.inside(mx, my, fx, fieldY(), FaTheme.FIELD_W, FaTheme.FIELD_H)) {
            return false;
        }
        double value = valueAt(mx);
        dragging = Math.abs(value - bounds.getLow()) <= Math.abs(value - bounds.getHigh()) ? 0 : 1;
        apply(mx);
        return true;
    }

    @Override
    void mouseDragged(FaContext ctx, double mx, double my) {
        if (dragging >= 0) apply(mx);
    }

    @Override
    void mouseReleased() {
        dragging = -1;
    }

    private double valueAt(double mx) {
        float inset = FaSliderRow.FILL_INSET;
        double t = Math.max(0.0, Math.min(1.0, (mx - fieldX(FaTheme.FIELD_W) - inset) / (FaTheme.FIELD_W - inset * 2.0f)));
        double raw = bounds.getMin() + t * (bounds.getMax() - bounds.getMin());
        double inc = bounds.getInc();
        double snapped = inc > 0 ? Math.round((raw - bounds.getMin()) / inc) * inc + bounds.getMin() : raw;
        return Math.max(bounds.getMin(), Math.min(bounds.getMax(), Math.round(snapped * 1e6) / 1e6));
    }

    private void apply(double mx) {
        double value = valueAt(mx);
        if (dragging == 0) bounds.setLow(value);
        else bounds.setHigh(value);
    }
}
