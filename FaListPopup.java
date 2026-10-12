package wtf.carnation.ui.fatality;

/** Dropdown list. Selected entries are drawn in the accent; multi lists stay open while toggling. */
final class FaListPopup extends FaPopup {
    interface Model {
        int size();

        String label(int index);

        boolean selected(int index);

        void click(int index);

        boolean multi();
    }

    private static final float ITEM_H = 14.5f;
    private static final float PAD = 3.5f;
    private static final int MAX_VISIBLE = 10;

    private final Model model;
    private final float minWidth;
    private float scroll;
    private final FaMotion scrollMotion = new FaMotion(0.0f);
    private final FaMotion[] hovers;

    FaListPopup(Object owner, Model model, float minWidth) {
        super(owner);
        this.model = model;
        this.minWidth = minWidth;
        this.hovers = new FaMotion[Math.max(1, model.size())];
        for (int i = 0; i < hovers.length; i++) hovers[i] = new FaMotion(0.0f);
        measure();
    }

    private void measure() {
        FaFont font = FaTheme.field();
        float widest = 0.0f;
        for (int i = 0; i < model.size(); i++) widest = Math.max(widest, font.width(model.label(i)));
        // A little wider than the field it drops from, like the reference.
        w = Math.max(minWidth + 20.0f, widest + 26.0f);
        h = Math.min(model.size(), MAX_VISIBLE) * ITEM_H + PAD * 2.0f;
    }

    private float maxScroll() {
        return Math.max(0.0f, model.size() * ITEM_H - (h - PAD * 2.0f));
    }

    @Override
    protected int background() {
        return FaTheme.FIELD;
    }

    @Override
    protected void draw(FaContext ctx, float alpha) {
        FaFont font = FaTheme.field();
        float offset = scrollMotion.toward(scroll, ctx.dt, 22.0f);
        try (var clip = ctx.draw.clip(x, y + PAD, w, h - PAD * 2.0f)) {
            for (int i = 0; i < model.size(); i++) {
                float iy = y + PAD + i * ITEM_H - offset;
                if (iy + ITEM_H < y || iy > y + h) continue;
                boolean hovered = ctx.mouseX >= x && ctx.mouseX <= x + w && ctx.mouseY >= iy
                        && ctx.mouseY < iy + ITEM_H && ctx.mouseY >= y + PAD && ctx.mouseY <= y + h - PAD;
                float hover = hovers[i].toward(hovered ? 1.0f : 0.0f, ctx.dt, 20.0f);
                if (hover > 0.01f) {
                    ctx.draw.rect(x + 2.0f, iy, w - 4.0f, ITEM_H, 2.0f,
                            FaTheme.alpha(FaTheme.FIELD_HOVER, alpha * hover));
                }
                int color = model.selected(i) ? FaTheme.mix(FaTheme.ACCENT, 0xFFFF8A84, hover)
                        : FaTheme.mix(0xFFB4B4B4, FaTheme.LABEL_HOVER, hover);
                ctx.draw.textMid(model.label(i), x + 7.0f + hover * 1.5f, iy + ITEM_H * 0.5f, font,
                        FaTheme.alpha(color, alpha));
            }
        }
        if (maxScroll() > 0.0f) {
            float track = h - PAD * 2.0f;
            float thumb = Math.max(12.0f, track * track / (model.size() * ITEM_H));
            float thumbY = y + PAD + (track - thumb) * (offset / maxScroll());
            ctx.draw.rect(x + w - 3.0f, thumbY, 1.5f, thumb, 0.75f, FaTheme.alpha(0xFF3A3A3A, alpha));
        }
    }

    @Override
    boolean mouseClicked(FaContext ctx, double mx, double my, int button) {
        if (button != 0) return true;
        if (my < y + PAD || my > y + h - PAD) return true;
        int index = (int) Math.floor((my - y - PAD + scroll) / ITEM_H);
        if (index < 0 || index >= model.size()) return true;
        model.click(index);
        if (!model.multi()) close();
        return true;
    }

    @Override
    boolean mouseScrolled(FaContext ctx, double mx, double my, double amount) {
        if (!contains(mx, my)) return false;
        scroll = Math.max(0.0f, Math.min(maxScroll(), scroll - (float) amount * ITEM_H * 2.0f));
        return true;
    }
}
