package wtf.moonlight.ui.clickgui;

import imgui.ImDrawList;
import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiStyleVar;
import imgui.flag.ImGuiWindowFlags;
import wtf.moonlight.ui.imgui.Effects;
import wtf.moonlight.ui.imgui.Fonts;

public final class Widgets {
    public static final float TEXT_BODY = 15;
    public static final float TEXT_CONTROL = 14;
    public static final float TEXT_SMALL = 12;

    private static float scale = 1;

    private Widgets() {
    }

    static void setScale(float value) {
        scale = value;
    }

    public static float scale(float pixels) {
        return pixels * scale;
    }

    public static void pushText(float size) {
        ImGui.pushFont(Fonts.regular(), size);
    }

    public static void pushIcon() {
        ImGui.pushFont(Fonts.icons(), 13);
    }

    public static void popFont() {
        ImGui.popFont();
    }

    public static float motion(int id, float target, float speed) {
        return Controls.motion(id, target, speed, -1);
    }

    public static float textWidth(String text) {
        return ImGui.calcTextSizeX(text);
    }

    public static float textHeight() {
        return ImGui.getTextLineHeight();
    }

    public static boolean choice(Theme theme, String id, String label, boolean selected, float width) {
        ImGui.pushID(id);
        float x = ImGui.getCursorScreenPosX();
        float y = ImGui.getCursorScreenPosY();
        boolean clicked = ImGui.invisibleButton("choice", width, scale(32));
        float hover = motion(ImGui.getItemID() ^ 0x9921, ImGui.isItemHovered() ? 1 : 0, 22);
        ImDrawList draw = ImGui.getWindowDrawList();
        draw.addRectFilled(x, y, x + width, y + scale(32),
                Colors.reAlpha(theme.selectorElementHover, (int) (Colors.alpha(theme.selectorElementHover) * hover)), scale(10));
        if (selected) Controls.check(draw, x + scale(14), y + scale(16), theme.textHighlight);
        pushText(TEXT_CONTROL);
        draw.pushClipRect(x, y, x + width, y + scale(32), true);
        draw.addText(x + scale(35), y + (scale(32) - textHeight()) * .5f, selected ? theme.textHighlight : theme.main, label);
        draw.popClipRect();
        popFont();
        ImGui.popID();
        return clicked;
    }

    public static boolean beginPopup(Theme theme, String id) {
        return beginPopup(theme, id, 0, Popups.Motion.DROPDOWN, 4, 4);
    }

    static boolean beginPopup(Theme theme, String id, int flags, Popups.Motion motion, float paddingX, float paddingY) {
        ImGui.pushStyleVar(ImGuiStyleVar.PopupRounding, scale(16));
        ImGui.pushStyleVar(ImGuiStyleVar.PopupBorderSize, scale(1));
        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, scale(paddingX), scale(paddingY));
        ImGui.pushStyleVar(ImGuiStyleVar.ItemSpacing, 0, 0);
        ImGui.pushStyleColor(ImGuiCol.PopupBg, theme.isDark() ? Colors.rgba(20, 20, 29, 235) : Colors.rgba(248, 249, 252, 245));
        ImGui.pushStyleColor(ImGuiCol.Border, theme.isDark() ? Colors.rgba(57, 61, 76, 205) : Colors.rgba(180, 187, 202, 205));
        ImGui.pushStyleColor(ImGuiCol.Text, theme.main);
        boolean result = Windows.beginPopup(id, flags | ImGuiWindowFlags.NoBackground, motion);
        ImGui.popStyleColor(3);
        ImGui.popStyleVar(4);
        if (result) {
            ImDrawList draw = ImGui.getWindowDrawList();
            float x = ImGui.getWindowPosX();
            float y = ImGui.getWindowPosY();
            float width = ImGui.getWindowSizeX();
            float height = ImGui.getWindowSizeY();
            draw.pushClipRectFullScreen();
            Effects.shadow(draw, x, y, x + width, y + height, scale(16), 0, Controls.SHADOW);
            Effects.blur(draw, x, y, x + width, y + height, scale(16), 0);
            Controls.popupBackdrop(draw, x, y, width, height);
            draw.popClipRect();
            if (ImGui.isWindowAppearing()) ImGui.getStateStorage().setFloat(ImGui.getID("##reveal"), 0);
        }
        return result;
    }

    public static void endPopup() {
        Popups.Motion motion = Windows.popupMotion();
        float open = Controls.motion(ImGui.getID("##reveal"), 1, motion == Popups.Motion.ACCOUNT ? 17 : 20, 0);
        float x = ImGui.getWindowPosX();
        float y = ImGui.getWindowPosY();
        float width = ImGui.getWindowSizeX();
        float height = ImGui.getWindowSizeY();
        Popups.capture(Windows.popupKey(), x, y, width, height, open, motion, Windows.subtree());
        for (long list : Windows.subtree()) Popups.transform(list, x, y, width, height, open, motion);
        Windows.endPopup();
    }

    static void label(ImDrawList draw, float x, float y, int color, String text, float size) {
        pushText(size);
        draw.addText(x, y, color, text);
        popFont();
    }
}
