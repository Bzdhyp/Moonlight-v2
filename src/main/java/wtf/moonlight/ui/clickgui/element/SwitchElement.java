package wtf.moonlight.ui.clickgui.element;

import imgui.ImGui;
import wtf.moonlight.ui.clickgui.Controls;
import wtf.moonlight.ui.clickgui.Panel;
import wtf.moonlight.ui.clickgui.Widgets;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public final class SwitchElement extends Element {
    private final BooleanSupplier getter;
    private final Consumer<Boolean> setter;

    public SwitchElement(Panel panel, BooleanSupplier getter, Consumer<Boolean> setter) {
        super(panel);
        this.getter = getter;
        this.setter = setter;
    }

    @Override
    public float width() {
        return Widgets.scale(29);
    }

    @Override
    public void draw() {
        boolean value = getter.getAsBoolean();
        boolean next = Controls.toggle(ImGui.getWindowDrawList(), 0, ImGui.getCursorScreenPosX(), ImGui.getCursorScreenPosY(), value);
        if (next != value) setter.accept(next);
    }
}
