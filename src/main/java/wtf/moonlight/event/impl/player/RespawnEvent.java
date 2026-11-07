package wtf.moonlight.event.impl.player;

import wtf.moonlight.event.Event;

public final class RespawnEvent extends Event {
    public static final RespawnEvent INSTANCE = new RespawnEvent();

    private RespawnEvent() {
    }
}
