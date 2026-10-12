package wtf.carnation.ui.fatality;

/** Per-frame state shared by every widget of the FA interface. */
final class FaContext {
    final FaClickGui gui;
    /** Set by the screen each frame; the game draws through the Canvas, previews through Java2D. */
    FaDraw draw;
    float mouseX;
    float mouseY;
    float dt;
    /** False while the cursor is over a popup, so widgets underneath don't light up. */
    boolean hoverEnabled = true;

    FaContext(FaClickGui gui) {
        this.gui = gui;
    }

    boolean hovered(float x, float y, float w, float h) {
        return hoverEnabled && mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    static boolean inside(double mx, double my, float x, float y, float w, float h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }
}
