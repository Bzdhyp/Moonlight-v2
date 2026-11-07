package wtf.moonlight.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import wtf.moonlight.MoonlightClient;
import wtf.moonlight.rotation.Rotation;

import java.util.Objects;

public final class RotationUtils {
    private RotationUtils() {
    }

    public static Rotation calculate(Vec3 from, Vec3 to) {
        Vec3 diff = to.subtract(from);
        double distance = Math.hypot(diff.x, diff.z);
        float yaw = (float) (Mth.atan2(diff.z, diff.x) * Mth.RAD_TO_DEG) - 90.0F;
        float pitch = (float) -(Mth.atan2(diff.y, distance) * Mth.RAD_TO_DEG);
        return new Rotation(yaw, pitch);
    }

    public static Rotation calculate(Entity entity) {
        LocalPlayer player = player();
        AABB box = entity.getBoundingBox();
        double height = Math.max(0, Math.min(player.getY() - entity.getY() + player.getEyeHeight(), (box.maxY - box.minY) * 0.9));
        return calculate(entity.position().add(0, height, 0));
    }

    public static Rotation calculate(Entity entity, boolean adaptive, double range) {
        Rotation normal = calculate(entity);
        HitResult result = RaycastUtils.rayCast(normal, range, 0.0F);
        if (!adaptive || result != null && result.getType() == HitResult.Type.ENTITY) return normal;
        AABB box = entity.getBoundingBox();
        Vec3 base = entity.position();
        for (double y = 1; y >= 0; y -= 0.25 + Math.random() * 0.1) {
            for (double x = 1; x >= -0.5; x -= 0.5) {
                for (double z = 1; z >= -0.5; z -= 0.5) {
                    Vec3 point = base.add((box.maxX - box.minX) * x, (box.maxY - box.minY) * y, (box.maxZ - box.minZ) * z);
                    Rotation rotation = calculate(point);
                    HitResult hit = RaycastUtils.rayCast(rotation, range, 0.0F);
                    if (hit != null && hit.getType() == HitResult.Type.ENTITY) return rotation;
                }
            }
        }
        return normal;
    }

    public static Rotation calculate(BlockPos to) {
        return calculate(Vec3.atCenterOf(to));
    }

    public static Rotation calculate(Vec3 to) {
        return calculate(player().getEyePosition(), to);
    }

    public static Rotation calculate(Vec3 position, Direction direction) {
        return calculate(position.add(0.5 + direction.getStepX() * 0.5, 0.5 + direction.getStepY() * 0.5, 0.5 + direction.getStepZ() * 0.5));
    }

    public static Rotation calculate(BlockPos position, Direction direction) {
        return calculate(Vec3.atLowerCornerOf(position), direction);
    }

    public static Rotation applySensitivityPatch(Rotation rotation) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return rotation;
        return applySensitivityPatch(rotation, new Rotation(player.yRotO, player.xRotO));
    }

    public static Rotation applySensitivityPatch(Rotation rotation, Rotation previous) {
        float sensitivity = (float) (Minecraft.getInstance().options.sensitivity().get() * (1 + Math.random() / 10000000) * 0.6F + 0.2F);
        double multiplier = sensitivity * sensitivity * sensitivity * 8.0F * 0.15D;
        float yaw = previous.yaw() + (float) (Math.round((rotation.yaw() - previous.yaw()) / multiplier) * multiplier);
        float pitch = previous.pitch() + (float) (Math.round((rotation.pitch() - previous.pitch()) / multiplier) * multiplier);
        return new Rotation(yaw, Mth.clamp(pitch, -90, 90));
    }

    public static Rotation relateToPlayerRotation(Rotation rotation) {
        LocalPlayer player = player();
        float yaw = player.yRotO + Mth.wrapDegrees(rotation.yaw() - player.yRotO);
        return new Rotation(yaw, Mth.clamp(rotation.pitch(), -90, 90));
    }

    public static Rotation resetRotation(Rotation rotation) {
        LocalPlayer player = player();
        float yaw = rotation.yaw() + Mth.wrapDegrees(player.getYRot() - rotation.yaw());
        return new Rotation(yaw, player.getXRot());
    }

    public static Rotation move(Rotation target, double speed) {
        return move(lastRotation(), target, speed);
    }

    public static Rotation move(Rotation last, Rotation target, double speed) {
        double deltaYaw = Mth.wrapDegrees(target.yaw() - last.yaw());
        double deltaPitch = target.pitch() - last.pitch();
        double distance = Math.sqrt(deltaYaw * deltaYaw + deltaPitch * deltaPitch);
        if (speed == 0 || distance == 0) return new Rotation(0, 0);
        double maxYaw = speed * Math.abs(deltaYaw / distance);
        double maxPitch = speed * Math.abs(deltaPitch / distance);
        float moveYaw = (float) Math.max(Math.min(deltaYaw, maxYaw), -maxYaw);
        float movePitch = (float) Math.max(Math.min(deltaPitch, maxPitch), -maxPitch);
        return new Rotation(moveYaw, movePitch);
    }

    public static Rotation smooth(Rotation target, double speed) {
        return smooth(lastRotation(), target, speed);
    }

    public static Rotation smooth(Rotation last, Rotation target, double speed) {
        if (speed == 0) return target;
        Rotation move = move(last, target, speed);
        float yaw = last.yaw() + move.yaw();
        float pitch = last.pitch() + move.pitch();
        for (int i = 1; i <= (int) (Minecraft.getInstance().getFps() / 20.0F + Math.random() * 10); i++) {
            if (Math.abs(move.yaw()) + Math.abs(move.pitch()) > 0.0001) {
                yaw += (float) ((Math.random() - 0.5) / 1000);
                pitch -= (float) (Math.random() / 200);
            }
            Rotation fixed = applySensitivityPatch(new Rotation(yaw, pitch));
            yaw = fixed.yaw();
            pitch = Math.max(-90, Math.min(90, fixed.pitch()));
        }
        return new Rotation(yaw, pitch);
    }

    public static double getDistance(Entity entity) {
        return getDistance(player(), entity);
    }

    public static double getDistance(Player player, Entity entity) {
        Vec3 eyes = player.getEyePosition();
        AABB box = entity.getBoundingBox();
        Vec3 closest = new Vec3(Mth.clamp(eyes.x, box.minX, box.maxX), Mth.clamp(eyes.y, box.minY, box.maxY), Mth.clamp(eyes.z, box.minZ, box.maxZ));
        return eyes.distanceTo(closest);
    }

    private static LocalPlayer player() {
        return Objects.requireNonNull(Minecraft.getInstance().player);
    }

    private static Rotation lastRotation() {
        return Objects.requireNonNull(MoonlightClient.getRotations().getLastRotation());
    }
}
