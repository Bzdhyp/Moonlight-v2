package wtf.moonlight.module.impl.render;

import wtf.moonlight.module.Anchor;
import wtf.moonlight.module.Module;
import wtf.moonlight.module.Subcategory;
import wtf.moonlight.value.impl.NumberValue;

public final class Hotkeys extends Module {
    private final Anchor anchor = new Anchor(this, 12, 52);
    private final NumberValue scale = add(new NumberValue("Scale", 0.5, 3, 0.05, 1.5, "%.2f"));

    public Hotkeys() {
        super("Hotkeys", Subcategory.OVERLAY);
    }

    public Anchor anchor() {
        return anchor;
    }

    public float scale() {
        return scale.get().floatValue();
    }
}
