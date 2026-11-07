package wtf.moonlight.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wtf.moonlight.Moonlight;
import wtf.moonlight.event.impl.player.AttackEvent;
import wtf.moonlight.event.impl.player.UseItemEvent;

@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeMixin {
    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void moonlight$attack(CallbackInfo ci) {
        if (!Moonlight.EVENT_BUS.hasListeners(AttackEvent.class)) return;
        if (Moonlight.EVENT_BUS.post(new AttackEvent()).isCancelled()) {
            ci.cancel();
        }
    }

    @ModifyExpressionValue(
            method = "lambda$useItem$0",
            at = @At(value = "NEW", target = "(Lnet/minecraft/world/InteractionHand;IFF)Lnet/minecraft/network/protocol/game/ServerboundUseItemPacket;")
    )
    private ServerboundUseItemPacket moonlight$useItem(ServerboundUseItemPacket packet) {
        if (!Moonlight.EVENT_BUS.hasListeners(UseItemEvent.class)) return packet;
        UseItemEvent event = Moonlight.EVENT_BUS.post(new UseItemEvent(packet.yRot(), packet.xRot()));
        return new ServerboundUseItemPacket(packet.hand(), packet.sequence(), event.getYaw(), event.getPitch());
    }
}
