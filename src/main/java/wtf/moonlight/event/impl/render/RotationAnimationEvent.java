package wtf.moonlight.event.impl.render;

import wtf.moonlight.event.Event;

public final class RotationAnimationEvent extends Event {
    private float yaw;
    private float lastYaw;
    private float pitch;
    private float lastPitch;

    public RotationAnimationEvent(float yaw, float lastYaw, float pitch, float lastPitch) {
        this.yaw = yaw;
        this.lastYaw = lastYaw;
        this.pitch = pitch;
        this.lastPitch = lastPitch;
    }

    public float getYaw() {
        return yaw;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public float getLastYaw() {
        return lastYaw;
    }

    public void setLastYaw(float lastYaw) {
        this.lastYaw = lastYaw;
    }

    public float getPitch() {
        return pitch;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    public float getLastPitch() {
        return lastPitch;
    }

    public void setLastPitch(float lastPitch) {
        this.lastPitch = lastPitch;
    }
}
