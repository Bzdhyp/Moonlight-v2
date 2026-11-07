package wtf.moonlight.event.impl.input;

import com.mojang.blaze3d.platform.InputConstants;
import wtf.moonlight.event.CancellableEvent;

public final class KeyInputEvent extends CancellableEvent {
    private final int key;
    private final int action;

    public KeyInputEvent(int key, int action) {
        this.key = key;
        this.action = action;
    }

    public int getKey() {
        return key;
    }

    public boolean isPress() {
        return action == InputConstants.PRESS;
    }

    public boolean isRepeat() {
        return action == InputConstants.REPEAT;
    }
}
