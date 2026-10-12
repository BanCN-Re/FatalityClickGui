package wtf.carnation.ui.fatality;

import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;
import wtf.carnation.module.value.Value;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/** Key binding field: click, then press a key; Escape, Delete or Backspace clears it. */
final class FaKeyRow extends FaRow implements FaKeySink {
    private final IntSupplier getter;
    private final IntConsumer setter;
    private boolean binding;

    FaKeyRow(Value<?> value, String label, IntSupplier getter, IntConsumer setter) {
        super(value, label);
        this.getter = getter;
        this.setter = setter;
    }

    static String keyName(int key) {
        if (key <= 0) return "None";
        try {
            String name = InputConstants.Type.KEYSYM.getOrCreate(key).getDisplayName().getString();
            return name == null || name.isEmpty() ? "?" : name;
        } catch (RuntimeException exception) {
            return "?";
        }
    }

    @Override
    protected void draw(FaContext ctx, float x, float centerY, float w, float alpha) {
        FaFont font = FaTheme.field();
        String text = binding ? "..." : keyName(getter.getAsInt());
        float fw = Math.max(40.0f, font.width(text) + 18.0f);
        float fh = FaTheme.FIELD_H;
        float fx = fieldX(fw), fy = fieldY();
        drawLabel(ctx, x, centerY, fx - x - FaTheme.ROW_PAD - 6.0f, alpha);
        boolean hot = binding || ctx.hovered(fx, fy, fw, fh);
        ctx.draw.rect(fx, fy, fw, fh, FaTheme.FIELD_RADIUS, FaTheme.alpha(hot ? FaTheme.FIELD_HOVER : FaTheme.FIELD, alpha));
        int color = binding ? FaTheme.ACCENT : hot ? FaTheme.FIELD_TEXT_HOVER : FaTheme.FIELD_TEXT;
        ctx.draw.textCenter(text, fx + fw * 0.5f, centerY, font, FaTheme.alpha(color, alpha));
        lastFieldX = fx;
        lastFieldW = fw;
    }

    private float lastFieldX, lastFieldW;

    @Override
    boolean mouseClicked(FaContext ctx, double mx, double my, int button) {
        if (!shown || !FaContext.inside(mx, my, lastFieldX, fieldY(), lastFieldW, FaTheme.FIELD_H)) return false;
        if (button == 1) {
            setter.accept(0);
            binding = false;
            ctx.gui.focus(null);
            return true;
        }
        if (button != 0) return false;
        binding = !binding;
        ctx.gui.focus(binding ? this : null);
        return true;
    }

    @Override
    public boolean keyPressed(int key, int modifiers) {
        if (!binding) return false;
        boolean clear = key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_DELETE || key == GLFW.GLFW_KEY_BACKSPACE;
        setter.accept(clear ? 0 : key);
        binding = false;
        return true;
    }

    @Override
    public void focusLost() {
        binding = false;
    }
}
