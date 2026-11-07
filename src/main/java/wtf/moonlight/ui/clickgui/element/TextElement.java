package wtf.moonlight.ui.clickgui.element;

import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiStyleVar;
import imgui.type.ImString;
import wtf.moonlight.ui.clickgui.Colors;
import wtf.moonlight.ui.clickgui.Controls;
import wtf.moonlight.ui.clickgui.Panel;
import wtf.moonlight.ui.clickgui.Theme;
import wtf.moonlight.ui.clickgui.Widgets;
import wtf.moonlight.value.impl.TextValue;

import static wtf.moonlight.ui.clickgui.Widgets.scale;

public final class TextElement extends Element {
    private final TextValue value;
    private final ImString buffer = new ImString(1023);

    public TextElement(Panel panel, TextValue value) {
        super(panel);
        this.value = value;
    }

    @Override
    public float width() {
        return scale(134);
    }

    @Override
    public float height() {
        return scale(23);
    }

    @Override
    public void draw() {
        buffer.set(value.get());
        Theme theme = panel.colors();
        Widgets.pushText(Widgets.TEXT_BODY);
        ImGui.pushStyleVar(ImGuiStyleVar.FrameRounding, scale(5));
        ImGui.pushStyleVar(ImGuiStyleVar.FramePadding, scale(7), scale(4));
        ImGui.pushStyleVar(ImGuiStyleVar.FrameBorderSize, 0);
        ImGui.pushStyleColor(ImGuiCol.FrameBg, theme.selectorBg);
        ImGui.pushStyleColor(ImGuiCol.FrameBgHovered, theme.selectorBg);
        ImGui.pushStyleColor(ImGuiCol.FrameBgActive, theme.selectorBg);
        ImGui.pushStyleColor(ImGuiCol.Text, theme.main);
        ImGui.pushStyleColor(ImGuiCol.TextDisabled, theme.secondary);
        ImGui.pushStyleColor(ImGuiCol.TextSelectedBg, Colors.reAlpha(theme.iconHighlight, Controls.FIELD_SELECTION_ALPHA));
        ImGui.pushStyleColor(ImGuiCol.NavCursor, 0);
        ImGui.setNextItemWidth(width());
        if (ImGui.inputText("##text", buffer)) value.set(buffer.get());
        float response = Controls.motion(ImGui.getItemID() ^ 0x6161, ImGui.isItemActive() || ImGui.isItemHovered() ? 1 : 0, 16, -1);
        int outline = Colors.mix(theme.split, theme.iconHighlight, response * Controls.FIELD_HOVER_BORDER);
        float inset = scale(0.5f);
        ImGui.getWindowDrawList().addRect(ImGui.getItemRectMinX() + inset, ImGui.getItemRectMinY() + inset,
                ImGui.getItemRectMaxX() - inset, ImGui.getItemRectMaxY() - inset, ImGui.getColorU32i(outline), scale(5.5f));
        ImGui.popStyleColor(7);
        ImGui.popStyleVar(3);
        Widgets.popFont();
    }
}
