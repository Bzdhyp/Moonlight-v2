package wtf.moonlight.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import wtf.moonlight.Moonlight;
import wtf.moonlight.event.impl.player.MoveInputEvent;

@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin {
    @ModifyExpressionValue(method = "tick", at = @At(value = "NEW", target = "(ZZZZZZZ)Lnet/minecraft/world/entity/player/Input;"))
    private Input moonlight$moveInput(Input original) {
        if (!Moonlight.EVENT_BUS.hasListeners(MoveInputEvent.class)) return original;
        return Moonlight.EVENT_BUS.post(new MoveInputEvent(original)).getInput();
    }
}
