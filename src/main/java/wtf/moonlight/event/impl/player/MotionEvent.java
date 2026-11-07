package wtf.moonlight.event.impl.player;

import net.minecraft.world.phys.Vec3;
import wtf.moonlight.event.CancellableEvent;

public final class MotionEvent extends CancellableEvent {
    private final double x;
    private final double y;
    private final double z;
    private float yaw;
    private float pitch;
    private final boolean onGround;

    public MotionEvent(double x, double y, double z, float yaw, float pitch, boolean onGround) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.onGround = onGround;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public Vec3 getPosition() {
        return new Vec3(x, y, z);
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

    public boolean isOnGround() {
        return onGround;
    }
}
