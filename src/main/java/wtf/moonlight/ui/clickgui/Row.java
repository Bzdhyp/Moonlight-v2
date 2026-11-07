package wtf.moonlight.ui.clickgui;

import imgui.ImDrawList;
import imgui.ImGui;
import imgui.ImGuiViewport;
import imgui.ImVec2;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiMouseButton;
import imgui.flag.ImGuiMouseCursor;
import imgui.flag.ImGuiStyleVar;
import imgui.flag.ImGuiWindowFlags;
import org.jspecify.annotations.Nullable;
import wtf.moonlight.bind.Bind;
import wtf.moonlight.module.Module;
import wtf.moonlight.ui.clickgui.element.BindElement;
import wtf.moonlight.ui.clickgui.element.ColorElement;
import wtf.moonlight.ui.clickgui.element.Element;
import wtf.moonlight.ui.clickgui.element.ListElement;
import wtf.moonlight.ui.clickgui.element.SelectorElement;
import wtf.moonlight.ui.clickgui.element.SliderElement;
import wtf.moonlight.ui.clickgui.element.SwitchElement;
import wtf.moonlight.ui.clickgui.element.TextElement;
import wtf.moonlight.ui.imgui.Fonts;
import wtf.moonlight.value.Value;
import wtf.moonlight.value.impl.BoolValue;
import wtf.moonlight.value.impl.ColorValue;
import wtf.moonlight.value.impl.ListValue;
import wtf.moonlight.value.impl.ModeValue;
import wtf.moonlight.value.impl.NumberValue;
import wtf.moonlight.value.impl.TextValue;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

import static wtf.moonlight.ui.clickgui.Widgets.scale;

final class Row {
    private static final float ROW_HEIGHT = 37;
    private static final float LABEL_GAP = 6;
    private static final float GROUP_BUTTON = 20;
    private static final float GROUP_GAP = 2;
    private static final float CONTROL_OVERHANG = 7;
    private static final float POPUP_WIDTH = 260;
    private static final float HUD_POPUP_WIDTH = 244;
    private static final float POPUP_TITLE = 36;
    private static final float POPUP_ROW = 30;
    private static final float POPUP_INSET = 6;
    private static final float POPUP_PAD = 14;
    private static final float FOLLOW = 12;
    private static final float DOT = 1.3F;
    private static final float DOT_GAP = 4;
    private static final float INLINE_GAP = 6;

    private static int nextId;

    private final Panel panel;
    private final String name;
    private final @Nullable Element element;
    private final List<Row> children;
    private final BooleanSupplier visibility;
    private final @Nullable Bind bind;
    private final @Nullable BindElement hotkeys;
    private final boolean dots;
    private final int id = nextId++;
    private float placedX = Float.NaN;
    private float placedY;
    private float lastMinX = Float.NaN;
    private float lastMinY;
    private float lastMaxX;
    private float grabX;
    private float grabY;
    private boolean manual;
    private boolean dragging;

    Row(Panel panel, String name, @Nullable Element element, List<Row> children, BooleanSupplier visibility) {
        this(panel, name, element, children, visibility, null, false);
    }

    private Row(Panel panel, String name, @Nullable Element element, List<Row> children, BooleanSupplier visibility,
                @Nullable Bind bind, boolean dots) {
        this.panel = panel;
        this.name = name;
        this.element = element;
        this.children = children;
        this.visibility = visibility;
        this.bind = bind;
        this.hotkeys = bind != null ? new BindElement(panel, bind, panel.capture(), panel.modules()) : null;
        this.dots = dots;
    }

    static Row of(Panel panel, Value<?> value, boolean compact) {
        Element element = switch (value) {
            case BoolValue bool -> new SwitchElement(panel, bool::get, bool::set);
            case NumberValue number -> new SliderElement(panel, number, compact);
            case ModeValue mode -> new SelectorElement(panel, mode);
            case TextValue text -> new TextElement(panel, text);
            case ColorValue color -> new ColorElement(panel, color);
            case ListValue list -> new ListElement(panel, list);
            default -> null;
        };
        return new Row(panel, value.getName(), element, List.of(), value::isVisible);
    }

    static Row of(Panel panel, Module module) {
        List<Row> children = new ArrayList<>();
        children.add(bindRow(panel, module));
        for (Value<?> value : module.getValues()) children.add(of(panel, value, true));
        return new Row(panel, module.getName(), new SwitchElement(panel, module::isEnabled, module::setEnabled), children, () -> true,
                module.getBind(), true);
    }

    static Row enabled(Panel panel, Module module) {
        return new Row(panel, "Enabled", new SwitchElement(panel, module::isEnabled, module::setEnabled), List.of(bindRow(panel, module)), () -> true,
                module.getBind(), false);
    }

    private static Row bindRow(Panel panel, Module module) {
        return new Row(panel, "Bind", new BindElement(panel, module.getBind(), panel.capture(), panel.modules()), List.of(), () -> true);
    }

    boolean isVisible() {
        return visibility.getAsBoolean();
    }

    void render() {
        render(ROW_HEIGHT);
    }

    private void render(float height) {
        Theme theme = panel.colors();
        ImGui.pushStyleVar(ImGuiStyleVar.ChildRounding, 0);
        ImGui.pushStyleColor(ImGuiCol.ChildBg, 0);
        ImGui.pushID(id);
        Windows.beginChild(name + "###row", 0, scale(height), ImGuiWindowFlags.NoScrollbar | ImGuiWindowFlags.NoScrollWithMouse);
        float rowY = ImGui.getCursorPosY();
        float rowWidth = ImGui.getContentRegionAvailX() - scale(CONTROL_OVERHANG);
        float elementWidth = element != null ? element.width() : 0;
        float elementX = rowWidth - elementWidth;
        float dotsX = elementX - scale(GROUP_GAP + GROUP_BUTTON);
        float controlsX = dots ? dotsX : elementX;
        Widgets.pushText(Widgets.TEXT_BODY);
        float x = ImGui.getCursorScreenPosX();
        float y = ImGui.getCursorScreenPosY();
        ImDrawList draw = ImGui.getWindowDrawList();
        BindElement icon = bind != null && bind.isBound() ? hotkeys : null;
        float iconWidth = icon != null ? scale(INLINE_GAP) + icon.width() : 0;
        float labelWidth = Math.max(1, controlsX - scale(LABEL_GAP) - iconWidth);
        ImVec2 labelSize = ImGui.calcTextSize(name, false, labelWidth);
        if (labelSize.y > scale(height - 4)) {
            Widgets.popFont();
            Widgets.pushText(Widgets.TEXT_SMALL);
            labelSize = ImGui.calcTextSize(name, false, labelWidth);
        }
        draw.pushClipRect(x, y, x + labelWidth, y + scale(height), true);
        Controls.addText(draw, ImGui.getFont(), ImGui.getFontSize(), x,
                y + Math.max(0, (scale(height) - labelSize.y) * .5f - scale(1)), theme.main, name, labelWidth);
        draw.popClipRect();
        float textWidth = Widgets.textWidth(name);
        if (textWidth > labelWidth && ImGui.isWindowHovered() && ImGui.isMouseHoveringRect(x, y, x + labelWidth, y + scale(height))) {
            ImGui.setTooltip(name.replace("%", "%%"));
        }
        Widgets.popFont();
        boolean menu = !children.isEmpty() && ImGui.isWindowHovered() && ImGui.isMouseClicked(ImGuiMouseButton.Right);
        if (icon != null) {
            ImGui.setCursorScreenPos(x + Math.min(textWidth, labelWidth) + scale(INLINE_GAP), y + (scale(height) - icon.height()) * .5f);
            icon.draw();
        }
        if (element != null) {
            ImGui.setCursorPos(Math.max(0, elementX), rowY + (scale(height) - element.height()) * .5f);
            element.draw();
        }
        if (dots) {
            ImGui.setCursorPos(dotsX, rowY + (scale(height) - scale(GROUP_BUTTON)) * .5f);
            if (ImGui.invisibleButton("dots", scale(GROUP_BUTTON), scale(GROUP_BUTTON))) menu = true;
            float hover = Widgets.motion(ImGui.getItemID() ^ 0x3d07, ImGui.isPopupOpen("grouppopup") ? 1 : ImGui.isItemHovered() ? .7f : 0, 20);
            float centerX = (ImGui.getItemRectMinX() + ImGui.getItemRectMaxX()) * .5f;
            float centerY = (ImGui.getItemRectMinY() + ImGui.getItemRectMaxY()) * .5f;
            for (int i = -1; i <= 1; i++) {
                draw.addCircleFilled(centerX + scale(DOT_GAP) * i, centerY, scale(DOT), Colors.mix(theme.secondary, theme.textHighlight, hover));
            }
        }
        if (menu) ImGui.openPopup("grouppopup");
        if (ImGui.isPopupOpen("grouppopup")) {
            float right = ImGui.getWindowPosX() + rowWidth;
            renderGroupPopup(theme, right - scale(GROUP_BUTTON), y, right, y + scale(height), POPUP_WIDTH, false);
        }
        Windows.endChild();
        ImGui.popID();
        ImGui.popStyleColor();
        ImGui.popStyleVar();
    }

    boolean popup(Theme theme, float minX, float minY, float maxX, float maxY, boolean open) {
        ImGui.pushID(id);
        if (open) ImGui.openPopup("grouppopup");
        boolean shown = ImGui.isPopupOpen("grouppopup");
        if (shown) renderGroupPopup(theme, minX, minY, maxX, maxY, HUD_POPUP_WIDTH, true);
        ImGui.popID();
        return shown;
    }

    private void renderGroupPopup(Theme theme, float anchorMinX, float anchorMinY, float anchorMaxX, float anchorMaxY, float width, boolean follow) {
        ImGuiViewport viewport = ImGui.getMainViewport();
        float safeMinX = viewport.getWorkPosX() + scale(12);
        float safeMinY = viewport.getWorkPosY() + scale(12);
        float safeMaxX = viewport.getWorkPosX() + viewport.getWorkSizeX() - scale(12);
        float safeMaxY = viewport.getWorkPosY() + viewport.getWorkSizeY() - scale(12);
        long visibleRows = children.stream().filter(Row::isVisible).count();
        float listHeight = Math.min(scale(Math.max(1, visibleRows) * POPUP_ROW),
                Math.max(scale(POPUP_ROW), Math.min(viewport.getWorkSizeY() * .7f, safeMaxY - safeMinY - scale(POPUP_TITLE + POPUP_INSET + 8))));
        float popupWidth = Math.min(scale(width), Math.max(1, safeMaxX - safeMinX));
        float popupHeight = scale(POPUP_TITLE + POPUP_INSET + 8) + listHeight;
        float gap = scale(8);
        float x;
        float y;
        if (follow) {
            boolean moved = !Float.isNaN(lastMinX) && (Math.abs(anchorMinY - lastMinY) > 1
                    || Math.abs(anchorMinX - lastMinX) > 1 && Math.abs(anchorMaxX - lastMaxX) > 1);
            lastMinX = anchorMinX;
            lastMinY = anchorMinY;
            lastMaxX = anchorMaxX;
            if (moved && !dragging) manual = false;
            float bestX = anchorMaxX + gap;
            float bestY = anchorMinY;
            if (bestX + popupWidth > safeMaxX) {
                bestX = anchorMinX - gap - popupWidth;
                if (bestX < safeMinX) {
                    bestX = anchorMinX;
                    bestY = anchorMaxY + gap + popupHeight <= safeMaxY ? anchorMaxY + gap : anchorMinY - gap - popupHeight;
                }
            }
            if (Float.isNaN(placedX)) {
                placedX = bestX;
                placedY = bestY;
            } else if (dragging) {
                placedX = ImGui.getMousePosX() - grabX;
                placedY = ImGui.getMousePosY() - grabY;
            } else if (!manual) {
                float ease = 1 - (float) Math.exp(-FOLLOW * ImGui.getIO().getDeltaTime());
                placedX += (bestX - placedX) * ease;
                placedY += (bestY - placedY) * ease;
            }
            placedX = Math.clamp(placedX, safeMinX, Math.max(safeMinX, safeMaxX - popupWidth));
            placedY = Math.clamp(placedY, safeMinY, Math.max(safeMinY, safeMaxY - popupHeight));
            x = placedX;
            y = placedY;
        } else {
            float right = anchorMaxX + gap;
            float left = anchorMinX - gap - popupWidth;
            x = right;
            if (right + popupWidth > safeMaxX) {
                x = left >= safeMinX ? left : (safeMaxX - anchorMaxX >= anchorMinX - safeMinX ? right : left);
            }
            y = (anchorMinY + anchorMaxY) * .5f - scale(POPUP_TITLE + 4 + POPUP_ROW * .5f);
        }
        ImGui.setNextWindowPos(Math.clamp(x, safeMinX, Math.max(safeMinX, safeMaxX - popupWidth)),
                Math.clamp(y, safeMinY, Math.max(safeMinY, safeMaxY - popupHeight)), follow ? ImGuiCond.Always : ImGuiCond.Appearing);
        ImGui.setNextWindowSize(popupWidth, popupHeight);
        int flags = ImGuiWindowFlags.NoScrollbar | (follow ? ImGuiWindowFlags.NoMove : 0);
        if (Widgets.beginPopup(theme, "grouppopup", flags, Popups.Motion.DROPDOWN, 0, 0)) {
            float popupX = ImGui.getWindowPosX();
            float popupY = ImGui.getWindowPosY();
            if (follow) drag(popupX, popupY, popupWidth);
            ImDrawList draw = ImGui.getWindowDrawList();
            ImGui.pushFont(Fonts.wordmark(), 16);
            draw.pushClipRect(popupX, popupY, popupX + popupWidth - scale(POPUP_PAD), popupY + scale(POPUP_TITLE), true);
            draw.addText(popupX + scale(POPUP_INSET + POPUP_PAD), popupY + (scale(POPUP_TITLE + 4) - Widgets.textHeight()) * .5f, theme.textHighlight, name);
            draw.popClipRect();
            ImGui.popFont();
            float cardMinX = popupX + scale(POPUP_INSET);
            float cardMinY = popupY + scale(POPUP_TITLE);
            float cardMaxX = popupX + popupWidth - scale(POPUP_INSET);
            float cardMaxY = cardMinY + listHeight + scale(8);
            draw.addRectFilled(cardMinX, cardMinY, cardMaxX, cardMaxY, theme.modulePanelBg, scale(10));
            draw.addRect(cardMinX, cardMinY, cardMaxX, cardMaxY, theme.split, scale(10), 0, scale(1));
            ImGui.setCursorScreenPos(cardMinX + scale(POPUP_PAD), cardMinY + scale(4));
            ImGui.pushStyleVar(ImGuiStyleVar.ItemSpacing, 0, 0);
            Windows.beginChild("grouppanel", cardMaxX - cardMinX - scale(2 * POPUP_PAD - CONTROL_OVERHANG), listHeight, ImGuiWindowFlags.NoBackground | ImGuiWindowFlags.NoScrollbar);
            for (Row child : children) {
                if (child.isVisible()) child.render(POPUP_ROW);
            }
            Windows.endChild();
            ImGui.popStyleVar();
            Widgets.endPopup();
        }
    }

    private void drag(float x, float y, float width) {
        boolean header = ImGui.isWindowHovered() && ImGui.isMouseHoveringRect(x, y, x + width, y + scale(POPUP_TITLE));
        if (header && ImGui.isMouseClicked(ImGuiMouseButton.Left)) {
            dragging = true;
            grabX = ImGui.getMousePosX() - x;
            grabY = ImGui.getMousePosY() - y;
        }
        if (dragging && !ImGui.isMouseDown(ImGuiMouseButton.Left)) {
            dragging = false;
            manual = true;
        }
        if (header || dragging) ImGui.setMouseCursor(ImGuiMouseCursor.ResizeAll);
    }
}
