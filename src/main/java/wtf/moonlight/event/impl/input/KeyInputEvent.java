package wtf.moonlight.event.impl.input;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.input.KeyEvent;
import wtf.moonlight.event.CancellableEvent;

public final class KeyInputEvent extends CancellableEvent {
    private final KeyEvent event;
    private final int action;

    public KeyInputEvent(KeyEvent event, int action) {
        this.event = event;
        this.action = action;
    }

    public KeyEvent getEvent() {
        return event;
    }

    public int getKey() {
        return event.key();
    }

    public boolean isPress() {
        return action == InputConstants.PRESS;
    }

    public boolean isRepeat() {
        return action == InputConstants.REPEAT;
    }
}
