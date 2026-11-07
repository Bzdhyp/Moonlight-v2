package wtf.moonlight.ui.clickgui.element;

import imgui.ImDrawList;
import imgui.ImGui;
import imgui.flag.ImDrawFlags;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiStyleVar;
import imgui.type.ImString;
import org.jspecify.annotations.Nullable;
import wtf.moonlight.ui.clickgui.Colors;
import wtf.moonlight.ui.clickgui.Theme;
import wtf.moonlight.ui.clickgui.Widgets;
import wtf.moonlight.ui.imgui.Natives;
import wtf.moonlight.value.impl.ColorValue;

import java.util.Locale;

import static wtf.moonlight.ui.clickgui.Widgets.scale;

public final class Palette {
    public static final float WIDTH = 212;
    public static final float HEIGHT = 262;

    private static final float PAD = 12;
    private static final float SQUARE = 160;
    private static final float BAR = 10;
    private static final float ROW = 22;
    private static final float MENU_ROW = 32;
    private static final int WHITE = Colors.rgb(255, 255, 255);
    private static final String[] FORMATS = {"HEX", "RGB", "HSV"};
    private static final int[] FORMAT_ICONS = {0xF1FC, 0xF53F, 0xF5C3};
    private static final ImString BUFFER = new ImString(40);

    private static int format;
    private static float hue;
    private static int known;
    private static boolean editing;

    private Palette() {
    }

    public static void draw(Theme theme, ColorValue value) {
        int color = value.get();
        float[] hsv = hsv(color);
        if (color != known) {
            if (hsv[1] > 0 && hsv[2] > 0) hue = hsv[0];
            known = color;
        }
        int alpha = color >>> 24;
        ImDrawList draw = ImGui.getWindowDrawList();
        long list = draw.ptr;
        float x = ImGui.getWindowPosX() + scale(PAD);
        float y = ImGui.getWindowPosY() + scale(PAD);
        float width = scale(WIDTH - PAD * 2);
        float mouseX = ImGui.getMousePosX();
        float mouseY = ImGui.getMousePosY();

        float height = scale(SQUARE);
        int start = Natives.vertexCount(list);
        draw.addRectFilled(x, y, x + width, y + height, WHITE, scale(6));
        Natives.gradient(list, start, x, y, x + width, y, WHITE, rgb(hue, 1, 1));
        start = Natives.vertexCount(list);
        draw.addRectFilled(x, y, x + width, y + height, WHITE, scale(6));
        Natives.gradient(list, start, x, y, x, y + height, 0, Colors.rgb(0, 0, 0));
        ImGui.setCursorScreenPos(x, y);
        ImGui.invisibleButton("##sv", width, height);
        if (ImGui.isItemActive()) {
            hsv[1] = Math.clamp((mouseX - x) / width, 0, 1);
            hsv[2] = 1 - Math.clamp((mouseY - y) / height, 0, 1);
            color = Colors.reAlpha(rgb(hue, hsv[1], hsv[2]), alpha);
        }
        knob(draw, theme, x + hsv[1] * width, y + (1 - hsv[2]) * height, scale(6), rgb(hue, hsv[1], hsv[2]), state());

        float bar = scale(BAR);
        float hueY = y + height + scale(12);
        for (int i = 0; i < 6; i++) {
            float from = x + width * i / 6;
            float to = x + width * (i + 1) / 6 + (i == 0 ? scale(1) : 0);
            int corners = i == 0 ? ImDrawFlags.RoundCornersLeft : i == 5 ? ImDrawFlags.RoundCornersRight : ImDrawFlags.RoundCornersNone;
            start = Natives.vertexCount(list);
            draw.addRectFilled(from, hueY, to, hueY + bar, WHITE, bar / 2, corners);
            Natives.gradient(list, start, from, hueY, x + width * (i + 1) / 6, hueY, rgb(i / 6.0F, 1, 1), rgb((i + 1) / 6.0F, 1, 1));
        }
        ImGui.setCursorScreenPos(x, hueY - scale(4));
        ImGui.invisibleButton("##hue", width, bar + scale(8));
        if (ImGui.isItemActive()) {
            hue = Math.clamp((mouseX - x) / width, 0, 0.9999F);
            color = Colors.reAlpha(rgb(hue, hsv[1], hsv[2]), alpha);
        }
        knob(draw, theme, x + hue * width, hueY + bar / 2, scale(7), rgb(hue, 1, 1), state());

        float alphaY = hueY + bar + scale(10);
        draw.addRectFilled(x, alphaY, x + width, alphaY + bar, WHITE, bar / 2);
        float cell = bar / 2;
        for (int column = 1; column < (int) (width / cell) - 1; column++) {
            for (int row = 0; row < 2; row++) {
                if ((column + row) % 2 == 1) {
                    draw.addRectFilled(x + column * cell, alphaY + row * cell, x + (column + 1) * cell, alphaY + (row + 1) * cell, Colors.rgb(204, 204, 204));
                }
            }
        }
        start = Natives.vertexCount(list);
        draw.addRectFilled(x, alphaY, x + width, alphaY + bar, WHITE, bar / 2);
        Natives.gradient(list, start, x, alphaY, x + width, alphaY, Colors.reAlpha(color, 0), Colors.reAlpha(color, 255));
        ImGui.setCursorScreenPos(x, alphaY - scale(4));
        ImGui.invisibleButton("##alpha", width, bar + scale(8));
        if (ImGui.isItemActive()) {
            alpha = Math.round(Math.clamp((mouseX - x) / width, 0, 1) * 255);
            color = Colors.reAlpha(color, alpha);
        }
        knob(draw, theme, x + alpha / 255.0F * width, alphaY + bar / 2, scale(7), Colors.reAlpha(color, alpha), state());

        color = bottom(theme, draw, x, alphaY + bar + scale(14), width, color, value);
        if (color != value.get()) {
            value.set(color);
            known = color;
        }
    }

    private static int bottom(Theme theme, ImDrawList draw, float x, float y, float width, int color, ColorValue value) {
        float middle = y + scale(ROW) / 2;
        ImGui.setCursorScreenPos(x, y);
        if (ImGui.invisibleButton("##format", scale(50), scale(ROW))) ImGui.openPopup("##formats");
        draw.addLine(x + scale(2), middle - scale(2), x + scale(5), middle + scale(1), theme.main, scale(1.3F));
        draw.addLine(x + scale(5), middle + scale(1), x + scale(8), middle - scale(2), theme.main, scale(1.3F));
        Widgets.pushText(Widgets.TEXT_CONTROL);
        draw.addText(x + scale(14), middle - Widgets.textHeight() / 2, theme.main, FORMATS[format]);
        Widgets.popFont();
        draw.addRectFilled(x + scale(56), middle - scale(7), x + scale(70), middle + scale(7), Colors.reAlpha(color, 255), scale(3));

        String text = text(color);
        if (!editing) BUFFER.set(text);
        Widgets.pushText(Widgets.TEXT_SMALL);
        ImGui.pushStyleVar(ImGuiStyleVar.FramePadding, scale(4), (scale(ROW) - Widgets.textHeight()) / 2);
        ImGui.pushStyleColor(ImGuiCol.FrameBg, 0);
        ImGui.pushStyleColor(ImGuiCol.Text, theme.main);
        ImGui.setCursorScreenPos(x + scale(76), y);
        ImGui.setNextItemWidth(width - scale(76 + 44));
        ImGui.inputText("##value", BUFFER);
        editing = ImGui.isItemActive();
        if (ImGui.isItemDeactivatedAfterEdit()) {
            Integer parsed = parse(BUFFER.get(), color >>> 24);
            if (parsed != null) color = parsed;
        }
        ImGui.popStyleColor(2);
        ImGui.popStyleVar();
        Widgets.popFont();

        ImGui.setCursorScreenPos(x + width - scale(40), y);
        if (ImGui.invisibleButton("##reset", scale(20), scale(ROW)) && !value.isInitial()) {
            value.reset();
            color = value.get();
        }
        int reset = value.isInitial() ? Colors.reAlpha(theme.secondary, 90) : ImGui.isItemHovered() ? theme.textHighlight : theme.secondary;
        if (!value.isInitial() && ImGui.isItemHovered()) ImGui.setTooltip("Reset");
        ImGui.setCursorScreenPos(x + width - scale(20), y);
        if (ImGui.invisibleButton("##copy", scale(20), scale(ROW))) ImGui.setClipboardText(text);
        int copy = ImGui.isItemHovered() ? theme.textHighlight : theme.secondary;
        Widgets.pushIcon();
        draw.addText(x + width - scale(36), middle - Widgets.textHeight() / 2, reset, Character.toString(0xF0E2));
        draw.addText(x + width - scale(16), middle - Widgets.textHeight() / 2, copy, Character.toString(0xF0C5));
        Widgets.popFont();

        formats(theme);
        return color;
    }

    private static void formats(Theme theme) {
        ImGui.setNextWindowSize(scale(130), scale(8 + MENU_ROW * FORMATS.length));
        if (!Widgets.beginPopup(theme, "##formats")) return;
        ImDrawList draw = ImGui.getWindowDrawList();
        float x = ImGui.getWindowPosX() + scale(4);
        for (int i = 0; i < FORMATS.length; i++) {
            float y = ImGui.getWindowPosY() + scale(4 + MENU_ROW * i);
            float middle = y + scale(MENU_ROW) / 2;
            ImGui.setCursorScreenPos(x, y);
            ImGui.pushID(i);
            boolean clicked = ImGui.invisibleButton("format", scale(122), scale(MENU_ROW));
            float hover = Widgets.motion(ImGui.getItemID() ^ 0x6c6c, ImGui.isItemHovered() ? 1 : 0, 22);
            ImGui.popID();
            draw.addRectFilled(x, y, x + scale(122), y + scale(MENU_ROW),
                    Colors.reAlpha(theme.selectorElementHover, (int) (Colors.alpha(theme.selectorElementHover) * hover)), scale(12));
            Widgets.pushIcon();
            draw.addText(x + scale(12), middle - Widgets.textHeight() / 2, theme.main, Character.toString(FORMAT_ICONS[i]));
            Widgets.popFont();
            Widgets.pushText(Widgets.TEXT_CONTROL);
            draw.addText(x + scale(34), middle - Widgets.textHeight() / 2, i == format ? theme.textHighlight : theme.main, FORMATS[i]);
            Widgets.popFont();
            if (i == format) {
                draw.addLine(x + scale(100), middle, x + scale(103), middle + scale(3), theme.iconHighlight, scale(1.5F));
                draw.addLine(x + scale(103), middle + scale(3), x + scale(108), middle - scale(3), theme.iconHighlight, scale(1.5F));
            }
            if (clicked) {
                format = i;
                ImGui.closeCurrentPopup();
            }
        }
        Widgets.endPopup();
    }

    private static String text(int color) {
        int alpha = color >>> 24;
        String suffix = alpha < 255 ? ", " + alpha : "";
        return switch (format) {
            case 1 -> (color & 0xFF) + ", " + (color >>> 8 & 0xFF) + ", " + (color >>> 16 & 0xFF) + suffix;
            case 2 -> {
                float[] hsv = hsv(color);
                yield String.format(Locale.ROOT, "%d, %d%%, %d%%", Math.round(shownHue(hsv) * 360), Math.round(hsv[1] * 100), Math.round(hsv[2] * 100)) + suffix;
            }
            default -> ColorValue.hex(color);
        };
    }

    private static float shownHue(float[] hsv) {
        return hsv[1] > 0 && hsv[2] > 0 ? hsv[0] : hue;
    }

    private static @Nullable Integer parse(String text, int alpha) {
        if (format == 0) return ColorValue.parse(text);
        String[] parts = text.replace("%", "").split("[,\\s]+");
        if (parts.length < 3 || parts.length > 4) return null;
        float[] numbers = new float[parts.length];
        try {
            for (int i = 0; i < parts.length; i++) numbers[i] = Float.parseFloat(parts[i].strip());
        } catch (NumberFormatException exception) {
            return null;
        }
        int a = parts.length == 4 ? Math.clamp(Math.round(numbers[3]), 0, 255) : alpha;
        if (format == 1) {
            return Colors.rgba(Math.clamp(Math.round(numbers[0]), 0, 255), Math.clamp(Math.round(numbers[1]), 0, 255),
                    Math.clamp(Math.round(numbers[2]), 0, 255), a);
        }
        hue = Math.clamp(numbers[0] / 360.0F, 0, 0.9999F);
        return Colors.reAlpha(rgb(hue, Math.clamp(numbers[1] / 100.0F, 0, 1), Math.clamp(numbers[2] / 100.0F, 0, 1)), a);
    }

    private static float state() {
        return Widgets.motion(ImGui.getItemID() ^ 0x65f0, ImGui.isItemActive() ? 1 : ImGui.isItemHovered() ? .7f : 0, 18);
    }

    private static void knob(ImDrawList draw, Theme theme, float x, float y, float radius, int fill, float hover) {
        draw.addCircleFilled(x, y, radius + scale(3 + 2 * hover), Colors.reAlpha(theme.iconHighlight, (int) (30 + 40 * hover)), 24);
        float size = radius + scale(.6f * hover);
        draw.addCircleFilled(x, y, size, fill);
        draw.addCircle(x, y, size, WHITE, 0, scale(2));
    }

    private static float[] hsv(int color) {
        float r = (color & 0xFF) / 255.0F;
        float g = (color >>> 8 & 0xFF) / 255.0F;
        float b = (color >>> 16 & 0xFF) / 255.0F;
        float max = Math.max(r, Math.max(g, b));
        float delta = max - Math.min(r, Math.min(g, b));
        float h = 0;
        if (delta > 0) {
            if (max == r) h = (g - b) / delta;
            else if (max == g) h = 2 + (b - r) / delta;
            else h = 4 + (r - g) / delta;
            h = (h / 6 + 1) % 1;
        }
        return new float[]{h, max > 0 ? delta / max : 0, max};
    }

    private static int rgb(float h, float s, float v) {
        float sector = h % 1 * 6;
        int index = (int) sector;
        float f = sector - index;
        float p = v * (1 - s);
        float q = v * (1 - s * f);
        float t = v * (1 - s * (1 - f));
        float[] rgb = switch (index) {
            case 0 -> new float[]{v, t, p};
            case 1 -> new float[]{q, v, p};
            case 2 -> new float[]{p, v, t};
            case 3 -> new float[]{p, q, v};
            case 4 -> new float[]{t, p, v};
            default -> new float[]{v, p, q};
        };
        return Colors.rgb(Math.round(rgb[0] * 255), Math.round(rgb[1] * 255), Math.round(rgb[2] * 255));
    }
}
