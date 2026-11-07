package wtf.moonlight.ui.clickgui;

import imgui.ImDrawList;
import imgui.ImFont;
import imgui.ImGui;
import imgui.ImGuiStorage;
import org.jspecify.annotations.Nullable;
import wtf.moonlight.ui.imgui.Natives;

import static wtf.moonlight.ui.clickgui.Widgets.scale;

public final class Controls {
    public static final float FIELD_HOVER_FILL = .45f;
    public static final float FIELD_HOVER_BORDER = .22f;
    public static final int FIELD_SELECTION_ALPHA = 48;
    static final int SHADOW = Colors.rgb(0, 0, 0);

    private static final float TEXT_CAPTION = 10;
    private static final float TEXT_CONTROL = 14;

    private static @Nullable Theme palette;

    private Controls() {
    }

    static void setTheme(@Nullable Theme theme) {
        palette = theme;
    }

    public static float motion(int id, float target, float speed, float initial) {
        ImGuiStorage storage = ImGui.getStateStorage();
        float value = storage.getFloat(id, initial < 0 ? target : initial);
        value = value + (target - value) * (1.0F - (float) Math.exp(-speed * ImGui.getIO().getDeltaTime()));
        if (Math.abs(value - target) < 0.0005f) value = target;
        storage.setFloat(id, value);
        return value;
    }

    public static void addText(ImDrawList draw, ImFont font, float size, float x, float y, int color, String text) {
        addText(draw, font, size, x, y, color, text, 0);
    }

    public static void addText(ImDrawList draw, ImFont font, float size, float x, float y, int color, String text, float wrapWidth) {
        if (Colors.alpha(color) == 0 || text.isEmpty()) return;
        font.renderText(draw, size, x, y, color, draw.getClipRectMinX(), draw.getClipRectMinY(), draw.getClipRectMaxX(), draw.getClipRectMaxY(),
                text, null, wrapWidth, 0);
    }

    static void glass(ImDrawList draw, float minX, float minY, float maxX, float maxY, int tint, float rounding) {
        if (maxX <= minX || maxY <= minY) return;
        float tintAlpha = Colors.alpha(tint) / 255.0F;
        draw.addRectFilled(minX, minY, maxX, maxY, Colors.reAlpha(tint, (int) (tintAlpha * 255.0F + 0.5F)), rounding);
    }

    public static boolean toggle(ImDrawList draw, int id, float x, float y, boolean value) {
        ImGui.pushID(id);
        if (hit("##toggle", x - scale(5), y - scale(6), scale(39), scale(30))) value = !value;
        int item = ImGui.getItemID();
        float on = motion(item ^ 0x55aa721, value ? 1 : 0, 19, -1);
        float hover = motion(item ^ 0x118a0, ImGui.isItemHovered() ? 1 : 0, 20, -1);
        ImGui.popID();
        draw.addRectFilled(x - scale(1 + hover), y - scale(1 + hover), x + scale(30 + hover), y + scale(19 + hover),
                c(75, 126, 255, (int) (45 * hover)), scale(10));
        draw.addRectFilled(x, y, x + scale(29), y + scale(18), Colors.mix(c(29, 33, 43), c(75, 126, 255), on), scale(9));
        draw.addCircleFilled(x + scale(9 + (20 - 9) * on), y + scale(9), scale(7), Colors.mix(c(133, 144, 156), c(248, 249, 252), on));
        return value;
    }

    public static boolean select(ImDrawList draw, int id, float x, float y, float width, String value) {
        ImGui.pushID(id);
        boolean click = hit("##select", x, y, width, scale(23));
        float response = motion(ImGui.getItemID() ^ 0x6161, ImGui.isItemHovered() ? 1 : 0, 16, -1);
        ImGui.popID();
        draw.addRectFilled(x, y, x + width, y + scale(23), Colors.mix(c(25, 28, 38), c(31, 38, 54), response * FIELD_HOVER_FILL), scale(5));
        draw.addRect(x, y, x + width, y + scale(23), Colors.mix(c(32, 35, 46), c(75, 126, 255), response * FIELD_HOVER_BORDER), scale(5));
        draw.pushClipRect(x, y, x + width - scale(20), y + scale(23), true);
        textY(draw, x + scale(7), y, 23, c(170, 173, 184), value, TEXT_CONTROL);
        draw.popClipRect();
        draw.addLine(x + width - scale(13), y + scale(9), x + width - scale(9), y + scale(13), c(139, 143, 154), scale(1));
        draw.addLine(x + width - scale(9), y + scale(13), x + width - scale(5), y + scale(9), c(139, 143, 154), scale(1));
        return click;
    }

    public static float slider(ImDrawList draw, int id, float x, float y, float amount, float initial, float width) {
        float track = scale(width);
        ImGui.pushID(id);
        hit("##slider", x - scale(5), y - scale(8), track + scale(10), scale(19));
        if (ImGui.isItemActive()) amount = Math.clamp((ImGui.getIO().getMousePosX() - x) / track, 0.0F, 1.0F);
        float shown = motion(ImGui.getItemID() ^ 0xa8f1, amount, 14, initial);
        ImGui.popID();
        draw.addRectFilled(x, y, x + track, y + scale(3), c(34, 38, 48), scale(2));
        draw.addRectFilled(x, y, x + track * shown, y + scale(3), c(75, 126, 255), scale(2));
        draw.addCircleFilled(x + track * shown, y + scale(1.5f), scale(5.5f), c(247, 248, 252));
        return amount;
    }

    static void pageReveal(float progress) {
        if (progress >= .9999f) return;
        float offset = scale(9) * (1 - progress);
        for (long list : Windows.subtree()) Natives.transform(list, 0, 0, 1, offset, 0, progress);
    }

    static void chevron(ImDrawList draw, float x, float y, int color) {
        draw.addLine(x, y, x + scale(3), y + scale(3), color, scale(1.3f));
        draw.addLine(x + scale(3), y + scale(3), x, y + scale(6), color, scale(1.3f));
    }

    static void check(ImDrawList draw, float x, float y, int color) {
        draw.addLine(x, y, x + scale(3), y + scale(3), color, scale(1.5f));
        draw.addLine(x + scale(3), y + scale(3), x + scale(8), y - scale(3), color, scale(1.5f));
    }

    static void card(ImDrawList draw, float x, float y, float width, float height, String title) {
        text(draw, x + scale(12), y - scale(18), c(89, 94, 106), title, TEXT_CAPTION);
        draw.addRectFilled(x, y, x + width, y + height, c(17, 19, 27, 224), scale(14));
        draw.addRect(x, y, x + width, y + height, c(31, 34, 44), scale(14));
    }

    static void popupBackdrop(ImDrawList draw, float x, float y, float width, float height) {
        glass(draw, x, y, x + width, y + height, c(20, 20, 29, 235), scale(16));
        draw.addRect(x, y, x + width, y + height, c(57, 61, 76, 205), scale(16));
        draw.addLine(x + scale(16), y + scale(1), x + scale(width / scale(1) - 16), y + scale(1), Colors.rgba(255, 255, 255, 22));
    }

    static float popupEase(float open) {
        return 1.0F - (float) Math.pow(1.0F - open, 3.0F);
    }

    private static boolean hit(String id, float x, float y, float width, float height) {
        ImGui.setCursorScreenPos(x, y);
        return ImGui.invisibleButton(id, width, height);
    }

    private static void text(ImDrawList draw, float x, float y, int color, String value, float size) {
        addText(draw, ImGui.getFont(), scale(size), x, y, color, value);
    }

    private static void textY(ImDrawList draw, float x, float y, float height, int color, String value, float size) {
        ImFont font = ImGui.getFont();
        float textHeight = font.calcTextSizeAY(scale(size), Float.MAX_VALUE, 0, value);
        addText(draw, font, scale(size), x, y + (float) Math.floor((scale(height) - textHeight) * 0.5f), color, value);
    }

    private static int c(int r, int g, int b) {
        return c(r, g, b, 255);
    }

    private static int c(int r, int g, int b, int a) {
        int raw = Colors.rgba(r, g, b, a);
        Theme theme = palette;
        if (theme == null || theme.isDark()) return raw;
        if (r == 17 && g == 19 && b == 27) return theme.modulePanelBg;
        if (r == 31 && g == 34 && b == 44) return theme.windowBorder;
        if (r == 29 && g == 33 && b == 43) return theme.switchDisabledBg;
        if (r == 89 && g == 94 && b == 106) return theme.secondary;
        if (r == 25 && g == 28 && b == 38) return theme.selectorBg;
        if (r == 31 && g == 38 && b == 54) return theme.buttonHoveredBg;
        if (r == 32 && g == 35 && b == 46) return theme.split;
        if (r == 170 && g == 173 && b == 184) return theme.main;
        if (r == 34 && g == 38 && b == 48) return theme.sliderBg;
        if (r == 20 && g == 20 && b == 29) return Colors.rgba(248, 249, 252, 245);
        if (r == 57 && g == 61 && b == 76) return Colors.rgba(180, 187, 202, 205);
        return raw;
    }
}
