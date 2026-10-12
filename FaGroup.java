package wtf.carnation.ui.fatality;

import wtf.carnation.module.Group;
import wtf.carnation.module.Module;
import wtf.carnation.module.value.Mode;
import wtf.carnation.module.value.ModeContainer;
import wtf.carnation.module.value.Value;
import wtf.carnation.module.value.impl.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * One module as a Fatality group box: the title sits on the top edge, then an "Enabled" row with
 * the key bind and every value of the module as rows. Search can narrow the rows down.
 */
final class FaGroup extends FaBox {
    /** Title center relative to the group's top edge. */
    private static final float TITLE_RISE = 1.5f;

    final Module module;
    private final List<FaRow> rows = new ArrayList<>();
    private final boolean[] matched;
    private final FaMotion x = new FaMotion(Float.NaN);
    private final FaMotion y = new FaMotion(Float.NaN);
    private final FaMotion flash = new FaMotion(0.0f);
    private float targetX, targetY;
    private float lastX, lastY, lastW, lastH;
    private boolean drawn;
    private boolean filtered;

    FaGroup(Module module) {
        this.module = module;
        // The array-list "Hide" switch every module carries rides on the Enabled row as an eye.
        BoolValue hide = null;
        for (Value<?> value : module.getValues()) {
            if (value instanceof BoolValue bool && "Hide".equals(bool.getName())) {
                hide = bool;
                break;
            }
        }
        rows.add(new FaEnableRow(module, hide));
        Set<Value<?>> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        if (hide != null) seen.add(hide);
        for (Value<?> value : module.getValues()) add(value, seen, rows);
        matched = new boolean[rows.size()];
        java.util.Arrays.fill(matched, true);
    }

    private static void add(Value<?> value, Set<Value<?>> seen, List<FaRow> into) {
        if (value == null || !seen.add(value)) return;
        FaRow row = create(value, seen);
        if (row != null) into.add(row);
        if (value instanceof ModeContainer container) {
            for (Mode<?> mode : container.getModeHandlers()) {
                for (Value<?> child : mode.getValues()) add(child, seen, into);
            }
        }
    }

    private static FaRow create(Value<?> value, Set<Value<?>> seen) {
        if (value instanceof BoolValue bool) {
            return new FaCheckRow(bool, bool.getDisplayName(), () -> Boolean.TRUE.equals(bool.getValue()), bool::setValue);
        }
        if (value instanceof KeyValue key) {
            return new FaKeyRow(key, key.getDisplayName(), () -> key.getValue() == null ? 0 : key.getValue(), key::setValue);
        }
        if (value instanceof SlotValue slots) return new FaSlotRow(slots);
        if (value instanceof BoundsNumberValue bounds) return new FaRangeRow(bounds);
        if (value instanceof NumberValue number) return new FaSliderRow(number);
        if (value instanceof ModeValue mode) return FaComboRow.single(mode);
        if (value instanceof ColorValue color) return new FaColorRow(color);
        if (value instanceof TextValue text) return new FaTextRow(text);
        if (value instanceof MultiBoolValue multi) return FaComboRow.multi(multi);
        if (value instanceof GroupValue group) {
            List<FaRow> children = new ArrayList<>();
            List<Value<?>> members = group.getValue();
            if (members != null) for (Value<?> member : members) add(member, seen, children);
            return new FaGearRow(group, children);
        }
        return null;
    }

    String title() {
        return module.getDisplayName();
    }

    /** Narrows rows to a search query; returns whether the group should be shown at all. */
    boolean filter(String query) {
        filtered = !query.isEmpty();
        if (!filtered) {
            java.util.Arrays.fill(matched, true);
            return true;
        }
        boolean titleMatch = title().toLowerCase(Locale.ROOT).contains(query)
                || module.getName().toLowerCase(Locale.ROOT).contains(query);
        boolean any = titleMatch;
        for (int i = 0; i < rows.size(); i++) {
            matched[i] = titleMatch || i == 0 || rows.get(i).matches(query);
            if (i > 0 && matched[i] && !titleMatch) any = true;
        }
        return any;
    }

    float targetHeight() {
        float height = FaTheme.GROUP_TITLE_H + FaTheme.GROUP_BOTTOM;
        for (int i = 0; i < rows.size(); i++) if (matched[i]) height += rows.get(i).targetHeight();
        return height;
    }

    void place(float targetX, float targetY, boolean snap) {
        this.targetX = targetX;
        this.targetY = targetY;
        if (snap || Float.isNaN(x.value())) {
            x.snap(targetX);
            y.snap(targetY);
        }
    }

    float targetY() {
        return targetY;
    }

    void flash() {
        flash.snap(1.0f);
    }

    /**
     * Draws at the animated position relative to {@code originX/originY} (content origin minus
     * scroll). Rows outside the visible band are skipped but still animate.
     */
    void render(FaContext ctx, float originX, float originY, float width, float viewTop, float viewBottom, float alpha) {
        float gx = originX + x.toward(targetX, ctx.dt, 16.0f);
        float gy = originY + y.toward(targetY, ctx.dt, 16.0f);
        float height = FaTheme.GROUP_TITLE_H + FaTheme.GROUP_BOTTOM;
        float[] rowHeights = new float[rows.size()];
        for (int i = 0; i < rows.size(); i++) {
            rowHeights[i] = matched[i] ? rows.get(i).height(ctx.dt) : 0.0f;
            height += rowHeights[i];
        }
        lastX = gx;
        lastY = gy;
        lastW = width;
        lastH = height;
        drawn = gy + height >= viewTop && gy <= viewBottom;
        float glow = flash.toward(0.0f, ctx.dt, 2.2f);
        if (!drawn) {
            for (FaRow row : rows) row.shown = false;
            return;
        }

        ctx.draw.rect(gx, gy, width, height, FaTheme.GROUP_RADIUS, FaTheme.alpha(FaTheme.GROUP, alpha));
        ctx.draw.outline(gx, gy, width, height, FaTheme.GROUP_RADIUS, 0.5f,
                FaTheme.alpha(FaTheme.mix(FaTheme.GROUP_BORDER, FaTheme.ACCENT_DIM, glow), alpha));

        // Like the reference, the title rides on the top edge rather than inside a header band.
        FaFont titleFont = FaTheme.title();
        int titleColor = module.getGroup() != Group.User ? FaTheme.ORANGE : FaTheme.TITLE;
        ctx.draw.textMid(FaTheme.fit(title(), titleFont, width - 20.0f), gx + 9.5f, gy + TITLE_RISE, titleFont,
                FaTheme.alpha(FaTheme.mix(titleColor, FaTheme.ACCENT, glow), alpha));

        float rowY = gy + FaTheme.GROUP_TITLE_H;
        for (int i = 0; i < rows.size(); i++) {
            float rowHeight = rowHeights[i];
            FaRow row = rows.get(i);
            if (rowHeight <= 0.01f || rowY + rowHeight < viewTop || rowY > viewBottom) {
                row.shown = false;
                rowY += rowHeight;
                continue;
            }
            float rowAlpha = alpha * row.visibility();
            if (rowHeight < FaTheme.ROW_H - 0.05f) {
                try (var clip = ctx.draw.clip(gx, rowY, width, rowHeight)) {
                    row.render(ctx, gx, rowY, width, rowAlpha);
                }
            } else {
                row.render(ctx, gx, rowY, width, rowAlpha);
            }
            rowY += rowHeight;
        }
    }

    boolean contains(double mx, double my) {
        return drawn && FaContext.inside(mx, my, lastX, lastY, lastW, lastH);
    }

    boolean mouseClicked(FaContext ctx, double mx, double my, int button) {
        if (!contains(mx, my)) return false;
        for (int i = 0; i < rows.size(); i++) {
            if (matched[i] && rows.get(i).mouseClicked(ctx, mx, my, button)) return true;
        }
        return true;
    }

    void mouseDragged(FaContext ctx, double mx, double my) {
        for (FaRow row : rows) row.mouseDragged(ctx, mx, my);
    }

    void mouseReleased() {
        for (FaRow row : rows) row.mouseReleased();
    }

    boolean mouseScrolled(FaContext ctx, double mx, double my, double amount) {
        if (!contains(mx, my)) return false;
        for (int i = 0; i < rows.size(); i++) {
            if (matched[i] && rows.get(i).mouseScrolled(ctx, mx, my, amount)) return true;
        }
        return false;
    }
}
