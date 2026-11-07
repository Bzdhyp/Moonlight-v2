package wtf.moonlight.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import wtf.moonlight.Moonlight;
import wtf.moonlight.event.impl.render.RotationAnimationEvent;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
    @WrapOperation(
            method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;rotLerp(FFF)F")
    )
    private float moonlight$headYaw(float partialTicks, float start, float end, Operation<Float> original, @Local(argsOnly = true) LivingEntity entity) {
        if (entity != Minecraft.getInstance().player || !Moonlight.EVENT_BUS.hasListeners(RotationAnimationEvent.class)) {
            return original.call(partialTicks, start, end);
        }
        RotationAnimationEvent event = Moonlight.EVENT_BUS.post(new RotationAnimationEvent(end, start, 0.0F, 0.0F));
        return original.call(partialTicks, event.getLastYaw(), event.getYaw());
    }

    @WrapOperation(
            method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getXRot(F)F")
    )
    private float moonlight$headPitch(LivingEntity entity, float partialTicks, Operation<Float> original) {
        if (entity != Minecraft.getInstance().player || !Moonlight.EVENT_BUS.hasListeners(RotationAnimationEvent.class)) {
            return original.call(entity, partialTicks);
        }
        RotationAnimationEvent event = Moonlight.EVENT_BUS.post(new RotationAnimationEvent(0.0F, 0.0F, entity.getXRot(), entity.xRotO));
        return Mth.lerp(partialTicks, event.getLastPitch(), event.getPitch());
    }
}
