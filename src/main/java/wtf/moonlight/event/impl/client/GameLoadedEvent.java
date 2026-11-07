package wtf.moonlight.event.impl.client;

import wtf.moonlight.event.Event;

public final class GameLoadedEvent extends Event {
    public static final GameLoadedEvent INSTANCE = new GameLoadedEvent();

    private GameLoadedEvent() {
    }
}
