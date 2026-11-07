package wtf.moonlight.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import wtf.moonlight.Moonlight;
import wtf.moonlight.event.impl.player.ItemRaycastEvent;

@Mixin(Item.class)
public abstract class ItemMixin {
    @WrapOperation(
            method = "getPlayerPOVHitResult",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;calculateViewVector(FF)Lnet/minecraft/world/phys/Vec3;")
    )
    private static Vec3 moonlight$itemRaycast(float pitch, float yaw, Operation<Vec3> original, @Local(argsOnly = true) Player player) {
        if (player != Minecraft.getInstance().player || !Moonlight.EVENT_BUS.hasListeners(ItemRaycastEvent.class)) return original.call(pitch, yaw);
        ItemRaycastEvent event = Moonlight.EVENT_BUS.post(new ItemRaycastEvent(yaw, pitch));
        return original.call(event.getPitch(), event.getYaw());
    }
}
