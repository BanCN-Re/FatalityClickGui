package wtf.carnation.ui.fatality;

import java.util.List;

/** The panel behind a gear icon: a titled stack of ordinary rows. */
final class FaRowsPopup extends FaPopup {
    private static final float TITLE_H = 20.0f;
    private final String title;
    private final List<FaRow> rows;

    FaRowsPopup(Object owner, String title, List<FaRow> rows) {
        super(owner);
        this.title = title;
        this.rows = rows;
        w = 206.0f;
        h = measure();
    }

    private float measure() {
        float height = TITLE_H + 4.0f;
        for (FaRow row : rows) height += row.targetHeight();
        return Math.max(TITLE_H + 12.0f, height);
    }

    @Override
    protected void layout(FaContext ctx) {
        h = measure();
    }

    @Override
    protected void draw(FaContext ctx, float alpha) {
        ctx.draw.textMid(title, x + 10.0f, y + TITLE_H * 0.5f + 1.0f, FaTheme.title(), FaTheme.alpha(FaTheme.TITLE, alpha));
        float rowY = y + TITLE_H;
        try (var clip = ctx.draw.clip(x, y, w, h)) {
            for (FaRow row : rows) {
                float height = row.height(ctx.dt);
                if (height <= 0.01f) continue;
                try (var rowClip = ctx.draw.clip(x, rowY, w, height)) {
                    row.render(ctx, x - 2.0f, rowY, w + 4.0f, alpha * row.visibility());
                }
                rowY += height;
            }
        }
    }

    @Override
    boolean mouseClicked(FaContext ctx, double mx, double my, int button) {
        for (FaRow row : rows) if (row.visible() && row.mouseClicked(ctx, mx, my, button)) return true;
        return true;
    }

    @Override
    void mouseDragged(FaContext ctx, double mx, double my) {
        for (FaRow row : rows) row.mouseDragged(ctx, mx, my);
    }

    @Override
    void mouseReleased() {
        for (FaRow row : rows) row.mouseReleased();
    }

    @Override
    boolean mouseScrolled(FaContext ctx, double mx, double my, double amount) {
        for (FaRow row : rows) if (row.mouseScrolled(ctx, mx, my, amount)) return true;
        return contains(mx, my);
    }
}
