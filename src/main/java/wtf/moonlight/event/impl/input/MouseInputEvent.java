package wtf.moonlight.event.impl.input;

import com.mojang.blaze3d.platform.InputConstants;
import wtf.moonlight.event.CancellableEvent;

public final class MouseInputEvent extends CancellableEvent {
    private final int button;
    private final int action;

    public MouseInputEvent(int button, int action) {
        this.button = button;
        this.action = action;
    }

    public int getButton() {
        return button;
    }

    public boolean isPress() {
        return action == InputConstants.PRESS;
    }
}
