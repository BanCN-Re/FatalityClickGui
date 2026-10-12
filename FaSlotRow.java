package wtf.carnation.ui.fatality;

import wtf.carnation.module.value.impl.SlotValue;

/** Hotbar layout value: a summary field that opens the slot editor. */
final class FaSlotRow extends FaRow {
    private final SlotValue slots;

    FaSlotRow(SlotValue slots) {
        super(slots, slots.getDisplayName());
        this.slots = slots;
    }

    @Override
    protected void draw(FaContext ctx, float x, float centerY, float w, float alpha) {
        float fw = FaTheme.FIELD_W, fh = FaTheme.FIELD_H;
        float fx = fieldX(fw), fy = fieldY();
        drawLabel(ctx, x, centerY, fx - x - FaTheme.ROW_PAD - 6.0f, alpha);
        boolean open = ctx.gui.isPopupOpen(this);
        int count = slots.configuredSlots();
        FaComboRow.drawField(ctx, fx, fy, fw, fh, count == 0 ? "None" : count + (count == 1 ? " slot" : " slots"),
                open || ctx.hovered(fx, fy, fw, fh), alpha);
        ctx.draw.icon(open ? FaTheme.I_CHEVRON_UP : FaTheme.I_CHEVRON_DOWN, fx + fw - 8.0f, centerY, 9.0f,
                FaTheme.alpha(FaTheme.CHEVRON, alpha));
    }

    @Override
    boolean mouseClicked(FaContext ctx, double mx, double my, int button) {
        float fx = fieldX(FaTheme.FIELD_W), fy = fieldY();
        if (button != 0 || !shown || !FaContext.inside(mx, my, fx, fy, FaTheme.FIELD_W, FaTheme.FIELD_H)) return false;
        ctx.gui.togglePopup(this, () -> new FaSlotPopup(this, slots), fx, fy, FaTheme.FIELD_W, FaTheme.FIELD_H, false);
        return true;
    }
}
