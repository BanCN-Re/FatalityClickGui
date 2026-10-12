package wtf.carnation.ui.fatality;

import wtf.carnation.module.value.impl.GroupValue;

import java.util.List;

/** A value group shown as a label with a gear; its children live in a popup, like Fatality's extras. */
final class FaGearRow extends FaRow {
    private final List<FaRow> children;
    private final FaMotion spin = new FaMotion(0.0f);

    FaGearRow(GroupValue group, List<FaRow> children) {
        super(group, group.getDisplayName());
        this.children = children;
    }

    @Override
    protected void draw(FaContext ctx, float x, float centerY, float w, float alpha) {
        float gx = right() - FaTheme.GEAR * 0.5f;
        drawLabel(ctx, x, centerY, gx - x - FaTheme.ROW_PAD - 12.0f, alpha);
        boolean open = ctx.gui.isPopupOpen(this);
        boolean hot = open || ctx.hovered(gx - 8, centerY - 8, 16, 16);
        float t = spin.toward(hot ? 1.0f : 0.0f, ctx.dt, 14.0f);
        int color = FaTheme.mix(FaTheme.ICON, FaTheme.ICON_HOVER, t);
        ctx.draw.icon(FaTheme.I_GEAR, gx, centerY, 10.5f, FaTheme.alpha(color, alpha));
    }

    @Override
    boolean mouseClicked(FaContext ctx, double mx, double my, int button) {
        if (button != 0 || !hit(mx, my)) return false;
        float gx = right() - FaTheme.GEAR;
        ctx.gui.togglePopup(this, () -> new FaRowsPopup(this, label, children), gx, centerY() - 6, FaTheme.GEAR, 12,
                true);
        return true;
    }

    @Override
    boolean matches(String query) {
        if (super.matches(query)) return true;
        for (FaRow child : children) if (child.matches(query)) return true;
        return false;
    }
}
