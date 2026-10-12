package wtf.carnation.ui.fatality;

import wtf.carnation.module.value.Value;
import wtf.carnation.module.value.impl.BoolValue;
import wtf.carnation.module.value.impl.ModeValue;
import wtf.carnation.module.value.impl.MultiBoolValue;

import java.util.ArrayList;
import java.util.List;

/** Dropdown field: dim value text and a chevron; opens a list popup underneath. */
final class FaComboRow extends FaRow {
    private final FaListPopup.Model model;
    private final java.util.function.Supplier<String> summary;
    private final FaMotion chevron = new FaMotion(0.0f);

    private FaComboRow(Value<?> value, FaListPopup.Model model, java.util.function.Supplier<String> summary) {
        super(value, value.getDisplayName());
        this.model = model;
        this.summary = summary;
    }

    static FaComboRow single(ModeValue mode) {
        FaListPopup.Model model = new FaListPopup.Model() {
            private String[] modes() {
                String[] modes = mode.getAvailableModes();
                return modes == null ? new String[0] : modes;
            }

            @Override public int size() { return modes().length; }
            @Override public String label(int index) { return wtf.carnation.module.value.impl.ModeValue.display(modes()[index]); }
            @Override public boolean selected(int index) {
                return modes()[index].equalsIgnoreCase(mode.getAvailableModeName());
            }
            @Override public void click(int index) { mode.setMode(modes()[index]); }
            @Override public boolean multi() { return false; }
        };
        return new FaComboRow(mode, model, () -> wtf.carnation.module.value.impl.ModeValue.display(mode.getAvailableModeName()));
    }

    static FaComboRow multi(MultiBoolValue multi) {
        FaListPopup.Model model = new FaListPopup.Model() {
            private List<BoolValue> options() {
                List<BoolValue> available = new ArrayList<>();
                for (BoolValue option : multi.options) if (option.isAvailable()) available.add(option);
                return available;
            }

            @Override public int size() { return options().size(); }
            @Override public String label(int index) { return options().get(index).getDisplayName(); }
            @Override public boolean selected(int index) { return Boolean.TRUE.equals(options().get(index).getValue()); }
            @Override public void click(int index) {
                BoolValue option = options().get(index);
                multi.set(option.getName(), !Boolean.TRUE.equals(option.getValue()));
            }
            @Override public boolean multi() { return true; }
        };
        return new FaComboRow(multi, model, () -> {
            StringBuilder text = new StringBuilder();
            for (BoolValue option : multi.options) {
                if (!option.isAvailable() || !Boolean.TRUE.equals(option.getValue())) continue;
                if (!text.isEmpty()) text.append(", ");
                text.append(option.getDisplayName());
            }
            return text.isEmpty() ? "None" : text.toString();
        });
    }

    @Override
    protected void draw(FaContext ctx, float x, float centerY, float w, float alpha) {
        float fw = FaTheme.FIELD_W, fh = FaTheme.FIELD_H;
        float fx = fieldX(fw), fy = fieldY();
        drawLabel(ctx, x, centerY, fx - x - FaTheme.ROW_PAD - 6.0f, alpha);
        boolean open = ctx.gui.isPopupOpen(this);
        boolean hot = open || ctx.hovered(fx, fy, fw, fh);
        drawField(ctx, fx, fy, fw, fh, summary.get(), hot, alpha);
        float turn = chevron.toward(open ? 1.0f : 0.0f, ctx.dt, 18.0f);
        ctx.draw.icon(turn > 0.5f ? FaTheme.I_CHEVRON_UP : FaTheme.I_CHEVRON_DOWN, fx + fw - 9.0f, centerY,
                9.5f, FaTheme.alpha(FaTheme.CHEVRON, alpha));
    }

    static void drawField(FaContext ctx, float fx, float fy, float fw, float fh, String text, boolean hot,
                          float alpha) {
        ctx.draw.rect(fx, fy, fw, fh, FaTheme.FIELD_RADIUS, FaTheme.alpha(hot ? FaTheme.FIELD_HOVER : FaTheme.FIELD, alpha));
        FaFont font = FaTheme.field();
        String shown = FaTheme.fit(text, font, fw - 24.0f);
        ctx.draw.textMid(shown, fx + 7.0f, fy + fh * 0.5f, font,
                FaTheme.alpha(hot ? FaTheme.FIELD_TEXT_HOVER : FaTheme.FIELD_TEXT, alpha));
    }

    @Override
    boolean mouseClicked(FaContext ctx, double mx, double my, int button) {
        float fx = fieldX(FaTheme.FIELD_W), fy = fieldY();
        if (button != 0 || !shown || !FaContext.inside(mx, my, fx, fy, FaTheme.FIELD_W, FaTheme.FIELD_H)) {
            return false;
        }
        ctx.gui.togglePopup(this, () -> new FaListPopup(this, model, FaTheme.FIELD_W), fx, fy, FaTheme.FIELD_W,
                FaTheme.FIELD_H, false);
        return true;
    }

    @Override
    boolean matches(String query) {
        if (super.matches(query)) return true;
        for (int i = 0; i < model.size(); i++) if (model.label(i).toLowerCase().contains(query)) return true;
        return false;
    }
}
