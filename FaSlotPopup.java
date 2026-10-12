package wtf.carnation.ui.fatality;

import wtf.carnation.module.value.impl.SlotValue;

/** Hotbar layout editor: nine slots plus the offhand on top, the roles for the chosen slot below. */
final class FaSlotPopup extends FaPopup {
    private static final float PAD = 8.0f;
    private static final float CELL = 18.0f;
    private static final float CELL_GAP = 2.0f;
    private static final float OFFHAND_GAP = 8.0f;
    private static final float ROLE_H = 15.0f;
    private static final int COLUMNS = 3;
    private static final int OFFHAND = -1;

    private final SlotValue slots;
    private int selected;

    FaSlotPopup(Object owner, SlotValue slots) {
        super(owner);
        this.slots = slots;
        w = PAD * 2.0f + SlotValue.SLOT_COUNT * (CELL + CELL_GAP) - CELL_GAP + OFFHAND_GAP + CELL;
        h = measure();
    }

    private String[] roles() {
        return selected == OFFHAND ? slots.getOffhandRoles() : slots.getSelectableRoles();
    }

    private float measure() {
        int rows = (roles().length + COLUMNS - 1) / COLUMNS;
        return PAD + CELL + 8.0f + rows * ROLE_H + PAD;
    }

    @Override
    protected void layout(FaContext ctx) {
        h = measure();
    }

    private float cellX(int slot) {
        return slot == OFFHAND
                ? x + PAD + SlotValue.SLOT_COUNT * (CELL + CELL_GAP) - CELL_GAP + OFFHAND_GAP
                : x + PAD + slot * (CELL + CELL_GAP);
    }

    @Override
    protected void draw(FaContext ctx, float alpha) {
        FaFont small = FaTheme.small();
        float cy = y + PAD;
        for (int slot = OFFHAND; slot < SlotValue.SLOT_COUNT; slot++) {
            float cx = cellX(slot);
            String role = slot == OFFHAND ? slots.getOffhand() : slots.getRole(slot);
            boolean active = slot == selected;
            boolean hovered = ctx.mouseX >= cx && ctx.mouseX <= cx + CELL && ctx.mouseY >= cy && ctx.mouseY <= cy + CELL;
            ctx.draw.rect(cx, cy, CELL, CELL, 3.0f, FaTheme.alpha(hovered ? 0xFF1F1F1F : FaTheme.FIELD, alpha));
            if (active) ctx.draw.outline(cx, cy, CELL, CELL, 3.0f, 0.7f, FaTheme.alpha(FaTheme.ACCENT, alpha));
            boolean empty = role == null || role.equalsIgnoreCase("None");
            String text = empty ? (slot == OFFHAND ? "O" : String.valueOf(slot + 1)) : initials(role);
            ctx.draw.textCenter(text, cx + CELL * 0.5f, cy + CELL * 0.5f, small,
                    FaTheme.alpha(empty ? FaTheme.MUTED : FaTheme.LABEL, alpha));
        }
        String[] roles = roles();
        float listY = cy + CELL + 8.0f;
        float columnW = (w - PAD * 2.0f) / COLUMNS;
        FaFont font = FaTheme.field();
        for (int i = 0; i < roles.length; i++) {
            float rx = x + PAD + (i % COLUMNS) * columnW;
            float ry = listY + (i / COLUMNS) * ROLE_H;
            boolean chosen = selected == OFFHAND ? slots.isOffhand(roles[i]) : slots.isRole(selected, roles[i]);
            boolean hovered = ctx.mouseX >= rx && ctx.mouseX <= rx + columnW && ctx.mouseY >= ry && ctx.mouseY < ry + ROLE_H;
            int color = chosen ? FaTheme.ACCENT : hovered ? FaTheme.LABEL_HOVER : FaTheme.FIELD_TEXT_HOVER;
            ctx.draw.textMid(FaTheme.fit(roles[i], font, columnW - 4.0f), rx, ry + ROLE_H * 0.5f, font,
                    FaTheme.alpha(color, alpha));
        }
    }

    private static String initials(String role) {
        String[] words = role.trim().split("\\s+");
        if (words.length >= 2) return ("" + words[0].charAt(0) + words[1].charAt(0)).toUpperCase();
        return role.length() <= 2 ? role : role.substring(0, 2);
    }

    @Override
    boolean mouseClicked(FaContext ctx, double mx, double my, int button) {
        float cy = y + PAD;
        for (int slot = OFFHAND; slot < SlotValue.SLOT_COUNT; slot++) {
            if (FaContext.inside(mx, my, cellX(slot), cy, CELL, CELL)) {
                if (button == 1) {
                    if (slot == OFFHAND) slots.setOffhand("None");
                    else slots.setRole(slot, "None");
                }
                selected = slot;
                return true;
            }
        }
        if (button != 0) return true;
        String[] roles = roles();
        float listY = cy + CELL + 8.0f;
        float columnW = (w - PAD * 2.0f) / COLUMNS;
        for (int i = 0; i < roles.length; i++) {
            float rx = x + PAD + (i % COLUMNS) * columnW;
            float ry = listY + (i / COLUMNS) * ROLE_H;
            if (FaContext.inside(mx, my, rx, ry, columnW, ROLE_H)) {
                if (selected == OFFHAND) slots.setOffhand(roles[i]);
                else slots.setRole(selected, roles[i]);
                return true;
            }
        }
        return true;
    }
}
