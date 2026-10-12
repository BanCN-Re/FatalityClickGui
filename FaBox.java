package wtf.carnation.ui.fatality;

/** Anything laid out in the content masonry: module groups and the Render tab's preview. */
abstract class FaBox {
    abstract float targetHeight();

    abstract void place(float targetX, float targetY, boolean snap);

    abstract float targetY();

    /** {@code originX/originY} is the content origin minus scroll; anything outside the view band is skipped. */
    abstract void render(FaContext ctx, float originX, float originY, float width, float viewTop, float viewBottom,
                         float alpha);

    abstract boolean mouseClicked(FaContext ctx, double mx, double my, int button);

    abstract void mouseDragged(FaContext ctx, double mx, double my);

    abstract void mouseReleased();

    abstract boolean mouseScrolled(FaContext ctx, double mx, double my, double amount);
}
