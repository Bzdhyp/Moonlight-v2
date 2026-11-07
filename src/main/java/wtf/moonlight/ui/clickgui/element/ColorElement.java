package wtf.moonlight.ui.clickgui.element;

import imgui.ImDrawList;
import imgui.ImGui;
import wtf.moonlight.ui.clickgui.Panel;
import wtf.moonlight.ui.clickgui.Theme;
import wtf.moonlight.ui.clickgui.Widgets;
import wtf.moonlight.value.impl.ColorValue;

import static wtf.moonlight.ui.clickgui.Widgets.scale;

public final class ColorElement extends Element {
    private final ColorValue value;

    public ColorElement(Panel panel, ColorValue value) {
        super(panel);
        this.value = value;
    }

    @Override
    public float width() {
        return scale(24);
    }

    @Override
    public void draw() {
        Theme theme = panel.colors();
        float x = ImGui.getCursorScreenPosX();
        float y = ImGui.getCursorScreenPosY();
        if (ImGui.invisibleButton("##color", width(), height())) ImGui.openPopup("##palette");
        ImDrawList draw = ImGui.getWindowDrawList();
        draw.addRectFilled(x + scale(5), y + scale(2), x + scale(19), y + scale(16), value.get(), scale(3));
        draw.addRect(x + scale(5), y + scale(2), x + scale(19), y + scale(16), theme.windowBorder, scale(3));
        ImGui.setNextWindowSize(scale(Palette.WIDTH), scale(Palette.HEIGHT));
        if (Widgets.beginPopup(theme, "##palette")) {
            Palette.draw(theme, value);
            Widgets.endPopup();
        }
    }
}
