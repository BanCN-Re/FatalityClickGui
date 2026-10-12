package wtf.carnation.ui.fatality;

import wtf.carnation.module.value.impl.ColorValue;

import java.util.Locale;

/** A small color chip on the right; clicking it opens the picker beneath. */
final class FaColorRow extends FaRow {
    private final ColorValue color;

    FaColorRow(ColorValue color) {
        super(color, color.getDisplayName());
        this.color = color;
    }

    private float chipX() {
        return right() - FaTheme.CHIP_W;
    }

    @Override
    protected void draw(FaContext ctx, float x, float centerY, float w, float alpha) {
        float cx = chipX(), cy = centerY - FaTheme.CHIP_H * 0.5f;
        drawLabel(ctx, x, centerY, cx - x - FaTheme.ROW_PAD - 6.0f, alpha);
        int argb = color.getColor();
        if ((argb >>> 24) < 255) {
            ctx.draw.rect(cx, cy, FaTheme.CHIP_W, FaTheme.CHIP_H, 2.0f, FaTheme.alpha(0xFF5A5A5A, alpha));
            ctx.draw.rect(cx, cy, FaTheme.CHIP_W * 0.5f, FaTheme.CHIP_H * 0.5f, 0.0f, FaTheme.alpha(0xFF3A3A3A, alpha));
            ctx.draw.rect(cx + FaTheme.CHIP_W * 0.5f, cy + FaTheme.CHIP_H * 0.5f, FaTheme.CHIP_W * 0.5f,
                    FaTheme.CHIP_H * 0.5f, 0.0f, FaTheme.alpha(0xFF3A3A3A, alpha));
        }
        ctx.draw.rect(cx, cy, FaTheme.CHIP_W, FaTheme.CHIP_H, 2.0f, FaTheme.alpha(argb, alpha));
        if (ctx.gui.isPopupOpen(this) || ctx.hovered(cx - 2, cy - 2, FaTheme.CHIP_W + 4, FaTheme.CHIP_H + 4)) {
            ctx.draw.outline(cx - 1.0f, cy - 1.0f, FaTheme.CHIP_W + 2.0f, FaTheme.CHIP_H + 2.0f, 2.5f, 0.6f,
                    FaTheme.alpha(0x66FFFFFF, alpha));
        }
    }

    @Override
    boolean mouseClicked(FaContext ctx, double mx, double my, int button) {
        float cx = chipX(), cy = centerY() - FaTheme.CHIP_H * 0.5f;
        if (button != 0 || !shown || !FaContext.inside(mx, my, cx - 3, cy - 4, FaTheme.CHIP_W + 6, FaTheme.CHIP_H + 8)) {
            return false;
        }
        ctx.gui.togglePopup(this, () -> new FaColorPopup(this, color, ctx.gui), cx - 12.0f, cy,
                FaTheme.CHIP_W, FaTheme.CHIP_H, false);
        return true;
    }

    @Override
    boolean matches(String query) {
        return super.matches(query) || FaColorPopup.hexString(color.getRawColor()).toLowerCase(Locale.ROOT).contains(query);
    }
}
