package wtf.moonlight.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import wtf.moonlight.MoonlightClient;
import wtf.moonlight.rotation.Rotation;

import java.util.List;
import java.util.Optional;

public final class RaycastUtils {
    private static final double REACH = 4.5;

    private RaycastUtils() {
    }

    public static @Nullable HitResult rayCast(Rotation rotation, double range) {
        return rayCast(rotation, range, 0);
    }

    public static boolean inView(Entity entity) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) return false;
        double renderDistance = minecraft.options.renderDistance().get() * 16.0;
        Rotation rotation = RotationUtils.calculate(entity);
        if (Math.abs(Mth.wrapDegrees(player.getYRot() - rotation.yaw())) > minecraft.options.fov().get()) return false;
        if (player.distanceTo(entity) > 100 || !(entity instanceof Player)) {
            HitResult result = rayCast(rotation, renderDistance, 0.2F);
            return result != null && result.getType() == HitResult.Type.ENTITY;
        }
        AABB box = entity.getBoundingBox();
        Vec3 base = entity.position();
        for (double y = 1; y >= -1; y -= 0.5) {
            for (double x = 1; x >= -1; x -= 1.0) {
                for (double z = 1; z >= -1; z -= 1.0) {
                    Vec3 point = base.add(box.getXsize() * x, box.getYsize() * y, box.getZsize() * z);
                    HitResult result = rayCast(RotationUtils.calculate(point), renderDistance, 0.2F);
                    if (result != null && result.getType() == HitResult.Type.ENTITY) return true;
                }
            }
        }
        return false;
    }

    public static @Nullable HitResult rayCast(Rotation rotation, double range, float expand) {
        return rayCast(rotation, range, expand, Minecraft.getInstance().player);
    }

    public static @Nullable HitResult rayCast(Rotation rotation, double range, float expand, @Nullable Entity entity) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || entity == null) return null;
        float partialTicks = minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        Vec3 eye = entity.getEyePosition(partialTicks);
        Vec3 look = Vec3.directionFromRotation(rotation.pitch(), rotation.yaw());
        Vec3 end = eye.add(look.scale(range));
        HitResult blockHit = level.clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, entity));
        double blockDistance = blockHit.getType() != HitResult.Type.MISS ? blockHit.getLocation().distanceTo(eye) : range;
        Entity pointed = null;
        Vec3 hit = eye;
        double current = blockDistance;
        AABB search = entity.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0);
        List<Entity> candidates = level.getEntities(entity, search, e -> !e.isSpectator() && e.isPickable());
        for (Entity candidate : candidates) {
            AABB box = candidate.getBoundingBox().inflate(candidate.getPickRadius() + expand);
            Optional<Vec3> intercept = box.clip(eye, end);
            if (box.contains(eye)) {
                if (current >= 0.0) {
                    pointed = candidate;
                    hit = intercept.orElse(eye);
                    current = 0.0;
                }
            } else if (intercept.isPresent()) {
                Vec3 point = intercept.get();
                double distance = eye.distanceTo(point);
                if (distance < current || current == 0.0) {
                    if (candidate.getRootVehicle() == entity.getRootVehicle()) {
                        if (current == 0.0) {
                            pointed = candidate;
                            hit = point;
                        }
                    } else {
                        pointed = candidate;
                        hit = point;
                        current = distance;
                    }
                }
            }
        }
        if (pointed != null && (current < blockDistance || blockHit.getType() == HitResult.Type.MISS)) {
            return new EntityHitResult(pointed, hit);
        }
        return blockHit;
    }

    public static boolean overBlock(Rotation rotation, Direction direction, BlockPos pos, boolean strict) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;
        if (player == null || level == null) return false;
        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 end = eye.add(Vec3.directionFromRotation(rotation.pitch(), rotation.yaw()).scale(REACH));
        BlockHitResult result = level.clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        if (result.getType() == HitResult.Type.MISS) return false;
        return result.getBlockPos().equals(pos) && (!strict || result.getDirection() == direction);
    }

    public static boolean overBlock(Direction direction, BlockPos pos, boolean strict) {
        return overBlock(MoonlightClient.getRotations().getRotation(), direction, pos, strict);
    }

    public static boolean overBlock(Rotation rotation, BlockPos pos) {
        return overBlock(rotation, Direction.UP, pos, false);
    }

    public static boolean overBlock(Rotation rotation, BlockPos pos, Direction direction) {
        return overBlock(rotation, direction, pos, true);
    }
}
