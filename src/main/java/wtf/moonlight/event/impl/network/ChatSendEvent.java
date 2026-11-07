package wtf.moonlight.event.impl.network;

import wtf.moonlight.event.CancellableEvent;

public final class ChatSendEvent extends CancellableEvent {
    private final String message;

    public ChatSendEvent(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
