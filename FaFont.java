package wtf.carnation.ui.fatality;

import wtf.carnation.graphics.font.FontLoader;
import wtf.carnation.graphics.font.TrueTypeFont;

import java.util.HashMap;
import java.util.Map;

/** Font as the FA widgets see it: metrics only; drawing goes through {@link FaDraw}. */
interface FaFont {
    float width(String text);

    float height();

    // Sizes in design units; each distinct size is its own atlas, so keep the set small.
    float LOGO = 15.0f;
    float TAB = 9.4f;
    float TITLE = 10.0f;
    float LABEL = 9.8f;
    float FIELD = 9.6f;
    float SMALL = 8.8f;

    /** Font handles; swapped for previews rendered outside the game. */
    interface Provider {
        FaFont logo();
        FaFont tab();
        FaFont title();
        FaFont label();
        FaFont field();
        FaFont small();
        FaFont icon(float size);
    }

    record Game(TrueTypeFont font) implements FaFont {
        @Override
        public float width(String text) {
            return font.getStringWidth(text);
        }

        @Override
        public float height() {
            return font.getHeight();
        }
    }

    Provider GAME = new Provider() {
        // Handles are looked up per draw call, so keep one per (loader, size).
        private final Map<FontLoader, Map<Float, FaFont>> cache = new HashMap<>();

        private FaFont get(FontLoader loader, float size) {
            return cache.computeIfAbsent(loader, ignored -> new HashMap<>())
                    .computeIfAbsent(size, ignored -> new Game(loader.get(size)));
        }

        @Override public FaFont logo() { return get(FontLoader.manropeBold, LOGO); }
        @Override public FaFont tab() { return get(FontLoader.misans, TAB); }
        @Override public FaFont title() { return get(FontLoader.misans, TITLE); }
        @Override public FaFont label() { return get(FontLoader.misans, LABEL); }
        @Override public FaFont field() { return get(FontLoader.misans, FIELD); }
        @Override public FaFont small() { return get(FontLoader.misans, SMALL); }
        @Override public FaFont icon(float size) { return get(FontLoader.lucide, size); }
    };
}
