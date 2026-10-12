package wtf.carnation.ui.fatality;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import wtf.carnation.Client;
import wtf.carnation.module.Module;
import wtf.carnation.module.impl.render.ESP;
import wtf.carnation.module.impl.render.Nametags;
import wtf.carnation.ui.clickgui.SettingsPanel;

import java.awt.Color;
import java.util.List;
import java.util.Locale;

/**
 * The Render tab's "Preview" box, after Fatality's: your own player model (your skin, armor and
 * held items) showing what the visual modules do to players. ESP draws its glow, or the plain
 * white outline without its shader, around the model's real silhouette; Nametags adds the tag
 * above the head. Drag to turn the model; it looks toward the cursor while you hover it.
 */
final class FaPreviewCard extends FaBox {
    static final float HEIGHT = 238.0f;
    private static final float FOOTER = 24.0f;
    private static final float TITLE_RISE = 1.5f;
    private static final float GLOW_RADIUS = 9.0f;
    private static final float PILL_H = 13.0f;

    /** Offline previews show every effect with stand-in data; the game always reads the modules. */
    static boolean demo;

    private final FaMotion x = new FaMotion(Float.NaN);
    private final FaMotion y = new FaMotion(Float.NaN);
    private final FaMotion headYaw = new FaMotion(0.0f);
    private final FaMotion headPitch = new FaMotion(0.0f);
    private final FaMotion[] pillHover = {new FaMotion(0.0f), new FaMotion(0.0f), new FaMotion(0.0f)};
    private final float[][] pills = new float[3][];
    private float targetX, targetY;
    private float lastX, lastY, lastW;
    private boolean drawn;
    private float yaw = 22.0f, spin;
    private boolean dragging;
    private double dragX;

    @Override
    float targetHeight() {
        return HEIGHT;
    }

    @Override
    void place(float targetX, float targetY, boolean snap) {
        this.targetX = targetX;
        this.targetY = targetY;
        if (snap || Float.isNaN(x.value())) {
            x.snap(targetX);
            y.snap(targetY);
        }
    }

    @Override
    float targetY() {
        return targetY;
    }

    // ── State of the effects being previewed ────────────────────────────────

    private record Effects(boolean esp, boolean shader, boolean gradient, boolean nametags) {
    }

    private static Effects effects() {
        if (demo) return new Effects(true, true, true, true);
        ESP esp = module(ESP.class);
        Nametags nametags = module(Nametags.class);
        return new Effects(esp != null && esp.isEnabled(), ESP.shader.get(), ESP.gradient.get(),
                nametags != null && nametags.isEnabled());
    }

    private static <T extends Module> T module(Class<T> type) {
        if (Client.instance == null || Client.instance.getModuleManager() == null) return null;
        return Client.instance.getModuleManager().getModule(type);
    }

    private static int themeColor(int index) {
        try {
            Color color = SettingsPanel.getColor(index);
            if (color != null) return 0xFF000000 | (color.getRGB() & 0xFFFFFF);
        } catch (RuntimeException | LinkageError ignored) {
            // Outside the game the Liquid theme is not available; fall through to the accent.
        }
        return index == 0 ? FaTheme.ACCENT : 0xFF8A6CFF;
    }

    // ── Drawing ─────────────────────────────────────────────────────────────

    @Override
    void render(FaContext ctx, float originX, float originY, float width, float viewTop, float viewBottom, float alpha) {
        float gx = originX + x.toward(targetX, ctx.dt, 16.0f);
        float gy = originY + y.toward(targetY, ctx.dt, 16.0f);
        lastX = gx;
        lastY = gy;
        lastW = width;
        drawn = gy + HEIGHT >= viewTop && gy <= viewBottom;
        if (!drawn) return;

        FaDraw draw = ctx.draw;
        draw.rect(gx, gy, width, HEIGHT, FaTheme.GROUP_RADIUS, FaTheme.alpha(FaTheme.GROUP, alpha));
        draw.outline(gx, gy, width, HEIGHT, FaTheme.GROUP_RADIUS, 0.5f, FaTheme.alpha(FaTheme.GROUP_BORDER, alpha));
        draw.textMid("Preview", gx + 9.5f, gy + TITLE_RISE, FaTheme.title(), FaTheme.alpha(FaTheme.TITLE, alpha));

        Effects effects = effects();
        float areaX = gx + 6.0f, areaY = gy + 10.0f, areaW = width - 12.0f, areaH = HEIGHT - 10.0f - FOOTER;
        // Leave headroom for the name tag and equipment row, the way they float in the world.
        float modelH = Math.min(areaH - 58.0f, areaW * 1.45f);
        float feetX = gx + width * 0.5f, feetY = areaY + areaH - 8.0f;
        float headTop = feetY - modelH;

        updatePose(ctx, areaX, areaY, areaW, areaH, feetX, feetY - modelH * 0.9f);
        // A soft contact shadow grounds the model on the card.
        draw.shadow(feetX - modelH * 0.17f, feetY - 1.5f, modelH * 0.34f, 3.0f, 1.5f, 5.0f,
                FaTheme.alpha(0x90000000, alpha));
        int esp = !effects.esp ? FaDraw.ESP_NONE : effects.shader ? FaDraw.ESP_GLOW : FaDraw.ESP_OUTLINE;
        int colorA = themeColor(0);
        int colorB = effects.gradient ? themeColor(1) : colorA;
        FaDraw.PlayerView view = new FaDraw.PlayerView(yaw, headYaw.value(), headPitch.value(), esp, colorA, colorB,
                GLOW_RADIUS);
        LocalPlayer player = Minecraft.getInstance() == null ? null : Minecraft.getInstance().player;
        boolean shown = draw.player(areaX, areaY, areaW, areaH, feetX, feetY, modelH, view);
        if (!shown) {
            draw.icon(FaTheme.I_PLAYER, feetX, areaY + areaH * 0.42f, 22.0f, FaTheme.alpha(0xFF2E2E2E, alpha));
            draw.textCenter(player == null ? "Join a world to preview" : "Preview unavailable", feetX,
                    areaY + areaH * 0.42f + 22.0f, FaTheme.small(), FaTheme.alpha(FaTheme.MUTED, alpha));
        }
        if (effects.nametags && (player != null || demo)) {
            drawNametag(ctx, player, feetX, headTop - 7.0f, alpha);
        }
        drawFooter(ctx, gx, gy + HEIGHT - FOOTER, width, effects, alpha);
    }

    private void updatePose(FaContext ctx, float ax, float ay, float aw, float ah, float eyeX, float eyeY) {
        if (dragging) {
            // Holding still before letting go should not fling the model.
            spin *= (float) Math.exp(-ctx.dt * 18.0f);
        } else {
            yaw += spin * ctx.dt;
            spin *= (float) Math.exp(-ctx.dt * 3.5f);
            if (Math.abs(spin) < 0.5f) spin = 0.0f;
        }
        boolean hovered = !dragging && ctx.hovered(ax, ay, aw, ah);
        // Like the inventory screen: the head turns toward the cursor, a little.
        float targetYaw = hovered ? (float) Math.toDegrees(Math.atan((eyeX - ctx.mouseX) / 40.0f)) * 0.35f : 0.0f;
        float targetPitch = hovered ? -(float) Math.toDegrees(Math.atan((eyeY - ctx.mouseY) / 40.0f)) * 0.35f : 0.0f;
        headYaw.toward(targetYaw, ctx.dt, 9.0f);
        headPitch.toward(targetPitch, ctx.dt, 9.0f);
    }

    /** The in-world Nametags look: a dark label with name, health and distance, equipment above. */
    private void drawNametag(FaContext ctx, LocalPlayer player, float centerX, float bottom, float alpha) {
        FaDraw draw = ctx.draw;
        String name = player != null ? player.getName().getString() : "Steve";
        float health = player != null ? Math.max(0.0f, player.getHealth()) : 20.0f;
        float maxHealth = player != null ? Math.max(1.0f, player.getMaxHealth()) : 20.0f;
        float absorption = player != null ? Math.max(0.0f, player.getAbsorptionAmount()) : 0.0f;
        FaFont nameFont = FaTheme.label(), detailFont = FaTheme.small();
        String healthText = String.format(Locale.ROOT, "%.1f HP", health);
        String absorptionText = absorption > 0.0f ? String.format(Locale.ROOT, " +%.1f", absorption) : "";
        String distanceText = "0.0 m";
        float healthW = detailFont.width(healthText), absorptionW = detailFont.width(absorptionText);
        float detailsW = healthW + absorptionW + 9.0f + detailFont.width(distanceText);
        float w = Math.max(nameFont.width(name), detailsW) + 12.0f, h = 27.0f;
        float x = centerX - w * 0.5f, y = bottom - h;

        draw.shadow(x, y, w, h, 5.0f, 6.0f, FaTheme.alpha(0x70000000, alpha));
        draw.rect(x, y, w, h, 5.0f, FaTheme.alpha(0xD0121417, alpha));
        draw.outline(x, y, w, h, 5.0f, 0.5f, FaTheme.alpha(0x1FFFFFFF, alpha));
        boolean friend = Client.instance != null && Client.instance.getFriendManager() != null
                && Client.instance.getFriendManager().isFriend(name);
        draw.textCenter(name, centerX, y + 8.0f, nameFont, FaTheme.alpha(friend ? 0xFF55FF77 : 0xFFFFFFFF, alpha));
        float ratio = Math.max(0.0f, Math.min(1.0f, health / maxHealth));
        int healthColor = 0xFF000000 | (Color.HSBtoRGB(ratio / 3.0f, 0.65f, 1.0f) & 0xFFFFFF);
        float detailX = centerX - detailsW * 0.5f, detailY = y + 17.5f;
        draw.textMid(healthText, detailX, detailY, detailFont, FaTheme.alpha(healthColor, alpha));
        draw.textMid(absorptionText, detailX + healthW, detailY, detailFont, FaTheme.alpha(0xFFFFCF5C, alpha));
        draw.textMid(distanceText, detailX + healthW + absorptionW + 9.0f, detailY, detailFont,
                FaTheme.alpha(0xA5FFFFFF, alpha));
        float barW = w - 12.0f, barY = y + h - 3.5f;
        draw.rect(x + 6.0f, barY, barW, 1.5f, 0.75f, FaTheme.alpha(0x23FFFFFF, alpha));
        if (ratio > 0.0f) draw.rect(x + 6.0f, barY, barW * ratio, 1.5f, 0.75f, FaTheme.alpha(healthColor, alpha));

        // Equipment floats above the label. Items are drawn after the window at the end of the
        // frame, so they wait while a popup covers the card or the window is still fading in.
        if (player == null || alpha < 0.98f || ctx.gui.popupOver(lastX, lastY, lastW, HEIGHT)) return;
        List<ItemStack> equipment = List.of(player.getMainHandItem(), player.getItemBySlot(EquipmentSlot.HEAD),
                player.getItemBySlot(EquipmentSlot.CHEST), player.getItemBySlot(EquipmentSlot.LEGS),
                player.getItemBySlot(EquipmentSlot.FEET), player.getOffhandItem());
        if (equipment.stream().allMatch(ItemStack::isEmpty)) return;
        float size = 11.0f, step = 13.0f;
        float startX = centerX - (step * (equipment.size() - 1) + size) * 0.5f, itemY = y - size - 3.0f;
        for (int i = 0; i < equipment.size(); i++) {
            float itemX = startX + i * step;
            ItemStack stack = equipment.get(i);
            if (stack.isEmpty()) draw.rect(itemX + 3.5f, itemY + 5.0f, 4.0f, 1.5f, 0.75f, FaTheme.alpha(0x41B9C3CF, alpha));
            else draw.item(stack, itemX, itemY, size);
        }
    }

    private void drawFooter(FaContext ctx, float gx, float fy, float width, Effects effects, float alpha) {
        FaDraw draw = ctx.draw;
        FaFont font = FaTheme.small();
        String[] labels = {"ESP", "Shader", "Nametags"};
        boolean[] on = {effects.esp, effects.shader, effects.nametags};
        float px = gx + 10.0f, mid = fy + FOOTER * 0.5f - 1.0f;
        for (int i = 0; i < labels.length; i++) {
            // "Shader" only matters while ESP is on.
            if (i == 1 && !effects.esp) {
                pills[i] = null;
                continue;
            }
            float w = font.width(labels[i]) + 18.0f;
            pills[i] = new float[]{px, mid - PILL_H * 0.5f, w, PILL_H};
            float hover = pillHover[i].toward(ctx.hovered(px, mid - PILL_H * 0.5f, w, PILL_H) ? 1.0f : 0.0f, ctx.dt, 16.0f);
            draw.rect(px, mid - PILL_H * 0.5f, w, PILL_H, PILL_H * 0.5f,
                    FaTheme.alpha(FaTheme.mix(FaTheme.FIELD, 0xFF1F1F1F, hover), alpha));
            draw.rect(px + 6.0f, mid - 1.5f, 3.0f, 3.0f, 1.5f, FaTheme.alpha(on[i] ? FaTheme.ACCENT : 0xFF3A3A3A, alpha));
            int color = on[i] ? FaTheme.mix(FaTheme.LABEL, FaTheme.LABEL_HOVER, hover)
                    : FaTheme.mix(FaTheme.FIELD_TEXT, FaTheme.FIELD_TEXT_HOVER, hover);
            draw.textMid(labels[i], px + 12.0f, mid, font, FaTheme.alpha(color, alpha));
            px += w + 5.0f;
        }
        String hint = "Drag to turn";
        if (px + font.width(hint) + 6.0f <= gx + width - 10.0f) {
            draw.textRightMid(hint, gx + width - 10.0f, mid, font, FaTheme.alpha(0xFF3C3C3C, alpha));
        }
    }

    // ── Input ───────────────────────────────────────────────────────────────

    @Override
    boolean mouseClicked(FaContext ctx, double mx, double my, int button) {
        if (!drawn || !FaContext.inside(mx, my, lastX, lastY, lastW, HEIGHT)) return false;
        if (button != 0) return true;
        for (int i = 0; i < pills.length; i++) {
            float[] pill = pills[i];
            if (pill != null && FaContext.inside(mx, my, pill[0], pill[1], pill[2], pill[3])) {
                toggle(i);
                return true;
            }
        }
        if (my < lastY + HEIGHT - FOOTER) {
            dragging = true;
            dragX = mx;
            spin = 0.0f;
        }
        return true;
    }

    private static void toggle(int index) {
        switch (index) {
            case 0 -> {
                ESP esp = module(ESP.class);
                if (esp != null) esp.toggle();
            }
            case 1 -> ESP.shader.setValue(!ESP.shader.get());
            case 2 -> {
                Nametags nametags = module(Nametags.class);
                if (nametags != null) nametags.toggle();
            }
            default -> {
                return;
            }
        }
        if (Client.instance != null && Client.instance.getConfigManager() != null) {
            Client.instance.getConfigManager().saveAllConfig();
        }
    }

    @Override
    void mouseDragged(FaContext ctx, double mx, double my) {
        if (!dragging) return;
        float delta = (float) (mx - dragX);
        dragX = mx;
        yaw += delta * 1.6f;
        // Remember how fast it was turning so letting go keeps it spinning briefly.
        spin = ctx.dt > 0.0f ? delta * 1.6f / Math.max(ctx.dt, 1.0f / 240.0f) * 0.6f : 0.0f;
        spin = Math.max(-720.0f, Math.min(720.0f, spin));
    }

    @Override
    void mouseReleased() {
        dragging = false;
    }

    @Override
    boolean mouseScrolled(FaContext ctx, double mx, double my, double amount) {
        return false;
    }
}
