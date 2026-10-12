package wtf.carnation.ui.fatality;

import net.minecraft.util.Util;
import wtf.carnation.Client;
import wtf.carnation.manager.config.ConfigManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Named configs: click to load, the cross deletes, the field at the bottom saves a new one. */
final class FaConfigPopup extends FaPopup implements FaKeySink {
    private static final float PAD = 8.0f;
    private static final float TITLE_H = 20.0f;
    private static final float ITEM_H = 17.0f;
    private static final float FIELD_H = 17.0f;
    private static final int MAX_ITEMS = 8;

    private final FaClickGui gui;
    private final FaTextInput name = new FaTextInput(32);
    private List<String> configs = List.of();
    private boolean editing;
    private float scroll;

    FaConfigPopup(Object owner, FaClickGui gui) {
        super(owner);
        this.gui = gui;
        w = 196.0f;
        name.onSubmit(this::save).onCancel(() -> {
            editing = false;
            gui.focus(null);
        });
        refresh();
    }

    private void refresh() {
        List<String> names = new ArrayList<>();
        var manager = Client.instance == null ? null : Client.instance.getConfigManager();
        if (manager != null) {
            for (String file : manager.listConfigFileNames()) if (!manager.isBuiltinConfig(file)) names.add(file);
        }
        configs = names;
        h = PAD + TITLE_H + Math.max(1, Math.min(MAX_ITEMS, configs.size())) * ITEM_H + 6.0f + FIELD_H + PAD;
    }

    private float listY() { return y + PAD + TITLE_H; }
    private float fieldY() { return listY() + Math.max(1, Math.min(MAX_ITEMS, configs.size())) * ITEM_H + 6.0f; }

    @Override
    protected void draw(FaContext ctx, float alpha) {
        FaFont font = FaTheme.field();
        ctx.draw.textMid("Configs", x + PAD, y + PAD + TITLE_H * 0.5f - 2.0f, FaTheme.title(), FaTheme.alpha(FaTheme.TITLE, alpha));
        boolean folderHover = ctx.mouseX >= x + w - PAD - 12 && ctx.mouseX <= x + w - PAD && ctx.mouseY >= y + PAD
                && ctx.mouseY <= y + PAD + 14;
        ctx.draw.icon(FaTheme.I_FOLDER, x + w - PAD - 6.0f, y + PAD + TITLE_H * 0.5f - 2.0f, 9.5f,
                FaTheme.alpha(folderHover ? FaTheme.ICON_HOVER : FaTheme.ICON, alpha));
        float ly = listY();
        if (configs.isEmpty()) {
            ctx.draw.textMid("No saved configs", x + PAD, ly + ITEM_H * 0.5f, font, FaTheme.alpha(FaTheme.MUTED, alpha));
        }
        try (var clip = ctx.draw.clip(x, ly, w, Math.min(MAX_ITEMS, Math.max(1, configs.size())) * ITEM_H)) {
            for (int i = 0; i < configs.size(); i++) {
                float iy = ly + i * ITEM_H - scroll;
                boolean hovered = ctx.mouseX >= x && ctx.mouseX <= x + w && ctx.mouseY >= iy && ctx.mouseY < iy + ITEM_H;
                if (hovered) ctx.draw.rect(x + 3.0f, iy, w - 6.0f, ITEM_H, 3.0f, FaTheme.alpha(FaTheme.FIELD_HOVER, alpha));
                String label = configs.get(i).replaceFirst("(?i)\\.json$", "");
                ctx.draw.textMid(FaTheme.fit(label, font, w - PAD * 2 - 16), x + PAD, iy + ITEM_H * 0.5f, font,
                        FaTheme.alpha(hovered ? FaTheme.LABEL_HOVER : FaTheme.FIELD_TEXT_HOVER, alpha));
                boolean deleteHover = hovered && ctx.mouseX >= x + w - PAD - 12;
                ctx.draw.icon(FaTheme.I_CLOSE, x + w - PAD - 5.0f, iy + ITEM_H * 0.5f, 8.0f,
                        FaTheme.alpha(deleteHover ? FaTheme.ACCENT : hovered ? FaTheme.ICON : 0x00000000, alpha));
            }
        }
        float fy = fieldY();
        float fw = w - PAD * 2 - 20.0f;
        ctx.draw.rect(x + PAD, fy, fw, FIELD_H, FaTheme.FIELD_RADIUS, FaTheme.alpha(editing ? FaTheme.FIELD_HOVER : FaTheme.FIELD, alpha));
        name.render(ctx.draw, x + PAD + 6.0f, fy + FIELD_H * 0.5f, fw - 12.0f, font, FaTheme.alpha(FaTheme.LABEL, alpha),
                editing, "New config name", FaTheme.alpha(FaTheme.MUTED, alpha));
        boolean plusHover = ctx.mouseX >= x + w - PAD - 16 && ctx.mouseX <= x + w - PAD && ctx.mouseY >= fy
                && ctx.mouseY <= fy + FIELD_H;
        ctx.draw.rect(x + w - PAD - 16.0f, fy, 16.0f, FIELD_H, FaTheme.FIELD_RADIUS,
                FaTheme.alpha(plusHover ? FaTheme.ACCENT : FaTheme.FIELD, alpha));
        ctx.draw.icon(FaTheme.I_PLUS, x + w - PAD - 8.0f, fy + FIELD_H * 0.5f, 9.5f,
                FaTheme.alpha(plusHover ? 0xFFFFFFFF : FaTheme.ICON_HOVER, alpha));
    }

    @Override
    boolean mouseClicked(FaContext ctx, double mx, double my, int button) {
        if (button != 0) return true;
        if (FaContext.inside(mx, my, x + w - PAD - 12, y + PAD, 12, 14)) {
            Util.getPlatform().openFile(ConfigManager.dir);
            return true;
        }
        float fy = fieldY();
        if (FaContext.inside(mx, my, x + w - PAD - 16, fy, 16, FIELD_H)) {
            save(name.text());
            return true;
        }
        if (FaContext.inside(mx, my, x + PAD, fy, w - PAD * 2 - 20, FIELD_H)) {
            editing = true;
            name.focused();
            gui.focus(this);
            return true;
        }
        float ly = listY();
        for (int i = 0; i < configs.size(); i++) {
            float iy = ly + i * ITEM_H - scroll;
            if (!FaContext.inside(mx, my, x, iy, w, ITEM_H) || iy < ly - 1 || iy > ly + MAX_ITEMS * ITEM_H) continue;
            String file = configs.get(i);
            var manager = Client.instance.getConfigManager();
            if (mx >= x + w - PAD - 12) {
                manager.deleteConfigFile(file);
                gui.toast("Deleted " + file.replaceFirst("(?i)\\.json$", ""));
                refresh();
            } else {
                if (manager.loadNamedConfig(file)) {
                    gui.toast("Loaded " + file.replaceFirst("(?i)\\.json$", ""));
                    close();
                } else {
                    gui.toast(manager.lastRefusal());
                }
            }
            return true;
        }
        return true;
    }

    private void save(String raw) {
        String clean = raw == null ? "" : raw.trim().replaceAll("[^A-Za-z0-9_\\-. ]", "");
        if (clean.isEmpty()) return;
        if (!clean.toLowerCase(Locale.ROOT).endsWith(".json")) clean += ".json";
        var manager = Client.instance.getConfigManager();
        if (manager.isBuiltinConfig(clean)) {
            gui.toast("That name is reserved");
            return;
        }
        manager.saveNamedConfig(clean);
        gui.toast("Saved " + clean.replaceFirst("(?i)\\.json$", ""));
        name.set("");
        editing = false;
        gui.focus(null);
        refresh();
    }

    @Override
    boolean mouseScrolled(FaContext ctx, double mx, double my, double amount) {
        if (!contains(mx, my)) return false;
        float max = Math.max(0.0f, configs.size() * ITEM_H - MAX_ITEMS * ITEM_H);
        scroll = Math.max(0.0f, Math.min(max, scroll - (float) amount * ITEM_H));
        return true;
    }

    @Override
    public boolean keyPressed(int key, int modifiers) {
        return editing && name.keyPressed(key, modifiers);
    }

    @Override
    public boolean charTyped(char character) {
        return editing && name.charTyped(character);
    }

    @Override
    public void focusLost() {
        editing = false;
    }

    @Override
    protected void onClose() {
        if (editing) gui.focus(null);
    }
}
