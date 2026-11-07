package wtf.moonlight.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import wtf.moonlight.Moonlight;
import wtf.moonlight.event.impl.player.AttackYawEvent;

@Mixin(Player.class)
public abstract class PlayerMixin {
    @WrapOperation(method = "causeExtraKnockback", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getYRot()F"))
    private float moonlight$attackYaw(Player self, Operation<Float> original) {
        float yaw = original.call(self);
        if (self != Minecraft.getInstance().player || !Moonlight.EVENT_BUS.hasListeners(AttackYawEvent.class)) return yaw;
        return Moonlight.EVENT_BUS.post(new AttackYawEvent(yaw)).getYaw();
    }
}
