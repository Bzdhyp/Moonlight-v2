package wtf.moonlight.ui.clickgui;

import imgui.ImDrawList;
import imgui.ImGui;
import imgui.ImGuiViewport;
import imgui.ImVec2;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiStyleVar;
import imgui.flag.ImGuiWindowFlags;
import org.jspecify.annotations.Nullable;
import wtf.moonlight.ui.clickgui.element.Element;
import wtf.moonlight.ui.clickgui.element.SelectorElement;
import wtf.moonlight.ui.clickgui.element.SliderElement;
import wtf.moonlight.ui.clickgui.element.SwitchElement;
import wtf.moonlight.ui.clickgui.element.TextElement;
import wtf.moonlight.value.Value;
import wtf.moonlight.value.impl.BoolValue;
import wtf.moonlight.value.impl.ModeValue;
import wtf.moonlight.value.impl.NumberValue;
import wtf.moonlight.value.impl.TextValue;

import java.util.List;
import java.util.function.BooleanSupplier;

import static wtf.moonlight.ui.clickgui.Widgets.scale;

final class Row {
    private static final float ROW_HEIGHT = 37;
    private static final float LABEL_GAP = 6;
    private static final float GROUP_BUTTON = 20;
    private static final float GROUP_GAP = 2;
    private static final float CONTROL_OVERHANG = 7;
    private static final String GEAR = Character.toString(0xF013);

    private static int nextId;

    private final Panel panel;
    private final String name;
    private final @Nullable Element element;
    private final List<Row> children;
    private final BooleanSupplier visibility;
    private final int id = nextId++;

    Row(Panel panel, String name, @Nullable Element element, List<Row> children, BooleanSupplier visibility) {
        this.panel = panel;
        this.name = name;
        this.element = element;
        this.children = children;
        this.visibility = visibility;
    }

    static Row of(Panel panel, Value<?> value) {
        Element element = switch (value) {
            case BoolValue bool -> new SwitchElement(panel, bool::get, bool::set);
            case NumberValue number -> new SliderElement(panel, number);
            case ModeValue mode -> new SelectorElement(panel, mode);
            case TextValue text -> new TextElement(panel, text);
            default -> null;
        };
        return new Row(panel, value.getName(), element, List.of(), value::isVisible);
    }

    boolean isVisible() {
        return visibility.getAsBoolean();
    }

    void render() {
        Theme theme = panel.colors();
        ImGui.pushStyleVar(ImGuiStyleVar.ChildRounding, 0);
        ImGui.pushStyleColor(ImGuiCol.ChildBg, 0);
        ImGui.pushID(id);
        Windows.beginChild(name + "###row", 0, scale(ROW_HEIGHT), ImGuiWindowFlags.NoScrollbar | ImGuiWindowFlags.NoScrollWithMouse);
        float rowY = ImGui.getCursorPosY();
        float rowWidth = ImGui.getContentRegionAvailX() - scale(CONTROL_OVERHANG);
        boolean hasGroup = !children.isEmpty();
        float groupWidth = hasGroup ? scale(GROUP_BUTTON + GROUP_GAP) : 0;
        float elementWidth = element != null ? element.width() : 0;
        float elementX = rowWidth - groupWidth - elementWidth;
        Widgets.pushText(Widgets.TEXT_BODY);
        float x = ImGui.getCursorScreenPosX();
        float y = ImGui.getCursorScreenPosY();
        ImDrawList draw = ImGui.getWindowDrawList();
        float labelWidth = Math.max(1, elementX - scale(LABEL_GAP));
        ImVec2 labelSize = ImGui.calcTextSize(name, false, labelWidth);
        if (labelSize.y > scale(ROW_HEIGHT - 4)) {
            Widgets.popFont();
            Widgets.pushText(Widgets.TEXT_SMALL);
            labelSize = ImGui.calcTextSize(name, false, labelWidth);
        }
        draw.pushClipRect(x, y, x + labelWidth, y + scale(ROW_HEIGHT), true);
        Controls.addText(draw, ImGui.getFont(), ImGui.getFontSize(), x,
                y + Math.max(0, (scale(ROW_HEIGHT) - labelSize.y) * .5f - scale(1)), theme.main, name, labelWidth);
        draw.popClipRect();
        if (Widgets.textWidth(name) > elementX - scale(LABEL_GAP) && ImGui.isWindowHovered()
                && ImGui.isMouseHoveringRect(x, y, x + Math.max(0, elementX - scale(LABEL_GAP)), y + scale(ROW_HEIGHT))) {
            ImGui.setTooltip(name.replace("%", "%%"));
        }
        Widgets.popFont();
        if (element != null) {
            ImGui.setCursorPos(Math.max(0, elementX), rowY + (scale(ROW_HEIGHT) - element.height()) * .5f);
            element.draw();
        }
        if (hasGroup) {
            ImGui.setCursorPos(rowWidth - scale(GROUP_BUTTON), rowY + (scale(ROW_HEIGHT) - scale(GROUP_BUTTON)) * .5f);
            renderGroupButton(theme);
        }
        Windows.endChild();
        ImGui.popID();
        ImGui.popStyleColor();
        ImGui.popStyleVar();
    }

    private void renderGroupButton(Theme theme) {
        if (ImGui.invisibleButton("group", scale(GROUP_BUTTON), scale(GROUP_BUTTON))) ImGui.openPopup("grouppopup");
        boolean open = ImGui.isPopupOpen("grouppopup");
        Widgets.pushIcon();
        ImGui.getWindowDrawList().addText(ImGui.getItemRectMinX() + scale(2), ImGui.getItemRectMinY() + scale(3),
                open ? theme.iconHighlight : theme.main, GEAR);
        Widgets.popFont();
        if (open) renderGroupPopup(theme, ImGui.getItemRectMinX(), ImGui.getItemRectMinY(), ImGui.getItemRectMaxX(), ImGui.getItemRectMaxY());
    }

    private void renderGroupPopup(Theme theme, float anchorMinX, float anchorMinY, float anchorMaxX, float anchorMaxY) {
        ImGuiViewport viewport = ImGui.getMainViewport();
        float safeMinX = viewport.getWorkPosX() + scale(12);
        float safeMinY = viewport.getWorkPosY() + scale(12);
        float safeMaxX = viewport.getWorkPosX() + viewport.getWorkSizeX() - scale(12);
        float safeMaxY = viewport.getWorkPosY() + viewport.getWorkSizeY() - scale(12);
        float workHeight = viewport.getWorkSizeY();
        float outerWidth = Math.min(Math.max(ImGui.getWindowWidth(), scale(340)) + scale(24), Math.max(1, safeMaxX - safeMinX));
        float width = Math.max(1, outerWidth - scale(24));
        long visibleRows = children.stream().filter(Row::isVisible).count();
        float contentHeight = visibleRows * (scale(ROW_HEIGHT) + ImGui.getStyle().getItemSpacingY());
        float height = Math.min(Math.max(scale(ROW_HEIGHT), contentHeight),
                Math.max(1, Math.min(workHeight * .7f, safeMaxY - safeMinY - scale(16))));
        float popupWidth = outerWidth;
        float popupHeight = height + scale(16);
        float gap = scale(8);
        float right = anchorMaxX + gap;
        float left = anchorMinX - gap - popupWidth;
        float x = right;
        if (right + popupWidth > safeMaxX) {
            x = left >= safeMinX ? left : (safeMaxX - anchorMaxX >= anchorMinX - safeMinX ? right : left);
        }
        float y = (anchorMinY + anchorMaxY) * .5f - scale(8 + ROW_HEIGHT * .5f);
        ImGui.setNextWindowPos(Math.clamp(x, safeMinX, Math.max(safeMinX, safeMaxX - popupWidth)),
                Math.clamp(y, safeMinY, Math.max(safeMinY, safeMaxY - popupHeight)), ImGuiCond.Appearing);
        ImGui.setNextWindowSize(popupWidth, popupHeight);
        ImGui.pushStyleVar(ImGuiStyleVar.PopupRounding, scale(12));
        ImGui.pushStyleVar(ImGuiStyleVar.PopupBorderSize, scale(1));
        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, scale(12), scale(4));
        ImGui.pushStyleColor(ImGuiCol.PopupBg, theme.modulePanelBg);
        ImGui.pushStyleColor(ImGuiCol.Border, theme.windowBorder);
        if (Widgets.beginPopup(theme, "grouppopup", ImGuiWindowFlags.NoScrollbar, Popups.Motion.DROPDOWN, 12, 8)) {
            ImGui.setCursorPosX(ImGui.getCursorPosX() + scale(CONTROL_OVERHANG));
            Windows.beginChild("grouppanel", width - scale(CONTROL_OVERHANG), height, ImGuiWindowFlags.NoBackground | ImGuiWindowFlags.NoScrollbar);
            for (Row child : children) {
                if (child.isVisible()) child.render();
            }
            Windows.endChild();
            Widgets.endPopup();
        }
        ImGui.popStyleColor(2);
        ImGui.popStyleVar(3);
    }
}
