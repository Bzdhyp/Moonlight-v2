package wtf.moonlight.event.impl.client;

import wtf.moonlight.event.Event;

public final class WorldChangeEvent extends Event {
    public static final WorldChangeEvent INSTANCE = new WorldChangeEvent();

    private WorldChangeEvent() {
    }
}
