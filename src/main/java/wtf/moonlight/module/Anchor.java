package wtf.moonlight.module;

import wtf.moonlight.value.impl.ModeValue;
import wtf.moonlight.value.impl.NumberValue;

public final class Anchor {
    private static final double MARGIN = 12;

    private final ModeValue position;
    private final NumberValue x;
    private final NumberValue y;

    public Anchor(Module module, double x, double y) {
        position = module.add(new ModeValue("Position", 0, "Top Left", "Top Right", "Bottom Left", "Bottom Right"));
        this.x = module.add(new NumberValue("X", 0, 10000, 0.1, x, "%.1f", () -> false));
        this.y = module.add(new NumberValue("Y", 0, 10000, 0.1, y, "%.1f", () -> false));
        position.onSelect(index -> {
            this.x.set(MARGIN);
            this.y.set(MARGIN);
        });
    }

    public boolean right() {
        return position.get() % 2 == 1;
    }

    public boolean bottom() {
        return position.get() >= 2;
    }

    public float left(float screen, float width, float unit) {
        return right() ? screen - x.get().floatValue() * unit - width : x.get().floatValue() * unit;
    }

    public float top(float screen, float height, float unit) {
        return bottom() ? screen - y.get().floatValue() * unit - height : y.get().floatValue() * unit;
    }

    public void drop(float screenWidth, float screenHeight, float left, float top, float width, float height, float unit) {
        float clampedLeft = Math.clamp(left, 0, Math.max(0, screenWidth - width));
        float clampedTop = Math.clamp(top, 0, Math.max(0, screenHeight - height));
        boolean right = clampedLeft + width / 2 > screenWidth / 2;
        boolean bottom = clampedTop + height / 2 > screenHeight / 2;
        position.set((bottom ? 2 : 0) + (right ? 1 : 0));
        x.set((double) ((right ? screenWidth - clampedLeft - width : clampedLeft) / unit));
        y.set((double) ((bottom ? screenHeight - clampedTop - height : clampedTop) / unit));
    }
}
