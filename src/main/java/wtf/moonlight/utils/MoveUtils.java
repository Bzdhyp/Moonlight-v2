package wtf.moonlight.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Input;
import wtf.moonlight.event.impl.player.MoveInputEvent;

public final class MoveUtils {
    private static final float ANGLE_UNIT = 45.0F;
    private static final float ANGLE_TOLERANCE = 22.5F;

    private MoveUtils() {
    }

    public static boolean isMoving() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null && player.input.getMoveVector().lengthSquared() > 0;
    }

    public static void fixMovement(MoveInputEvent event, float yaw) {
        LocalPlayer player = Minecraft.getInstance().player;
        Input input = event.getInput();
        int forward = impulse(input.forward(), input.backward());
        int strafe = impulse(input.left(), input.right());
        if (player == null || forward == 0 && strafe == 0) return;
        double difference = Mth.wrapDegrees(direction(player.getYRot(), forward, strafe) - yaw);
        double distance = Math.abs(difference);
        int fixedForward = 0;
        int fixedStrafe = 0;
        if (distance <= ANGLE_UNIT + ANGLE_TOLERANCE) {
            fixedForward++;
        } else if (distance >= 180.0F - ANGLE_UNIT - ANGLE_TOLERANCE) {
            fixedForward--;
        }
        if (difference >= ANGLE_UNIT - ANGLE_TOLERANCE && difference <= 180.0F - ANGLE_UNIT + ANGLE_TOLERANCE) {
            fixedStrafe--;
        } else if (difference <= -ANGLE_UNIT + ANGLE_TOLERANCE && difference >= -180.0F + ANGLE_UNIT - ANGLE_TOLERANCE) {
            fixedStrafe++;
        }
        event.setInput(new Input(fixedForward > 0, fixedForward < 0, fixedStrafe > 0, fixedStrafe < 0, input.jump(), input.shift(), input.sprint()));
    }

    public static int impulse(boolean positive, boolean negative) {
        return positive == negative ? 0 : positive ? 1 : -1;
    }

    private static float direction(float yaw, int forward, int strafe) {
        if (forward < 0 && strafe == 0) return yaw + 180.0F;
        if (forward > 0 && strafe < 0) return yaw + 45.0F;
        if (forward > 0 && strafe > 0) return yaw - 45.0F;
        if (forward == 0 && strafe < 0) return yaw + 90.0F;
        if (forward == 0 && strafe > 0) return yaw - 90.0F;
        if (forward < 0 && strafe < 0) return yaw + 135.0F;
        if (forward < 0) return yaw - 135.0F;
        return yaw;
    }
}
