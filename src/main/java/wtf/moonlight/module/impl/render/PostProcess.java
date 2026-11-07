package wtf.moonlight.module.impl.render;

import wtf.moonlight.module.Category;
import wtf.moonlight.module.Module;
import wtf.moonlight.value.impl.BoolValue;
import wtf.moonlight.value.impl.NumberValue;

public final class PostProcess extends Module {
    private final BoolValue blur = add(new BoolValue("Render Blur", true));
    private final BoolValue shadow = add(new BoolValue("Render Shadow", true));
    private final NumberValue blurAmount = add(new NumberValue("Blur Amount", 1, 8, 1, 4, "%.0f", blur::get));
    private final NumberValue blurOffset = add(new NumberValue("Blur Offset", 1, 10, 1, 10, "%.0f", blur::get));
    private final NumberValue bloomAmount = add(new NumberValue("Bloom Amount", 1, 8, 1, 2, "%.0f", shadow::get));
    private final NumberValue bloomOffset = add(new NumberValue("Bloom Offset", 1, 10, 1, 2, "%.0f", shadow::get));

    public PostProcess() {
        super("PostProcess", Category.RENDER);
    }

    public boolean blur() {
        return isEnabled() && blur.get();
    }

    public boolean shadow() {
        return isEnabled() && shadow.get();
    }

    public int blurAmount() {
        return blurAmount.get().intValue();
    }

    public int blurOffset() {
        return blurOffset.get().intValue();
    }

    public int bloomAmount() {
        return bloomAmount.get().intValue();
    }

    public int bloomOffset() {
        return bloomOffset.get().intValue();
    }
}
