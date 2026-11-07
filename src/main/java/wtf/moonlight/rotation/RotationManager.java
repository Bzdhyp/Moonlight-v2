package wtf.moonlight.rotation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;
import wtf.moonlight.event.Priority;
import wtf.moonlight.event.Subscribe;
import wtf.moonlight.event.impl.player.AttackYawEvent;
import wtf.moonlight.event.impl.player.FallFlyingEvent;
import wtf.moonlight.event.impl.player.ItemRaycastEvent;
import wtf.moonlight.event.impl.player.JumpEvent;
import wtf.moonlight.event.impl.player.MotionEvent;
import wtf.moonlight.event.impl.player.MoveInputEvent;
import wtf.moonlight.event.impl.player.PlayerTickEvent;
import wtf.moonlight.event.impl.player.RaycastEvent;
import wtf.moonlight.event.impl.player.RespawnEvent;
import wtf.moonlight.event.impl.player.StrafeEvent;
import wtf.moonlight.event.impl.player.UseItemEvent;
import wtf.moonlight.event.impl.render.RotationAnimationEvent;
import wtf.moonlight.mixin.MinecraftAccessor;
import wtf.moonlight.utils.MoveUtils;
import wtf.moonlight.utils.RotationUtils;

import java.util.Objects;
import java.util.function.Predicate;

public final class RotationManager {
    private @Nullable Rotation rotation;
    private @Nullable Rotation lastRotation;
    private @Nullable Rotation targetRotation;
    private @Nullable Rotation animation;
    private @Nullable Rotation lastAnimation;
    private @Nullable Predicate<Rotation> raycast;
    private float offsetYaw;
    private float offsetPitch;
    private float randomAngle;
    private double speed;
    private boolean active;
    private boolean smoothed;

    public void setRotations(Rotation rotation, double speed) {
        setRotations(rotation, speed, null);
    }

    public void setRotations(Rotation rotation, double speed, @Nullable Predicate<Rotation> raycast) {
        targetRotation = new Rotation(rotation.yaw(), clampPitch(rotation.pitch()));
        this.speed = speed;
        this.raycast = raycast;
        active = true;
        smooth();
    }

    public void smooth() {
        if (!smoothed) {
            Rotation target = targetRotation;
            Rotation last = lastRotation;
            if (target == null || last == null) return;
            float yaw = target.yaw();
            float pitch = clampPitch(target.pitch());
            Rotation current = rotation;
            LocalPlayer player = Minecraft.getInstance().player;
            Predicate<Rotation> check = raycast;
            if (check != null && current != null && player != null
                    && (Math.abs(yaw - current.yaw()) > 5 || Math.abs(pitch - current.pitch()) > 5)) {
                double step = Math.random() * Math.random() * Math.random() * 20;
                randomAngle += (float) ((20 + (float) (Math.random() - 0.5) * (Math.random() * Math.random() * Math.random() * 360))
                        * (player.tickCount / 10 % 2 == 0 ? -1 : 1));
                offsetYaw = (float) (offsetYaw - Mth.sin(Math.toRadians(randomAngle)) * step);
                offsetPitch = (float) (offsetPitch + Mth.cos(Math.toRadians(randomAngle)) * step);
                yaw += offsetYaw;
                pitch = clampPitch(pitch + offsetPitch);
                if (!check.test(new Rotation(yaw, pitch))) {
                    randomAngle = (float) Math.toDegrees(Math.atan2(target.yaw() - yaw, pitch - target.pitch())) - 180;
                    yaw -= offsetYaw;
                    pitch -= offsetPitch;
                    offsetYaw = (float) (offsetYaw - Mth.sin(Math.toRadians(randomAngle)) * step);
                    offsetPitch = (float) (offsetPitch + Mth.cos(Math.toRadians(randomAngle)) * step);
                    yaw += offsetYaw;
                    pitch = clampPitch(pitch + offsetPitch);
                }
                if (!check.test(new Rotation(yaw, pitch))) {
                    offsetYaw = 0;
                    offsetPitch = 0;
                    yaw = (float) (target.yaw() + Math.random() * 2);
                    pitch = clampPitch((float) (target.pitch() + Math.random() * 2));
                }
            }
            rotation = RotationUtils.smooth(last, new Rotation(yaw, pitch), speed + Math.random());
        }
        smoothed = true;
        ((MinecraftAccessor) Minecraft.getInstance()).moonlight$pick(1.0F);
    }

    public Rotation getRotation() {
        Rotation current = rotation;
        if (active && current != null) return current;
        LocalPlayer player = Objects.requireNonNull(Minecraft.getInstance().player);
        return new Rotation(player.getYRot(), player.getXRot());
    }

    public void setRotation(Rotation rotation) {
        this.rotation = rotation;
    }

    public @Nullable Rotation getLastRotation() {
        return lastRotation;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isSmoothed() {
        return smoothed;
    }

    public void setSmoothed(boolean smoothed) {
        this.smoothed = smoothed;
    }

    @Subscribe
    private void onRespawn(RespawnEvent event) {
        lastRotation = null;
        rotation = null;
    }

    @Subscribe(priority = Priority.LOW)
    private void onTick(PlayerTickEvent event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        if (!active || rotation == null || lastRotation == null || targetRotation == null) {
            Rotation current = new Rotation(player.getYRot(), player.getXRot());
            rotation = current;
            lastRotation = current;
            targetRotation = current;
        }
        if (active) smooth();
    }

    @Subscribe
    private void onAnimation(RotationAnimationEvent event) {
        Rotation current = animation;
        Rotation last = lastAnimation;
        if (!active || current == null || last == null) return;
        event.setYaw(current.yaw());
        event.setLastYaw(last.yaw());
        event.setPitch(current.pitch());
        event.setLastPitch(last.pitch());
    }

    @Subscribe(priority = Priority.LOWEST)
    private void onMotion(MotionEvent event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        Rotation current = rotation;
        if (active && current != null) {
            float yaw = current.yaw();
            float pitch = clampPitch(current.pitch());
            if (!Float.isNaN(yaw) && !Float.isNaN(pitch)) {
                event.setYaw(yaw);
                event.setPitch(pitch);
            }
            lastRotation = current;
            if (Math.abs(Mth.wrapDegrees(current.yaw() - player.getYRot())) < 1 && Math.abs(current.pitch() - player.getXRot()) < 1) {
                active = false;
                rebaseYaw(player, event.getYaw());
            }
        } else {
            lastRotation = new Rotation(player.getYRot(), player.getXRot());
        }
        lastAnimation = animation;
        animation = new Rotation(event.getYaw(), event.getPitch());
        targetRotation = new Rotation(player.getYRot(), player.getXRot());
        raycast = null;
        smoothed = false;
    }

    @Subscribe(priority = Priority.HIGH)
    private void onMoveInput(MoveInputEvent event) {
        Rotation current = rotation;
        if (active && current != null) MoveUtils.fixMovement(event, current.yaw());
    }

    @Subscribe
    private void onRaycast(RaycastEvent event) {
        Rotation current = rotation;
        if (!active || current == null) return;
        event.setYaw(current.yaw());
        event.setPitch(current.pitch());
    }

    @Subscribe
    private void onItemRaycast(ItemRaycastEvent event) {
        Rotation current = rotation;
        if (!active || current == null) return;
        event.setYaw(current.yaw());
        event.setPitch(current.pitch());
    }

    @Subscribe
    private void onStrafe(StrafeEvent event) {
        Rotation current = rotation;
        if (active && current != null) event.setYaw(current.yaw());
    }

    @Subscribe
    private void onJump(JumpEvent event) {
        Rotation current = rotation;
        if (active && current != null) event.setYaw(current.yaw());
    }

    @Subscribe(priority = Priority.HIGHEST)
    private void onUseItem(UseItemEvent event) {
        Rotation current = rotation;
        if (!active || current == null) return;
        event.setYaw(current.yaw());
        event.setPitch(clampPitch(current.pitch()));
    }

    @Subscribe
    private void onFallFlying(FallFlyingEvent event) {
        Rotation current = rotation;
        if (current != null) event.setPitch(current.pitch());
    }

    @Subscribe
    private void onAttackYaw(AttackYawEvent event) {
        Rotation current = rotation;
        if (current != null) event.setYaw(current.yaw());
    }

    private static void rebaseYaw(LocalPlayer player, float sentYaw) {
        float shift = (float) (Math.round((sentYaw - player.getYRot()) / 360.0) * 360.0);
        if (shift == 0) return;
        player.setYRot(player.getYRot() + shift);
        player.yRotO += shift;
        player.yBob += shift;
        player.yBobO += shift;
    }

    private static float clampPitch(float pitch) {
        return Mth.clamp(pitch, -90.0F, 90.0F);
    }
}
