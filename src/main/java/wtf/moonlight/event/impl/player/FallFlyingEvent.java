package wtf.moonlight.event.impl.player;

import wtf.moonlight.event.Event;

public final class FallFlyingEvent extends Event {
    private float pitch;

    public FallFlyingEvent(float pitch) {
        this.pitch = pitch;
    }

    public float getPitch() {
        return pitch;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }
}
