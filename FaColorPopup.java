package wtf.carnation.ui.fatality;

import net.minecraft.client.Minecraft;
import wtf.carnation.module.value.impl.ColorValue;

import java.awt.Color;
import java.util.Locale;

/**
 * Color picker: saturation/value square, vertical hue bar, horizontal alpha bar, copy and paste
 * buttons and an editable #rrggbbaa field. HSV is kept locally so dragging through grey or black
 * doesn't lose the hue.
 */
final class FaColorPopup extends FaPopup implements FaKeySink {
    private static final float PAD = 8.0f;
    private static final float SV_H = 118.0f;
    private static final float HUE_W = 9.0f;
    private static final float GAP = 6.0f;
    private static final float ALPHA_H = 8.0f;
    private static final float FOOT_H = 16.0f;
    private static final float RAINBOW_H = 18.0f;
    private static final int[] HUES = {0xFFFF0000, 0xFFFFFF00, 0xFF00FF00, 0xFF00FFFF, 0xFF0000FF, 0xFFFF00FF, 0xFFFF0000};

    private final ColorValue color;
    private final FaClickGui gui;
    private final FaTextInput hex = new FaTextInput(9);
    private float hue, saturation, brightness, alpha;
    private int lastWritten;
    private int dragging = -1;
    private boolean editing;

    FaColorPopup(Object owner, ColorValue color, FaClickGui gui) {
        super(owner);
        this.color = color;
        this.gui = gui;
        w = 180.0f;
        h = height();
        load(color.getRawColor());
        hex.onSubmit(this::applyHex).onCancel(() -> editing = false);
    }

    private float height() {
        return PAD + SV_H + GAP + ALPHA_H + GAP + FOOT_H + (color.isRainbowAllowed() ? RAINBOW_H : 0.0f) + PAD - 2.0f;
    }

    private void load(int argb) {
        float[] hsb = Color.RGBtoHSB((argb >> 16) & 255, (argb >> 8) & 255, argb & 255, null);
        if (hsb[1] > 0.001f && hsb[2] > 0.001f) hue = hsb[0];
        saturation = hsb[1];
        brightness = hsb[2];
        alpha = ((argb >>> 24) & 255) / 255.0f;
        lastWritten = argb;
    }

    private int current() {
        int rgb = Color.HSBtoRGB(hue, saturation, brightness) & 0xFFFFFF;
        return Math.round(alpha * 255.0f) << 24 | rgb;
    }

    private void write() {
        int argb = current();
        color.setColor(argb);
        lastWritten = argb;
    }

    // Geometry ---------------------------------------------------------------
    private float svX() { return x + PAD; }
    private float svY() { return y + PAD; }
    private float svW() { return w - PAD * 2.0f - HUE_W - GAP; }
    private float hueX() { return svX() + svW() + GAP; }
    private float alphaY() { return svY() + SV_H + GAP; }
    private float footY() { return alphaY() + ALPHA_H + GAP; }
    private float rainbowY() { return footY() + FOOT_H; }

    @Override
    protected void layout(FaContext ctx) {
        h = height();
        if (!editing && color.getRawColor() != lastWritten) load(color.getRawColor());
    }

    @Override
    protected void draw(FaContext ctx, float a) {
        float sx = svX(), sy = svY(), sw = svW();
        int pure = 0xFF000000 | (Color.HSBtoRGB(hue, 1.0f, 1.0f) & 0xFFFFFF);
        ctx.draw.horizontal(sx, sy, sw, SV_H, FaTheme.alpha(0xFFFFFFFF, a), FaTheme.alpha(pure, a));
        ctx.draw.vertical(sx, sy, sw, SV_H, 0x00000000, FaTheme.alpha(0xFF000000, a));
        float knobX = sx + saturation * sw, knobY = sy + (1.0f - brightness) * SV_H;
        ctx.draw.outline(knobX - 3.5f, knobY - 3.5f, 7.0f, 7.0f, 3.5f, 1.0f, FaTheme.alpha(0xCC000000, a));
        ctx.draw.outline(knobX - 3.0f, knobY - 3.0f, 6.0f, 6.0f, 3.0f, 1.0f, FaTheme.alpha(0xFFFFFFFF, a));

        float hx = hueX();
        float segment = SV_H / 6.0f;
        for (int i = 0; i < 6; i++) {
            ctx.draw.vertical(hx, sy + i * segment, HUE_W, segment + 0.5f, FaTheme.alpha(HUES[i], a),
                    FaTheme.alpha(HUES[i + 1], a));
        }
        float hueY = sy + hue * SV_H;
        ctx.draw.rect(hx - 1.5f, hueY - 1.0f, HUE_W + 3.0f, 2.0f, 1.0f, FaTheme.alpha(0xFFFFFFFF, a));

        float ay = alphaY(), aw = w - PAD * 2.0f;
        checker(ctx, svX(), ay, aw, ALPHA_H, a);
        int rgb = current() & 0xFFFFFF;
        ctx.draw.horizontal(svX(), ay, aw, ALPHA_H, FaTheme.alpha(rgb, a * 0.0f), FaTheme.alpha(0xFF000000 | rgb, a));
        float alphaX = svX() + alpha * aw;
        ctx.draw.rect(alphaX - 1.0f, ay - 1.5f, 2.0f, ALPHA_H + 3.0f, 1.0f, FaTheme.alpha(0xFFFFFFFF, a));

        float fy = footY(), mid = fy + FOOT_H * 0.5f;
        boolean copyHover = ctx.mouseX >= svX() && ctx.mouseX <= svX() + 14 && ctx.mouseY >= fy && ctx.mouseY <= fy + FOOT_H;
        boolean pasteHover = ctx.mouseX >= svX() + 18 && ctx.mouseX <= svX() + 32 && ctx.mouseY >= fy && ctx.mouseY <= fy + FOOT_H;
        ctx.draw.icon(FaTheme.I_COPY, svX() + 7.0f, mid, 10.0f, FaTheme.alpha(copyHover ? FaTheme.ICON_HOVER : FaTheme.ICON, a));
        ctx.draw.icon(FaTheme.I_PASTE, svX() + 25.0f, mid, 10.0f, FaTheme.alpha(pasteHover ? FaTheme.ICON_HOVER : FaTheme.ICON, a));
        FaFont font = FaTheme.field();
        float textX = svX() + 42.0f;
        ctx.draw.rect(textX - 6.0f, fy, x + w - PAD - textX + 6.0f, FOOT_H, FaTheme.FIELD_RADIUS,
                FaTheme.alpha(editing ? FaTheme.FIELD_HOVER : FaTheme.FIELD, a));
        if (editing) {
            hex.render(ctx.draw, textX, mid, w - PAD - textX + x, font, FaTheme.alpha(0xFFDDDDDE, a), true, "", 0);
        } else {
            ctx.draw.textMid(hexString(current()), textX, mid, font, FaTheme.alpha(0xFFDDDDDE, a));
        }

        if (color.isRainbowAllowed()) {
            float ry = rainbowY(), rmid = ry + RAINBOW_H * 0.5f;
            ctx.draw.textMid("Rainbow", svX(), rmid, FaTheme.label(), FaTheme.alpha(FaTheme.LABEL, a));
            float box = FaTheme.CHECK, boxX = x + w - PAD - box;
            ctx.draw.rect(boxX, rmid - box * 0.5f, box, box, 2.0f, FaTheme.alpha(FaTheme.FIELD, a));
            if (color.isRainbow()) ctx.draw.icon(FaTheme.I_CHECK, boxX + box * 0.5f, rmid, 9.5f, FaTheme.alpha(FaTheme.TICK, a));
        }
    }

    private static void checker(FaContext ctx, float x, float y, float w, float h, float a) {
        ctx.draw.rect(x, y, w, h, FaTheme.alpha(0xFF3A3A3A, a));
        float cell = h / 2.0f;
        for (int column = 0; column * cell < w; column++) {
            for (int row = 0; row < 2; row++) {
                if ((column + row) % 2 == 0) continue;
                ctx.draw.rect(x + column * cell, y + row * cell, Math.min(cell, w - column * cell), cell,
                        FaTheme.alpha(0xFF5A5A5A, a));
            }
        }
    }

    static String hexString(int argb) {
        return String.format(Locale.ROOT, "#%06x%02x", argb & 0xFFFFFF, (argb >>> 24) & 255);
    }

    @Override
    boolean mouseClicked(FaContext ctx, double mx, double my, int button) {
        if (button != 0) return true;
        if (FaContext.inside(mx, my, svX(), svY(), svW(), SV_H)) dragging = 0;
        else if (FaContext.inside(mx, my, hueX() - 2, svY(), HUE_W + 4, SV_H)) dragging = 1;
        else if (FaContext.inside(mx, my, svX(), alphaY() - 2, w - PAD * 2, ALPHA_H + 4)) dragging = 2;
        if (dragging >= 0) {
            drag(mx, my);
            return true;
        }
        float fy = footY();
        if (FaContext.inside(mx, my, svX(), fy, 14, FOOT_H)) {
            Minecraft.getInstance().keyboardHandler.setClipboard(hexString(current()));
            gui.toast("Copied " + hexString(current()));
        } else if (FaContext.inside(mx, my, svX() + 18, fy, 14, FOOT_H)) {
            String clipboard = Minecraft.getInstance().keyboardHandler.getClipboard();
            if (clipboard != null && parseHex(clipboard.trim()) != null) {
                load(parseHex(clipboard.trim()));
                write();
            }
        } else if (FaContext.inside(mx, my, svX() + 36, fy, w - PAD * 2 - 36, FOOT_H)) {
            editing = true;
            hex.set(hexString(current()));
            hex.focused();
            gui.focus(this);
        } else if (color.isRainbowAllowed() && FaContext.inside(mx, my, x, rainbowY(), w, RAINBOW_H)) {
            color.setRainbow(!color.isRainbow());
        }
        return true;
    }

    @Override
    void mouseDragged(FaContext ctx, double mx, double my) {
        if (dragging >= 0) drag(mx, my);
    }

    @Override
    void mouseReleased() {
        dragging = -1;
    }

    private void drag(double mx, double my) {
        switch (dragging) {
            case 0 -> {
                saturation = clamp((float) ((mx - svX()) / svW()));
                brightness = 1.0f - clamp((float) ((my - svY()) / SV_H));
            }
            case 1 -> hue = Math.min(0.9999f, clamp((float) ((my - svY()) / SV_H)));
            case 2 -> alpha = clamp((float) ((mx - svX()) / (w - PAD * 2.0f)));
            default -> {
                return;
            }
        }
        write();
    }

    private void applyHex(String text) {
        Integer parsed = parseHex(text.trim());
        if (parsed != null) {
            load(parsed);
            write();
        }
        editing = false;
        gui.focus(null);
    }

    /** Accepts #rgb, #rrggbb and #rrggbbaa (alpha last, like the field shows it). */
    static Integer parseHex(String text) {
        String value = text.startsWith("#") ? text.substring(1) : text;
        try {
            if (value.length() == 3) {
                StringBuilder expanded = new StringBuilder();
                for (char c : value.toCharArray()) expanded.append(c).append(c);
                return 0xFF000000 | Integer.parseInt(expanded.toString(), 16);
            }
            if (value.length() == 6) return 0xFF000000 | Integer.parseInt(value, 16);
            if (value.length() == 8) {
                long rgba = Long.parseLong(value, 16);
                return (int) ((rgba & 0xFF) << 24 | rgba >>> 8);
            }
        } catch (NumberFormatException ignored) {
        }
        return null;
    }

    private static float clamp(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    @Override
    public boolean keyPressed(int key, int modifiers) {
        return editing && hex.keyPressed(key, modifiers);
    }

    @Override
    public boolean charTyped(char character) {
        return editing && (Character.digit(character, 16) >= 0 || character == '#') && hex.charTyped(character);
    }

    @Override
    public void focusLost() {
        editing = false;
    }

    @Override
    protected void onClose() {
        if (editing) gui.focus(null);
    }
}
