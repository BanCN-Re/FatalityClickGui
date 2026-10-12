package wtf.carnation.ui.fatality;

import wtf.carnation.module.Category;

/**
 * Palette, metrics and glyphs of the FA interface. Colors are ARGB and were sampled from the
 * reference so the flat charcoal surfaces, the salmon accent and the grey text ramps match.
 */
final class FaTheme {
    // ── Surfaces ────────────────────────────────────────────────────────────
    static final int WINDOW = 0xFF151515;
    static final int HEADER_TOP = 0xFF191919;
    static final int HEADER = 0xFF1C1C1C;
    static final int FOOTER = 0xFF1C1C1C;
    static final int DIVIDER = 0xFF111111;
    static final int GROUP = 0xFF1C1C1C;
    static final int GROUP_BORDER = 0xFF222222;
    static final int FIELD = 0xFF161616;
    static final int FIELD_HOVER = 0xFF191919;
    static final int TAB_ACTIVE = 0xFF222222;
    static final int POPUP = 0xFF1C1C1C;
    static final int POPUP_BORDER = 0xFF242424;
    static final int SHADOW = 0x8C000000;

    // ── Text ────────────────────────────────────────────────────────────────
    static final int LOGO = 0xFFF2F0F5;
    static final int TITLE = 0xFFEDEDED;
    static final int LABEL = 0xFFE4E4E4;
    static final int LABEL_HOVER = 0xFFF4F4F4;
    static final int LABEL_DISABLED = 0xFF3A3A3A;
    static final int MUTED = 0xFF666666;
    static final int FIELD_TEXT = 0xFF686868;
    static final int FIELD_TEXT_HOVER = 0xFF959595;
    static final int TAB_TEXT = 0xFF666666;
    static final int TAB_TEXT_HOVER = 0xFF9A9A9A;
    static final int TAB_TEXT_ACTIVE = 0xFFEAEAEA;
    static final int SIDE_TEXT = 0xFF666666;
    static final int SIDE_TEXT_ENABLED = 0xFF9C9C9C;
    static final int SIDE_TEXT_ACTIVE = 0xFFEDEDED;
    static final int ICON = 0xFF6A6A6A;
    static final int ICON_FAINT = 0xFF444444;
    static final int ICON_HOVER = 0xFFD2D2D2;
    static final int CHEVRON = 0xFFCACACA;
    static final int TICK = 0xFFF6F6F6;

    // ── Accents ─────────────────────────────────────────────────────────────
    static final int ACCENT = 0xFFFF615A;
    static final int ACCENT_DIM = 0xFF8A3A36;
    static final int ORANGE = 0xFFF5A35D;

    // ── Metrics ─────────────────────────────────────────────────────────────
    // Design units: 0.8 of the reference's logical pixels, so the default window lands on a
    // 960x540 GUI with margin. FaClickGui scales the whole interface to the actual screen.
    static final float DEFAULT_W = 744.0f;
    static final float DEFAULT_H = 540.0f;
    static final float MIN_W = 500.0f;
    static final float MIN_H = 320.0f;
    static final float RADIUS = 4.0f;
    static final float HEADER_H = 40.0f;
    static final float FOOTER_H = 22.0f;
    static final float SIDEBAR_W = 98.0f;
    static final float CONTENT_PAD = 10.0f;
    static final float COLUMN_GAP = 13.5f;
    static final float GROUP_GAP = 16.0f;
    static final float GROUP_RADIUS = 3.5f;
    /** Group top to the first row; the title sits on the top edge itself. */
    static final float GROUP_TITLE_H = 15.0f;
    static final float GROUP_BOTTOM = 10.0f;
    static final float ROW_H = 25.5f;
    static final float ROW_PAD = 15.0f;
    static final float FIELD_W = 72.0f;
    static final float FIELD_H = 19.0f;
    static final float SLIDER_H = 13.0f;
    static final float FIELD_RADIUS = 2.5f;
    static final float CHECK = 10.5f;
    static final float CHIP_W = 17.5f;
    static final float CHIP_H = 9.0f;
    static final float GEAR = 10.5f;
    static final float MIN_COLUMN_W = 188.0f;

    // ── Lucide glyphs ───────────────────────────────────────────────────────
    static final String I_COMBAT = "\uE0B0";
    static final String I_MOVEMENT = "\uE3BC";
    static final String I_PLAYER = "\uE19E";
    static final String I_WORLD = "\uE0EB";
    static final String I_RENDER = "\uE0BE";
    static final String I_MISC = "\uE157";
    static final String I_GEAR = "\uE157";
    static final String I_SEARCH = "\uE154";
    static final String I_SAVE = "\uE150";
    static final String I_FOLDER = "\uE0DC";
    static final String I_INFO = "\uE0FE";
    static final String I_CHEVRON_DOWN = "\uE071";
    static final String I_CHEVRON_UP = "\uE074";
    static final String I_CHECK = "\uE070";
    static final String I_COPY = "\uE0A2";
    static final String I_PASTE = "\uE3EB";
    static final String I_CLOSE = "\uE1B1";
    static final String I_PLUS = "\uE140";
    static final String I_KEYBOARD = "\uE283";
    static final String I_EYE = "\uE0BE";
    static final String I_EYE_OFF = "\uE0BF";

    private FaTheme() {
    }

    /** Swapped by previews rendered outside the game. */
    static FaFont.Provider fonts = FaFont.GAME;

    static FaFont logo() { return fonts.logo(); }
    static FaFont tab() { return fonts.tab(); }
    static FaFont title() { return fonts.title(); }
    static FaFont label() { return fonts.label(); }
    static FaFont field() { return fonts.field(); }
    static FaFont small() { return fonts.small(); }
    static FaFont icon(float size) { return fonts.icon(size); }

    static String icon(Category category) {
        return switch (category) {
            case COMBAT -> I_COMBAT;
            case MOVEMENT -> I_MOVEMENT;
            case PLAYER -> I_PLAYER;
            case WORLD -> I_WORLD;
            case RENDER -> I_RENDER;
            case MISC -> I_MISC;
        };
    }

    /** Multiplies the alpha channel. */
    static int alpha(int argb, float alpha) {
        int a = Math.round((argb >>> 24) * Math.max(0.0f, Math.min(1.0f, alpha)));
        return (argb & 0x00FFFFFF) | a << 24;
    }

    static int mix(int from, int to, float amount) {
        float t = Math.max(0.0f, Math.min(1.0f, amount));
        int a = Math.round(((from >>> 24) & 255) + (((to >>> 24) & 255) - ((from >>> 24) & 255)) * t);
        int r = Math.round(((from >> 16) & 255) + (((to >> 16) & 255) - ((from >> 16) & 255)) * t);
        int g = Math.round(((from >> 8) & 255) + (((to >> 8) & 255) - ((from >> 8) & 255)) * t);
        int b = Math.round((from & 255) + ((to & 255) - (from & 255)) * t);
        return a << 24 | r << 16 | g << 8 | b;
    }

    static String fit(String text, FaFont font, float width) {
        if (text == null) return "";
        if (font.width(text) <= width) return text;
        String ellipsis = "...";
        int end = text.length();
        while (end > 0 && font.width(text.substring(0, end) + ellipsis) > width) end--;
        return end <= 0 ? ellipsis : text.substring(0, end).stripTrailing() + ellipsis;
    }
}
