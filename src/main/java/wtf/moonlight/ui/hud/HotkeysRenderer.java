package wtf.moonlight.ui.hud;

import imgui.ImDrawList;
import imgui.ImGui;
import imgui.ImGuiIO;
import org.jspecify.annotations.Nullable;
import wtf.moonlight.bind.Hotkey;
import wtf.moonlight.module.Module;
import wtf.moonlight.module.ModuleManager;
import wtf.moonlight.module.impl.render.Hotkeys;
import wtf.moonlight.ui.clickgui.Colors;
import wtf.moonlight.ui.clickgui.Controls;
import wtf.moonlight.ui.imgui.Effects;
import wtf.moonlight.ui.imgui.Fonts;
import wtf.moonlight.ui.imgui.ImGuiManager;
import wtf.moonlight.utils.KeyUtils;

import java.util.ArrayList;
import java.util.List;

public final class HotkeysRenderer implements HudElement {
    private static final float FOLLOW = 12;
    private static final float FADE = 14;
    private static final float ANIMATION = 0.3F;
    private static final float HEADER = 22;
    private static final float HEADER_ROUNDING = 10;
    private static final float ICON_X = 7;
    private static final float ICON_BOX = 12;
    private static final float TITLE_X = 29;
    private static final float TITLE_PAD = 9;
    private static final float TITLE = 10.5F;
    private static final float PILL = 18;
    private static final float PILL_ROUNDING = 4;
    private static final float PILL_PAD = 4.5F;
    private static final float TEXT = 10;
    private static final float GAP = 6;
    private static final String TITLE_TEXT = "Hotkeys";
    private static final String ICON = Character.toString(0xE660);
    private static final int WHITE = Colors.rgb(255, 255, 255);
    private static final int INK = Colors.rgb(28, 30, 38);
    private static final int HEADER_FILL = Colors.rgba(0, 0, 0, 179);
    private static final int LIGHT_HEADER_FILL = Colors.rgba(255, 255, 255, 179);
    private static final int PILL_FILL = Colors.rgba(40, 32, 43, 153);
    private static final int LIGHT_PILL_FILL = Colors.rgba(255, 255, 255, 153);
    private static final int SHADOW = Colors.rgba(0, 0, 0, 179);
    private static final int LIGHT_SHADOW = Colors.rgba(0, 0, 0, 77);

    private final Hotkeys module;
    private final ModuleManager modules;
    private final List<Entry> entries = new ArrayList<>();
    private float progress;
    private float column;
    private float width;
    private float height;
    private float originX;
    private float originY;
    private float unit = 1;
    private boolean snap;
    private @Nullable Rect bounds;

    public HotkeysRenderer(Hotkeys module, ModuleManager modules) {
        this.module = module;
        this.modules = modules;
    }

    @Override
    public Hotkeys module() {
        return module;
    }

    @Override
    public @Nullable Rect bounds() {
        return bounds;
    }

    @Override
    public void place(float x, float y) {
        ImGuiIO io = ImGui.getIO();
        module.anchor().drop(io.getDisplaySizeX(), io.getDisplaySizeY(), x, y, width, height, unit);
        snap = true;
    }

    @Override
    public void frame(Style style) {
        ImGuiIO io = ImGui.getIO();
        bounds = null;
        float delta = Math.min(io.getDeltaTime(), 0.1F);
        boolean fresh = progress <= 0;
        progress = Math.clamp(progress + (module.isEnabled() ? delta : -delta) / ANIMATION, 0.0F, 1.0F);
        if (progress <= 0) {
            entries.clear();
            return;
        }
        float k = module.scale() * style.scale() * ImGuiManager.displayScale();
        float slide = fresh || unit != k ? 1 : 1 - (float) Math.exp(-FOLLOW * delta);
        update(fresh ? 1 : 1 - (float) Math.exp(-FADE * delta));
        float header = TITLE_X + measure(TITLE, TITLE_TEXT, k) + TITLE_PAD;
        float left = 0;
        float right = 0;
        float rows = 0;
        for (Entry entry : entries) {
            left = Math.max(left, measure(TEXT, entry.key, k) + PILL_PAD * 2);
            right = Math.max(right, measure(TEXT, entry.name, k) + PILL_PAD * 2);
            rows += (GAP + PILL) * entry.shown;
        }
        column += (left - column) * slide;
        width = Math.max(header, entries.isEmpty() ? 0 : column + GAP + right) * k;
        height = (HEADER + rows) * k;
        float targetX = module.anchor().left(io.getDisplaySizeX(), width, k);
        float targetY = module.anchor().top(io.getDisplaySizeY(), height, k);
        originX = snap ? targetX : originX + (targetX - originX) * slide;
        originY = snap ? targetY : originY + (targetY - originY) * slide;
        unit = k;
        snap = false;

        float x = Math.round(originX);
        float y = Math.round(originY);
        bounds = new Rect(x, y, x + width, y + height, PILL_ROUNDING * k);
        boolean light = style.light();
        int ink = light ? INK : WHITE;
        float alpha = 1 - (float) Math.pow(1 - progress, 3);
        ImDrawList draw = ImGui.getBackgroundDrawList();
        float headerWidth = header * k;
        float headerHeight = HEADER * k;
        int shadow = light ? LIGHT_SHADOW : SHADOW;
        for (Pass pass : Pass.values()) {
            box(draw, pass, x, y, x + headerWidth, y + headerHeight, HEADER_ROUNDING * k, alpha, shadow,
                    fade(light ? LIGHT_HEADER_FILL : HEADER_FILL, alpha * style.alpha()));
            if (pass == Pass.FILL) {
                Controls.addText(draw, Fonts.hudIcons(), ICON_BOX * Fonts.HUD_ICON_EM * k, Math.round(x + ICON_X * k),
                        Math.round(y + (HEADER / 2 - ICON_BOX / 2 - (Fonts.HUD_ICON_ASCENT - 1) * ICON_BOX) * k), fade(style.accent(), alpha), ICON);
                text(draw, TITLE, x + TITLE_X * k, y + centered(HEADER, TITLE) * k, fade(ink, alpha), TITLE_TEXT, k);
            }
            float offset = HEADER + GAP;
            for (Entry entry : entries) {
                float top = y + offset * k;
                float shown = alpha * entry.shown;
                pill(draw, pass, x, top, entry.key, shown, style, ink, k);
                pill(draw, pass, x + (column + GAP) * k, top, entry.name, shown, style, ink, k);
                offset += (PILL + GAP) * entry.shown;
            }
        }
    }

    private void update(float fade) {
        for (Entry entry : entries) entry.active = false;
        for (Module owner : modules.getModules()) {
            for (Hotkey hotkey : owner.getBind().getHotkeys()) {
                boolean on = hotkey.isHold() ? hotkey.isEngaged() : owner.isEnabled();
                if (!hotkey.isVisible() || hotkey.getKey() == KeyUtils.NONE || !on) continue;
                Entry entry = entries.stream().filter(candidate -> candidate.hotkey == hotkey).findFirst().orElse(null);
                if (entry == null) {
                    entry = new Entry(hotkey);
                    entries.add(entry);
                }
                entry.active = true;
                entry.name = owner.getName();
                entry.key = KeyUtils.getName(hotkey.getKey());
            }
        }
        for (Entry entry : entries) entry.shown += ((entry.active ? 1 : 0) - entry.shown) * fade;
        entries.removeIf(entry -> !entry.active && entry.shown < 0.01F);
    }

    private static void pill(ImDrawList draw, Pass pass, float x, float top, String label, float alpha, Style style, int ink, float k) {
        float right = x + (measure(TEXT, label, k) + PILL_PAD * 2) * k;
        box(draw, pass, x, top, right, top + PILL * k, PILL_ROUNDING * k, alpha, style.light() ? LIGHT_SHADOW : SHADOW,
                fade(style.light() ? LIGHT_PILL_FILL : PILL_FILL, alpha * style.alpha()));
        if (pass == Pass.FILL) text(draw, TEXT, x + PILL_PAD * k, top + centered(PILL, TEXT) * k, fade(ink, alpha), label, k);
    }

    private static void box(ImDrawList draw, Pass pass, float minX, float minY, float maxX, float maxY, float rounding, float alpha, int shadow, int fill) {
        switch (pass) {
            case BLUR -> Effects.blur(draw, minX, minY, maxX, maxY, rounding, 0, alpha);
            case SHADOW -> Effects.shadow(draw, minX, minY, maxX, maxY, rounding, 0, fade(shadow, alpha));
            case FILL -> draw.addRectFilled(minX, minY, maxX, maxY, fill, rounding);
        }
    }

    private static float centered(float box, float em) {
        return box / 2 + Fonts.HUD_CAP * em / 2 - Fonts.HUD_ASCENT * em;
    }

    private static float measure(float em, String text, float k) {
        return Fonts.hudRegular().calcTextSizeAX(em * Fonts.HUD_EM * k, Float.MAX_VALUE, 0, text) / k;
    }

    private static void text(ImDrawList draw, float em, float x, float y, int color, String text, float k) {
        Controls.addText(draw, Fonts.hudRegular(), em * Fonts.HUD_EM * k, Math.round(x), Math.round(y), color, text);
    }

    private static int fade(int color, float alpha) {
        return Colors.reAlpha(color, Math.round(Colors.alpha(color) * alpha));
    }

    private enum Pass {
        BLUR,
        SHADOW,
        FILL
    }

    private static final class Entry {
        private final Hotkey hotkey;
        private String name = "";
        private String key = "";
        private float shown;
        private boolean active;

        private Entry(Hotkey hotkey) {
            this.hotkey = hotkey;
        }
    }
}
