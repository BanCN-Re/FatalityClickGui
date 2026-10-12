package wtf.carnation.ui.fatality;

import org.lwjgl.glfw.GLFW;
import wtf.carnation.Client;
import wtf.carnation.module.Module;
import wtf.carnation.module.value.impl.BoolValue;

/**
 * A module's own first row: "Enabled", with the key bind and the array-list visibility as small
 * extras left of the checkbox, where Fatality puts its per-feature gear and color chips.
 * Click the key to bind (right-click clears); click the eye to hide the module from the list.
 */
final class FaEnableRow extends FaCheckRow implements FaKeySink {
    private final Module module;
    private final BoolValue hide;
    private final FaMotion keyHover = new FaMotion(0.0f);
    private final FaMotion eyeHover = new FaMotion(0.0f);
    private FaClickGui gui;
    private boolean binding;
    private float keyX, keyW, eyeX;

    FaEnableRow(Module module, BoolValue hide) {
        super(null, "Enabled", module::isEnabled, enabled -> {
            if (module.isEnabled() != enabled) module.toggle();
        });
        this.module = module;
        this.hide = hide;
    }

    @Override
    protected float accessory(FaContext ctx, float right, float centerY, float alpha) {
        float used = drawKey(ctx, right, centerY, alpha);
        if (hide != null) used += drawEye(ctx, right - used, centerY, alpha);
        return used;
    }

    private float drawKey(FaContext ctx, float right, float centerY, float alpha) {
        int key = module.getKey();
        boolean hot = binding || ctx.hovered(keyX, centerY - 7.0f, keyW, 14.0f);
        float h = keyHover.toward(hot ? 1.0f : 0.0f, ctx.dt, 16.0f);
        if (binding || key > 0) {
            FaFont font = FaTheme.small();
            String text = binding ? "..." : FaTheme.fit(FaKeyRow.keyName(key), font, 60.0f);
            keyW = Math.max(18.0f, font.width(text) + 10.0f);
            keyX = right - keyW;
            ctx.draw.rect(keyX, centerY - 6.5f, keyW, 13.0f, 2.0f,
                    FaTheme.alpha(FaTheme.mix(FaTheme.FIELD, FaTheme.FIELD_HOVER, h), alpha));
            int color = binding ? FaTheme.ACCENT : FaTheme.mix(FaTheme.FIELD_TEXT, FaTheme.FIELD_TEXT_HOVER, h);
            ctx.draw.textCenter(text, keyX + keyW * 0.5f, centerY, font, FaTheme.alpha(color, alpha));
        } else {
            keyW = 14.0f;
            keyX = right - keyW;
            ctx.draw.icon(FaTheme.I_KEYBOARD, keyX + keyW * 0.5f, centerY, 10.5f,
                    FaTheme.alpha(FaTheme.mix(FaTheme.ICON_FAINT, FaTheme.ICON_HOVER, h), alpha));
        }
        return keyW + 5.0f;
    }

    private float drawEye(FaContext ctx, float right, float centerY, float alpha) {
        boolean hidden = Boolean.TRUE.equals(hide.getValue());
        eyeX = right - 14.0f;
        float h = eyeHover.toward(ctx.hovered(eyeX, centerY - 7.0f, 14.0f, 14.0f) ? 1.0f : 0.0f, ctx.dt, 16.0f);
        // Visible is the default and stays faint; hidden is the exception and reads a little louder.
        int base = hidden ? FaTheme.ICON : FaTheme.ICON_FAINT;
        ctx.draw.icon(hidden ? FaTheme.I_EYE_OFF : FaTheme.I_EYE, eyeX + 7.0f, centerY, 10.5f,
                FaTheme.alpha(FaTheme.mix(base, FaTheme.ICON_HOVER, h), alpha));
        return 14.0f + 4.0f;
    }

    @Override
    boolean mouseClicked(FaContext ctx, double mx, double my, int button) {
        if (shown && FaContext.inside(mx, my, keyX - 2.0f, centerY() - 8.0f, keyW + 4.0f, 16.0f)) {
            if (button == 1) {
                module.setKey(0);
                binding = false;
                ctx.gui.focus(null);
                save();
            } else if (button == 0) {
                binding = !binding;
                gui = ctx.gui;
                ctx.gui.focus(binding ? this : null);
            }
            return true;
        }
        if (hide != null && shown && FaContext.inside(mx, my, eyeX - 2.0f, centerY() - 8.0f, 18.0f, 16.0f)) {
            if (button == 0) hide.setValue(!Boolean.TRUE.equals(hide.getValue()));
            return true;
        }
        return super.mouseClicked(ctx, mx, my, button);
    }

    @Override
    public boolean keyPressed(int key, int modifiers) {
        if (!binding) return false;
        boolean clear = key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_DELETE || key == GLFW.GLFW_KEY_BACKSPACE;
        module.setKey(clear ? 0 : key);
        binding = false;
        save();
        if (gui != null) gui.focus(null);
        return true;
    }

    @Override
    public void focusLost() {
        binding = false;
    }

    private static void save() {
        if (Client.instance != null && Client.instance.getConfigManager() != null) {
            Client.instance.getConfigManager().saveAllConfig();
        }
    }
}
