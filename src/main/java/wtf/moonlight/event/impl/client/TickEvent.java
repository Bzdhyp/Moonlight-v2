package wtf.moonlight.event.impl.client;

import wtf.moonlight.event.Event;
import wtf.moonlight.event.EventPhase;

public final class TickEvent extends Event {
    public static final TickEvent PRE = new TickEvent(EventPhase.PRE);
    public static final TickEvent POST = new TickEvent(EventPhase.POST);

    private final EventPhase phase;

    private TickEvent(EventPhase phase) {
        this.phase = phase;
    }

    public EventPhase getPhase() {
        return phase;
    }
}
