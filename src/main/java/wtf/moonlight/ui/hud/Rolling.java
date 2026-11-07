package wtf.moonlight.ui.hud;

import imgui.ImDrawList;
import imgui.ImFont;
import wtf.moonlight.ui.clickgui.Colors;
import wtf.moonlight.ui.clickgui.Controls;

final class Rolling {
    private static final float SPEED = 4.5F;

    private String previous = "";
    private String current = "";
    private float progress = 1;

    void update(String next, float delta, boolean fresh) {
        progress = Math.min(1, progress + delta * SPEED);
        if (fresh || current.isEmpty()) {
            previous = current = next;
            progress = 1;
        } else if (progress >= 1 && !next.equals(current)) {
            previous = current;
            current = next;
            progress = 0;
        }
    }

    float width(ImFont font, float size) {
        float cell = cell(font, size);
        float width = 0;
        for (int i = 0; i < current.length(); i++) width += advance(font, size, current.charAt(i), cell);
        return width;
    }

    void draw(ImDrawList draw, ImFont font, float size, float x, float y, int color) {
        float cell = cell(font, size);
        float eased = 1 - (float) Math.pow(1 - progress, 3);
        boolean rolling = progress < 1 && previous.length() == current.length();
        draw.pushClipRect(x, y, x + width(font, size), y + size, true);
        for (int i = 0; i < current.length(); i++) {
            char next = current.charAt(i);
            char before = rolling ? previous.charAt(i) : next;
            float box = advance(font, size, next, cell);
            if (before == next) {
                glyph(draw, font, size, x, box, y, color, next);
            } else {
                glyph(draw, font, size, x, box, y - size * eased, fade(color, (1 - eased) * (1 - eased)), before);
                glyph(draw, font, size, x, box, y + size * (1 - eased), fade(color, eased * eased), next);
            }
            x += box;
        }
        draw.popClipRect();
    }

    private static int fade(int color, float alpha) {
        return Colors.reAlpha(color, Math.round(Colors.alpha(color) * alpha));
    }

    private static void glyph(ImDrawList draw, ImFont font, float size, float x, float box, float y, int color, char glyph) {
        String text = String.valueOf(glyph);
        Controls.addText(draw, font, size, Math.round(x + (box - font.calcTextSizeAX(size, Float.MAX_VALUE, 0, text)) / 2), Math.round(y), color, text);
    }

    private static float advance(ImFont font, float size, char glyph, float cell) {
        return Character.isDigit(glyph) ? cell : font.calcTextSizeAX(size, Float.MAX_VALUE, 0, String.valueOf(glyph));
    }

    private static float cell(ImFont font, float size) {
        float cell = 0;
        for (char digit = '0'; digit <= '9'; digit++) cell = Math.max(cell, font.calcTextSizeAX(size, Float.MAX_VALUE, 0, String.valueOf(digit)));
        return cell;
    }
}
