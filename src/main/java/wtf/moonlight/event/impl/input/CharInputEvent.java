package wtf.moonlight.event.impl.input;

import wtf.moonlight.event.CancellableEvent;

public final class CharInputEvent extends CancellableEvent {
    private final int codepoint;

    public CharInputEvent(int codepoint) {
        this.codepoint = codepoint;
    }

    public int getCodepoint() {
        return codepoint;
    }
}
