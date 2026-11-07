package wtf.moonlight.ui.hud;

import imgui.ImDrawList;
import imgui.ImFont;
import imgui.ImGui;
import imgui.ImGuiIO;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;
import wtf.moonlight.module.impl.render.Watermark;
import wtf.moonlight.module.impl.render.Watermark.Item;
import wtf.moonlight.ui.clickgui.Colors;
import wtf.moonlight.ui.clickgui.Controls;
import wtf.moonlight.ui.imgui.Effects;
import wtf.moonlight.ui.imgui.Fonts;
import wtf.moonlight.ui.imgui.ImGuiManager;
import wtf.moonlight.ui.imgui.Textures;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

public final class WatermarkRenderer implements HudElement {
    private static final float FOLLOW = 12;
    private static final float BREATH = 0.45F;
    private static final float BREATH_HOLD = 0.18F;
    private static final float BREATH_RATE = 10;
    private static final float HEIGHT = 28;
    private static final float PAD = 9;
    private static final float PAD_AVATAR = 4;
    private static final float PAD_TEXT = 10;
    private static final float SEGMENT_GAP = 8;
    private static final float ICON_GAP = 2;
    private static final float STAT_GAP = 3;
    private static final float CENTER = 13.5F;
    private static final float BASELINE = 18;
    private static final float TEXT = 10;
    private static final float NAME = 10.67F;
    private static final float ICON = 15;
    private static final float SMALL_ICON = 13;
    private static final float AVATAR = 21;
    private static final float LOGO = 13.94F;
    private static final float LOGO_BASELINE = 18.76F;
    private static final float LOGO_SHIFT = 1.25F;
    private static final float LOGO_DROP = 1.24F;
    private static final String FPS = " FPS";
    private static final String LOW = "  (1%L)";
    private static final int WHITE = Colors.rgb(255, 255, 255);
    private static final int BLACK = Colors.rgb(0, 0, 0);
    private static final int INK = Colors.rgb(28, 30, 38);
    private static final int LOGO_ALPHA = 204;
    private static final int BACKGROUND = 179;
    private static final int SHADOW = Colors.rgba(0, 0, 0, 179);
    private static final int LIGHT_SHADOW = Colors.rgba(0, 0, 0, 77);
    private static final DateTimeFormatter CLOCK_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final Watermark module;
    private final Supplier<String> user;
    private final Supplier<Identifier> skin;
    private final Supplier<Snapshot> data;
    private final FrameStats stats = new FrameStats();
    private final Rolling fps = new Rolling();
    private final Rolling low = new Rolling();
    private final Rolling clock = new Rolling();
    private final Rolling ping = new Rolling();
    private final Rolling tps = new Rolling();
    private final Rolling speed = new Rolling();
    private final float[] slots = new float[Item.values().length];
    private final float[] holds = new float[Item.values().length];
    private final float[] dims = new float[Item.values().length];
    private List<Item> order = List.of();
    private Snapshot info = new Snapshot("--", -1, -1, "--", -1, "");
    private float progress;
    private float slide = 1;
    private float fade = 1;
    private float width;
    private float originX;
    private float originY;
    private float unit = 1;
    private boolean snap;
    private int ink = WHITE;
    private int accent;
    private @Nullable Rect bounds;

    public WatermarkRenderer(Watermark module, Supplier<String> user, Supplier<Identifier> skin, Supplier<Snapshot> data) {
        this.module = module;
        this.user = user;
        this.skin = skin;
        this.data = data;
        Arrays.fill(slots, Float.NaN);
    }

    @Override
    public Watermark module() {
        return module;
    }

    @Override
    public @Nullable Rect bounds() {
        return bounds;
    }

    @Override
    public void place(float x, float y) {
        ImGuiIO io = ImGui.getIO();
        module.anchor().drop(io.getDisplaySizeX(), io.getDisplaySizeY(), x, y, width, HEIGHT * unit, unit);
        snap = true;
    }

    @Override
    public void frame(Style style) {
        ImGuiIO io = ImGui.getIO();
        stats.add(io.getDeltaTime());
        bounds = null;
        float delta = Math.min(io.getDeltaTime(), 0.1F);
        boolean fresh = progress <= 0;
        List<Item> shown = module.items();
        boolean visible = module.isEnabled() && !shown.isEmpty();
        progress = Math.clamp(progress + (visible ? delta : -delta) / module.animation(), 0.0F, 1.0F);
        if (progress <= 0) return;
        breathe(shown, delta, fresh);

        boolean light = style.light();
        ink = light ? INK : WHITE;
        accent = style.accent();
        float k = module.scale() * style.scale() * ImGuiManager.displayScale();
        fps.update(String.valueOf(stats.fps()), delta, fresh);
        low.update(String.valueOf(stats.low()), delta, fresh);
        clock.update(LocalTime.now().format(CLOCK_FORMAT), delta, fresh);
        info = data.get();
        ping.update(info.ping() >= 0 ? String.valueOf(info.ping()) : "--", delta, fresh);
        tps.update(info.tps() >= 0 ? String.format(Locale.ROOT, "%.1f", info.tps()) : "--", delta, fresh);
        speed.update(info.speed() >= 0 ? String.format(Locale.ROOT, "%.1f", info.speed()) : "--", delta, fresh);
        float target = walk(null, shown, 0, 0, k);
        float height = HEIGHT * k;
        float targetX = module.anchor().left(io.getDisplaySizeX(), target, k);
        float targetY = module.anchor().top(io.getDisplaySizeY(), height, k);
        slide = fresh || unit != k ? 1 : 1 - (float) Math.exp(-FOLLOW * delta);
        width += (target - width) * slide;
        originX = snap ? targetX : originX + (targetX - originX) * slide;
        originY = snap ? targetY : originY + (targetY - originY) * slide;
        unit = k;
        snap = false;

        float left = Math.round(originX);
        float top = Math.round(originY);
        float revealed = width * (1 - (float) Math.pow(1 - progress, 3));
        if (revealed < 0.5F) return;
        float minX = module.anchor().right() ? left + width - revealed : left;
        float maxX = minX + revealed;
        float maxY = top + height;
        float rounding = Math.min(height, revealed) / 2;
        bounds = new Rect(minX, top, maxX, maxY, rounding);

        ImDrawList draw = ImGui.getBackgroundDrawList();
        Effects.blur(draw, minX, top, maxX, maxY, rounding, 0);
        Effects.shadow(draw, minX, top, maxX, maxY, rounding, 0, light ? LIGHT_SHADOW : SHADOW);
        draw.addRectFilled(minX, top, maxX, maxY, Colors.reAlpha(light ? WHITE : BLACK, Math.round(BACKGROUND * style.alpha())), rounding);
        draw.pushClipRect(minX, top, maxX, maxY, true);
        walk(draw, shown, left, top, k);
        draw.popClipRect();
    }

    private void breathe(List<Item> shown, float delta, boolean fresh) {
        if (!fresh && shown.size() == order.size() && shown.containsAll(order) && !shown.equals(order)) {
            for (int i = 0; i < shown.size(); i++) {
                if (order.get(i) != shown.get(i)) holds[shown.get(i).ordinal()] = BREATH_HOLD;
            }
        }
        order = shown;
        float follow = 1 - (float) Math.exp(-BREATH_RATE * delta);
        for (int i = 0; i < dims.length; i++) {
            holds[i] = Math.max(0, holds[i] - delta);
            dims[i] += ((holds[i] > 0 ? 1 : 0) - dims[i]) * follow;
        }
    }

    private float walk(@Nullable ImDrawList draw, List<Item> shown, float left, float top, float k) {
        float x = 0;
        float end = PAD_TEXT;
        boolean[] seen = new boolean[slots.length];
        for (Item item : shown) {
            boolean avatar = item == Item.AVATAR;
            float start = next(x, avatar ? PAD_AVATAR : PAD, k);
            float at = start;
            if (draw != null) {
                int index = item.ordinal();
                float slot = slots[index];
                slots[index] = Float.isNaN(slot) ? start / k : slot + (start / k - slot) * slide;
                at = slots[index] * k;
                seen[index] = true;
                fade = 1 - BREATH * dims[index];
            }
            x = start + segment(draw, item, left, top, at, k) - at;
            end = avatar ? PAD_AVATAR : PAD_TEXT;
        }
        fade = 1;
        if (draw != null) {
            for (int i = 0; i < slots.length; i++) {
                if (!seen[i]) slots[i] = Float.NaN;
            }
        }
        return x + end * k;
    }

    private int tone(int color) {
        return Colors.reAlpha(color, Math.round(Colors.alpha(color) * fade));
    }

    private float segment(@Nullable ImDrawList draw, Item item, float left, float top, float x, float k) {
        return switch (item) {
            case LOGO -> logo(draw, left, top, x, k);
            case FPS -> rolling(draw, left, top, rolling(draw, left, top, icon(draw, left, top, x, k, item, ICON), k, fps, FPS) + STAT_GAP * k, k, low, LOW);
            case USER -> label(draw, left, top, icon(draw, left, top, x, k, item, ICON), k, NAME, user.get());
            case SERVER -> label(draw, left, top, icon(draw, left, top, x, k, item, SMALL_ICON), k, TEXT, info.server());
            case PING -> rolling(draw, left, top, icon(draw, left, top, x, k, item, SMALL_ICON), k, ping, " ms");
            case TPS -> rolling(draw, left, top, icon(draw, left, top, x, k, item, SMALL_ICON), k, tps, " TPS");
            case COORDINATES -> label(draw, left, top, icon(draw, left, top, x, k, item, SMALL_ICON), k, TEXT, info.coordinates());
            case SPEED -> rolling(draw, left, top, icon(draw, left, top, x, k, item, SMALL_ICON), k, speed, " b/s");
            case VERSION -> label(draw, left, top, icon(draw, left, top, x, k, item, SMALL_ICON), k, TEXT, info.version());
            case TIME -> rolling(draw, left, top, icon(draw, left, top, x, k, item, SMALL_ICON), k, clock, "");
            case AVATAR -> {
                if (draw != null) Textures.face(draw, skin.get(), left + x + AVATAR / 2 * k, top + CENTER * k, AVATAR / 2 * k, tone(WHITE));
                yield x + AVATAR * k;
            }
        };
    }

    private float logo(@Nullable ImDrawList draw, float left, float top, float x, float k) {
        String title = module.title();
        float size = LOGO * Fonts.WORDMARK_EM * k;
        if (draw != null) {
            float y = top + (LOGO_BASELINE - LOGO * Fonts.WORDMARK_ASCENT) * k;
            int shade = tone(Colors.reAlpha(accent, LOGO_ALPHA));
            text(draw, Fonts.wordmark(), size, left + x, y + LOGO_DROP * k, shade, title);
            text(draw, Fonts.wordmark(), size, left + x, y, shade, title);
            text(draw, Fonts.wordmark(), size, left + x + LOGO_SHIFT * k, y, tone(ink), title);
        }
        return x + Fonts.wordmark().calcTextSizeAX(size, Float.MAX_VALUE, 0, title) + LOGO_SHIFT * k;
    }

    private float label(@Nullable ImDrawList draw, float left, float top, float x, float k, float em, String value) {
        float size = em * Fonts.HUD_EM * k;
        if (draw != null) text(draw, Fonts.hud(), size, left + x, top + (BASELINE - em * Fonts.HUD_ASCENT) * k, tone(ink), value);
        return x + Fonts.hud().calcTextSizeAX(size, Float.MAX_VALUE, 0, value);
    }

    private float icon(@Nullable ImDrawList draw, float left, float top, float x, float k, Item item, float box) {
        if (draw != null) {
            float y = top + (CENTER - box / 2 - (Fonts.HUD_ICON_ASCENT - 1) * box) * k;
            text(draw, Fonts.hudIcons(), box * Fonts.HUD_ICON_EM * k, left + x, y, tone(accent), item.icon());
        }
        return x + (box + ICON_GAP) * k;
    }

    private float rolling(@Nullable ImDrawList draw, float left, float top, float x, float k, Rolling value, String suffix) {
        float size = TEXT * Fonts.HUD_EM * k;
        float y = top + (BASELINE - TEXT * Fonts.HUD_ASCENT) * k;
        if (draw != null) value.draw(draw, Fonts.hud(), size, Math.round(left + x), Math.round(y), tone(ink));
        x += value.width(Fonts.hud(), size);
        if (draw != null) text(draw, Fonts.hud(), size, left + x, y, tone(ink), suffix);
        return x + Fonts.hud().calcTextSizeAX(size, Float.MAX_VALUE, 0, suffix);
    }

    private static float next(float x, float pad, float k) {
        return x == 0 ? pad * k : x + SEGMENT_GAP * k;
    }

    private static void text(ImDrawList draw, ImFont font, float size, float x, float y, int color, String text) {
        Controls.addText(draw, font, size, Math.round(x), Math.round(y), color, text);
    }
}
