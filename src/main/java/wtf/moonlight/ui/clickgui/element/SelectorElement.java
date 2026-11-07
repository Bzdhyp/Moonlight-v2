package wtf.moonlight.ui.clickgui.element;

import imgui.ImGui;
import wtf.moonlight.ui.clickgui.Controls;
import wtf.moonlight.ui.clickgui.Panel;
import wtf.moonlight.ui.clickgui.Theme;
import wtf.moonlight.ui.clickgui.Widgets;
import wtf.moonlight.value.impl.ModeValue;

import java.util.List;

import static wtf.moonlight.ui.clickgui.Widgets.scale;

public final class SelectorElement extends Element {
    private final ModeValue value;

    public SelectorElement(Panel panel, ModeValue value) {
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
        List<String> modes = value.getModes();
        float x = ImGui.getCursorScreenPosX();
        float y = ImGui.getCursorScreenPosY();
        if (Controls.select(ImGui.getWindowDrawList(), 0, x, y, scale(134), modes.get(value.get()))) ImGui.openPopup("popup");
        float displayX = ImGui.getIO().getDisplaySizeX();
        float displayY = ImGui.getIO().getDisplaySizeY();
        float popupHeight = Math.min(scale(32 * modes.size() + 8), displayY - scale(20));
        float atX = Math.clamp(x, scale(10), Math.max(scale(10), displayX - scale(144)));
        float atY = Math.clamp(y + (scale(23) - popupHeight) * .5f, scale(10), Math.max(scale(10), displayY - popupHeight - scale(10)));
        ImGui.setNextWindowPos(atX, atY);
        ImGui.setNextWindowSize(scale(134), popupHeight);
        if (Widgets.beginPopup(theme, "popup")) {
            for (int i = 0; i < modes.size(); i++) {
                ImGui.pushID(i);
                if (Widgets.choice(theme, "option", modes.get(i), value.get() == i, ImGui.getContentRegionAvailX())) {
                    value.set(i);
                    ImGui.closeCurrentPopup();
                }
                ImGui.popID();
            }
            Widgets.endPopup();
        }
    }
}
