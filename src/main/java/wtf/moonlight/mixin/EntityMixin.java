package wtf.moonlight.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import wtf.moonlight.Moonlight;
import wtf.moonlight.event.impl.player.RaycastEvent;
import wtf.moonlight.event.impl.player.StrafeEvent;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @ModifyArg(
            method = "moveRelative",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getInputVector(Lnet/minecraft/world/phys/Vec3;FF)Lnet/minecraft/world/phys/Vec3;"),
            index = 2
    )
    private float moonlight$strafeYaw(float yaw) {
        if ((Object) this != Minecraft.getInstance().player || !Moonlight.EVENT_BUS.hasListeners(StrafeEvent.class)) return yaw;
        return Moonlight.EVENT_BUS.post(new StrafeEvent(yaw)).getYaw();
    }

    @WrapOperation(
            method = "getViewVector",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;calculateViewVector(FF)Lnet/minecraft/world/phys/Vec3;")
    )
    private Vec3 moonlight$raycast(float pitch, float yaw, Operation<Vec3> original) {
        if ((Object) this != Minecraft.getInstance().player || !Moonlight.EVENT_BUS.hasListeners(RaycastEvent.class)) return original.call(pitch, yaw);
        RaycastEvent event = Moonlight.EVENT_BUS.post(new RaycastEvent(yaw, pitch));
        return original.call(event.getPitch(), event.getYaw());
    }
}
