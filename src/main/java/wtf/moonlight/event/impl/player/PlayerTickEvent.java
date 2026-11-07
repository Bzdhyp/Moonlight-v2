package wtf.moonlight.event.impl.player;

import wtf.moonlight.event.Event;

public final class PlayerTickEvent extends Event {
    public static final PlayerTickEvent INSTANCE = new PlayerTickEvent();

    private PlayerTickEvent() {
    }
}
