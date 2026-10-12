package wtf.carnation.ui.fatality;

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

/** Single-line text editing shared by search, text values, numeric entry and config names. */
final class FaTextInput {
    private final int maxLength;
    private String text = "";
    private Consumer<String> onChange = text -> { };
    private Consumer<String> onSubmit = text -> { };
    private Runnable onCancel = () -> { };
    private long focusedAt;

    FaTextInput(int maxLength) {
        this.maxLength = maxLength;
    }

    FaTextInput onChange(Consumer<String> listener) {
        onChange = listener;
        return this;
    }

    FaTextInput onSubmit(Consumer<String> listener) {
        onSubmit = listener;
        return this;
    }

    FaTextInput onCancel(Runnable listener) {
        onCancel = listener;
        return this;
    }

    String text() {
        return text;
    }

    void set(String value) {
        text = value == null ? "" : value.length() > maxLength ? value.substring(0, maxLength) : value;
    }

    void focused() {
        focusedAt = System.currentTimeMillis();
    }

    /** @return true if the key was used; Enter submits, Escape cancels. */
    boolean keyPressed(int key, int modifiers) {
        boolean control = (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;
        switch (key) {
            case GLFW.GLFW_KEY_BACKSPACE -> {
                if (!text.isEmpty()) {
                    text = control ? "" : text.substring(0, text.offsetByCodePoints(text.length(), -1));
                    onChange.accept(text);
                }
                return true;
            }
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                onSubmit.accept(text);
                return true;
            }
            case GLFW.GLFW_KEY_ESCAPE -> {
                onCancel.run();
                return true;
            }
            case GLFW.GLFW_KEY_V -> {
                if (!control) return false;
                String clipboard = Minecraft.getInstance().keyboardHandler.getClipboard();
                if (clipboard != null) append(clipboard.replace("\r", "").replace("\n", ""));
                return true;
            }
            case GLFW.GLFW_KEY_C -> {
                if (!control) return false;
                Minecraft.getInstance().keyboardHandler.setClipboard(text);
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    boolean charTyped(char character) {
        if (character < 32 || character == 127) return false;
        append(String.valueOf(character));
        return true;
    }

    private void append(String value) {
        int room = maxLength - text.length();
        if (room <= 0 || value.isEmpty()) return;
        text += value.length() > room ? value.substring(0, room) : value;
        onChange.accept(text);
    }

    /** Draws text clipped from the left so the caret end stays visible, plus a blinking caret. */
    void render(FaDraw draw, float x, float centerY, float width, FaFont font, int color, boolean focused,
                String placeholder, int placeholderColor) {
        if (text.isEmpty() && !focused) {
            draw.textMid(placeholder, x, centerY, font, placeholderColor);
            return;
        }
        String shown = text;
        while (!shown.isEmpty() && font.width(shown) > width - 3.0f) {
            shown = shown.substring(shown.offsetByCodePoints(0, 1));
        }
        if (shown.isEmpty() && focused && text.isEmpty() && placeholder != null) {
            draw.textMid(placeholder, x, centerY, font, placeholderColor);
        }
        draw.textMid(shown, x, centerY, font, color);
        if (focused && ((System.currentTimeMillis() - focusedAt) / 530L) % 2L == 0L) {
            float caretX = x + font.width(shown) + 1.0f;
            draw.rect(caretX, centerY - font.height() * 0.42f, 1.0f, font.height() * 0.84f, color);
        }
    }
}
