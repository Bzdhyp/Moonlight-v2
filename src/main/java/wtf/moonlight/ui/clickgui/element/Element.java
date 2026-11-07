package wtf.moonlight.ui.clickgui.element;

import wtf.moonlight.ui.clickgui.Panel;
import wtf.moonlight.ui.clickgui.Widgets;

public abstract class Element {
    protected final Panel panel;

    protected Element(Panel panel) {
        this.panel = panel;
    }

    public abstract float width();

    public float height() {
        return Widgets.scale(18);
    }

    public abstract void draw();
}
