package wtf.moonlight.ui.clickgui.element;

import imgui.ImDrawList;
import imgui.ImGui;
import org.jspecify.annotations.Nullable;
import wtf.moonlight.ui.clickgui.Colors;
import wtf.moonlight.ui.clickgui.Controls;
import wtf.moonlight.ui.clickgui.Panel;
import wtf.moonlight.ui.clickgui.Theme;
import wtf.moonlight.ui.clickgui.Widgets;
import wtf.moonlight.ui.imgui.Fonts;
import wtf.moonlight.value.impl.ListValue;

import java.util.ArrayList;
import java.util.List;

import static wtf.moonlight.ui.clickgui.Widgets.scale;

public final class ListElement extends Element {
    private static final float ROW = 32;
    private static final float WIDTH = 188;
    private static final float PLAIN_WIDTH = 160;
    private static final float ICON = 14;
    private static final float SLIDE = 18;
    private static final float DRAG_START = 4;
    private static final double LONG_PRESS = 0.25;
    private static final String GRIP = Character.toString(0xF58E);

    private final ListValue value;
    private @Nullable String held;
    private float grab;
    private float pressY;
    private double pressedAt;
    private boolean dragging;

    public ListElement(Panel panel, ListValue value) {
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
        Theme theme = panel.colors();
        List<String> shown = new ArrayList<>();
        for (String option : value.getOptions()) {
            if (value.has(option)) shown.add(option);
        }
        float x = ImGui.getCursorScreenPosX();
        float y = ImGui.getCursorScreenPosY();
        if (Controls.select(ImGui.getWindowDrawList(), 0, x, y, width(), shown.isEmpty() ? "None" : String.join(", ", shown))) {
            ImGui.openPopup("##list");
        }
        float step = scale(ROW);
        ImGui.setNextWindowSize(scale(value.isSortable() ? WIDTH : PLAIN_WIDTH), scale(8) + step * value.getOptions().size());
        if (!Widgets.beginPopup(theme, "##list")) {
            held = null;
            dragging = false;
            return;
        }
        rows(theme, step);
        Widgets.endPopup();
    }

    private void rows(Theme theme, float step) {
        float left = ImGui.getCursorScreenPosX();
        float top = ImGui.getCursorScreenPosY();
        float width = ImGui.getContentRegionAvailX();
        float height = step * value.getOptions().size();
        ImGui.invisibleButton("##rows", width, height);
        boolean hovered = ImGui.isItemHovered();
        float mouseY = ImGui.getMousePosY();
        int under = Math.clamp((int) ((mouseY - top) / step), 0, value.getOptions().size() - 1);
        if (ImGui.isItemActivated()) {
            held = value.getOptions().get(under);
            grab = mouseY - top - under * step;
            pressY = mouseY;
            pressedAt = ImGui.getTime();
            dragging = false;
        }
        String current = held;
        if (current != null && value.isSortable() && ImGui.isItemActive() && !dragging
                && (ImGui.getTime() - pressedAt >= LONG_PRESS || Math.abs(mouseY - pressY) > scale(DRAG_START))) {
            dragging = true;
        }
        float lifted = Math.clamp(mouseY - grab - top, 0, height - step);
        if (current != null && dragging) {
            int slot = Math.round(lifted / step);
            if (value.getOptions().indexOf(current) != slot) value.move(current, slot);
        }
        if (current != null && ImGui.isItemDeactivated()) {
            if (!dragging) value.toggle(current);
            held = null;
            dragging = false;
        }

        ImDrawList draw = ImGui.getWindowDrawList();
        List<String> options = value.getOptions();
        String carried = dragging ? held : null;
        float carriedGrip = 0;
        for (int i = 0; i < options.size(); i++) {
            String option = options.get(i);
            int id = ImGui.getID("##slot" + option);
            boolean moving = option.equals(carried);
            float offset = moving ? lifted : Widgets.motion(id, i * step, SLIDE);
            if (moving) ImGui.getStateStorage().setFloat(id, lifted);
            float hover = Widgets.motion(id ^ 0x5A17, !dragging && hovered && i == under ? 1 : 0, 22);
            float grip = Widgets.motion(id ^ 0x6A11, moving ? 1 : 0, 22);
            if (moving) carriedGrip = grip;
            else row(draw, theme, option, left, top + offset, width, hover, grip, false);
        }
        if (carried != null) row(draw, theme, carried, left, top + lifted, width, 0, carriedGrip, true);
    }

    private void row(ImDrawList draw, Theme theme, String option, float x, float y, float width, float hover, float grip, boolean lifted) {
        float height = scale(ROW);
        int fill = lifted ? theme.selectorElementActive : Colors.reAlpha(theme.selectorElementHover, (int) (Colors.alpha(theme.selectorElementHover) * hover));
        draw.addRectFilled(x, y, x + width, y + height, fill, scale(12));
        boolean on = value.has(option);
        int color = on ? theme.textHighlight : theme.main;
        if (on) Controls.check(draw, x + scale(14), y + height / 2, theme.textHighlight);
        String icon = value.icon(option);
        if (icon != null) {
            ImGui.pushFont(Fonts.hudIcons(), ICON * Fonts.HUD_ICON_EM);
            draw.addText(x + scale(30), y + (height - Widgets.textHeight()) * .5f, color, icon);
            Widgets.popFont();
        }
        Widgets.pushText(Widgets.TEXT_CONTROL);
        draw.addText(x + scale(icon != null ? 52 : 35), y + (height - Widgets.textHeight()) * .5f, color, option);
        Widgets.popFont();
        if (!value.isSortable()) return;
        Widgets.pushIcon();
        draw.addText(x + width - scale(22), y + (height - Widgets.textHeight()) * .5f, Colors.interpolate(theme.secondary, theme.textHighlight, grip), GRIP);
        Widgets.popFont();
    }
}
