package wtf.moonlight.event.impl.player;

import wtf.moonlight.event.Event;

public final class JumpEvent extends Event {
    private float yaw;

    public JumpEvent(float yaw) {
        this.yaw = yaw;
    }

    public float getYaw() {
        return yaw;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }
}
