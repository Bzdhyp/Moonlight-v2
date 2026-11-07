package wtf.moonlight.event.impl.input;

import wtf.moonlight.event.Event;

public final class MouseMoveEvent extends Event {
    public static final MouseMoveEvent INSTANCE = new MouseMoveEvent();

    private MouseMoveEvent() {
    }
}
