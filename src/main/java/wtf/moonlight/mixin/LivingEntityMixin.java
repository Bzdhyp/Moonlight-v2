package wtf.moonlight.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import wtf.moonlight.Moonlight;
import wtf.moonlight.event.impl.player.FallFlyingEvent;
import wtf.moonlight.event.impl.player.JumpEvent;
import wtf.moonlight.event.impl.render.RotationAnimationEvent;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @WrapOperation(method = "jumpFromGround", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getYRot()F"))
    private float moonlight$jumpYaw(LivingEntity self, Operation<Float> original) {
        float yaw = original.call(self);
        if (self != Minecraft.getInstance().player || !Moonlight.EVENT_BUS.hasListeners(JumpEvent.class)) return yaw;
        return Moonlight.EVENT_BUS.post(new JumpEvent(yaw)).getYaw();
    }

    @WrapOperation(method = "tickHeadTurn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getYRot()F"))
    private float moonlight$headTurnYaw(LivingEntity self, Operation<Float> original) {
        float yaw = original.call(self);
        if (self != Minecraft.getInstance().player || !Moonlight.EVENT_BUS.hasListeners(RotationAnimationEvent.class)) return yaw;
        return Moonlight.EVENT_BUS.post(new RotationAnimationEvent(yaw, 0.0F, 0.0F, 0.0F)).getYaw();
    }

    @WrapOperation(method = "updateFallFlyingMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getXRot()F"))
    private float moonlight$fallFlyingPitch(LivingEntity self, Operation<Float> original) {
        float pitch = original.call(self);
        if (self != Minecraft.getInstance().player || !Moonlight.EVENT_BUS.hasListeners(FallFlyingEvent.class)) return pitch;
        return Moonlight.EVENT_BUS.post(new FallFlyingEvent(pitch)).getPitch();
    }
}
