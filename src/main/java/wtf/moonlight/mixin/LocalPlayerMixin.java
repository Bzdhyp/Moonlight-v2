package wtf.moonlight.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wtf.moonlight.Moonlight;
import wtf.moonlight.event.impl.player.MotionEvent;
import wtf.moonlight.event.impl.player.PlayerTickEvent;
import wtf.moonlight.event.impl.player.RespawnEvent;
import wtf.moonlight.event.impl.player.SlowdownEvent;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {
    @Unique
    private @Nullable MotionEvent moonlight$motion;

    @Inject(method = "tick", at = @At("HEAD"))
    private void moonlight$tick(CallbackInfo ci) {
        Moonlight.EVENT_BUS.post(PlayerTickEvent.INSTANCE);
    }

    @Inject(method = "sendPosition", at = @At("HEAD"), cancellable = true)
    private void moonlight$motion(CallbackInfo ci) {
        moonlight$motion = null;
        LocalPlayer self = (LocalPlayer) (Object) this;
        MotionEvent event = Moonlight.EVENT_BUS.hasListeners(MotionEvent.class)
                ? Moonlight.EVENT_BUS.post(new MotionEvent(self.getX(), self.getY(), self.getZ(), self.getYRot(), self.getXRot(), self.onGround()))
                : null;
        if (self.tickCount <= 1) Moonlight.EVENT_BUS.post(RespawnEvent.INSTANCE);
        if (event == null) return;
        if (event.isCancelled()) {
            ci.cancel();
            return;
        }
        moonlight$motion = event;
    }

    @WrapOperation(method = "sendPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;position()Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 moonlight$motionPosition(LocalPlayer self, Operation<Vec3> original) {
        MotionEvent motion = moonlight$motion;
        return motion != null ? motion.getPosition() : original.call(self);
    }

    @WrapOperation(method = "sendPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getX()D"))
    private double moonlight$motionX(LocalPlayer self, Operation<Double> original) {
        MotionEvent motion = moonlight$motion;
        return motion != null ? motion.getX() : original.call(self);
    }

    @WrapOperation(method = "sendPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getY()D"))
    private double moonlight$motionY(LocalPlayer self, Operation<Double> original) {
        MotionEvent motion = moonlight$motion;
        return motion != null ? motion.getY() : original.call(self);
    }

    @WrapOperation(method = "sendPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getZ()D"))
    private double moonlight$motionZ(LocalPlayer self, Operation<Double> original) {
        MotionEvent motion = moonlight$motion;
        return motion != null ? motion.getZ() : original.call(self);
    }

    @WrapOperation(method = "sendPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getYRot()F"))
    private float moonlight$motionYaw(LocalPlayer self, Operation<Float> original) {
        MotionEvent motion = moonlight$motion;
        return motion != null ? motion.getYaw() : original.call(self);
    }

    @WrapOperation(method = "sendPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getXRot()F"))
    private float moonlight$motionPitch(LocalPlayer self, Operation<Float> original) {
        MotionEvent motion = moonlight$motion;
        return motion != null ? motion.getPitch() : original.call(self);
    }

    @WrapOperation(method = "sendPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;onGround()Z"))
    private boolean moonlight$motionOnGround(LocalPlayer self, Operation<Boolean> original) {
        MotionEvent motion = moonlight$motion;
        return motion != null ? motion.isOnGround() : original.call(self);
    }

    @WrapOperation(method = "modifyInput", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isUsingItem()Z"))
    private boolean moonlight$slowdown(LocalPlayer self, Operation<Boolean> original) {
        boolean usingItem = original.call(self);
        if (!usingItem || !Moonlight.EVENT_BUS.hasListeners(SlowdownEvent.class)) return usingItem;
        return !Moonlight.EVENT_BUS.post(new SlowdownEvent()).isCancelled();
    }
}
