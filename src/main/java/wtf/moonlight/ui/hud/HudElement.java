package wtf.moonlight.ui.hud;

import org.jspecify.annotations.Nullable;
import wtf.moonlight.module.Module;

public interface HudElement {
    Module module();

    @Nullable Rect bounds();

    void place(float x, float y);

    void frame(Style style);
}
