package wtf.carnation.ui.fatality;

/**
 * A floating panel above the window (dropdown lists, color picker, gear options...). Popups
 * stack; the topmost gets input first and a click outside it closes only that one.
 */
abstract class FaPopup {
    protected final Object owner;
    protected float x, y, w, h;
    private final FaMotion open = new FaMotion(0.0f);
    private boolean closing;

    protected FaPopup(Object owner) {
        this.owner = owner;
    }

    Object owner() {
        return owner;
    }

    /** Positions the popup under an anchor, flipping above it when there is no room. */
    void place(float anchorX, float anchorY, float anchorW, float anchorH, float minX, float minY,
               float maxX, float maxY) {
        x = Math.max(minX + 2.0f, Math.min(anchorX, maxX - w - 2.0f));
        float below = anchorY + anchorH + 3.0f;
        float above = anchorY - h - 3.0f;
        y = below + h <= maxY - 2.0f || above < minY ? below : above;
        y = Math.max(minY + 2.0f, Math.min(y, maxY - h - 2.0f));
    }

    /** Positions the popup above an anchor (footer menus), clamped to the screen. */
    void placeAbove(float anchorX, float anchorY, float minX, float minY, float maxX, float maxY) {
        x = Math.max(minX + 2.0f, Math.min(anchorX, maxX - w - 2.0f));
        y = Math.max(minY + 2.0f, anchorY - h - 4.0f);
    }

    /** Positions the popup beside an anchor (right of it when possible). */
    void placeBeside(float anchorX, float anchorY, float anchorW, float anchorH, float minX, float minY,
                     float maxX, float maxY) {
        float right = anchorX + anchorW + 6.0f;
        x = right + w <= maxX - 2.0f ? right : anchorX - w - 6.0f;
        x = Math.max(minX + 2.0f, Math.min(x, maxX - w - 2.0f));
        y = Math.max(minY + 2.0f, Math.min(anchorY - 4.0f, maxY - h - 2.0f));
    }

    final void render(FaContext ctx) {
        float alpha = open.toward(closing ? 0.0f : 1.0f, ctx.dt, closing ? 26.0f : 20.0f);
        if (alpha <= 0.01f) return;
        layout(ctx);
        try (var ignored = ctx.draw.push()) {
            ctx.draw.translate(0.0f, (1.0f - alpha) * -4.0f);
            ctx.draw.shadow(x, y, w, h, FaTheme.FIELD_RADIUS, 8.0f, FaTheme.alpha(FaTheme.SHADOW, alpha));
            ctx.draw.rect(x, y, w, h, FaTheme.FIELD_RADIUS, FaTheme.alpha(background(), alpha));
            ctx.draw.outline(x, y, w, h, FaTheme.FIELD_RADIUS, 0.5f, FaTheme.alpha(FaTheme.POPUP_BORDER, alpha));
            draw(ctx, alpha);
        }
    }

    protected int background() {
        return FaTheme.POPUP;
    }

    /** Updates size (and contents) before drawing; position stays where it was placed. */
    protected void layout(FaContext ctx) {
    }

    protected abstract void draw(FaContext ctx, float alpha);

    boolean contains(double mx, double my) {
        return !closing && FaContext.inside(mx, my, x, y, w, h);
    }

    boolean finished() {
        return closing && open.value() <= 0.01f;
    }

    boolean closing() {
        return closing;
    }

    void close() {
        if (closing) return;
        closing = true;
        onClose();
    }

    protected void onClose() {
    }

    /** Called only for clicks inside the popup. */
    boolean mouseClicked(FaContext ctx, double mx, double my, int button) {
        return true;
    }

    void mouseDragged(FaContext ctx, double mx, double my) {
    }

    void mouseReleased() {
    }

    boolean mouseScrolled(FaContext ctx, double mx, double my, double amount) {
        return contains(mx, my);
    }

    boolean keyPressed(FaContext ctx, int key, int modifiers) {
        return false;
    }

    boolean charTyped(FaContext ctx, char character) {
        return false;
    }
}
