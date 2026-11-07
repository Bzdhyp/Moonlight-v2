package wtf.moonlight.event.impl.player;

import wtf.moonlight.event.Event;

public final class UseItemEvent extends Event {
    private float yaw;
    private float pitch;

    public UseItemEvent(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public float getYaw() {
        return yaw;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }
}
