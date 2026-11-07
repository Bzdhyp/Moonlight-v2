package wtf.moonlight.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import wtf.moonlight.Moonlight;
import wtf.moonlight.event.impl.network.ChatSendEvent;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
    @WrapMethod(method = "sendChat")
    private void moonlight$sendChat(String message, Operation<Void> original) {
        if (!Moonlight.EVENT_BUS.hasListeners(ChatSendEvent.class)) {
            original.call(message);
            return;
        }
        ChatSendEvent event = Moonlight.EVENT_BUS.post(new ChatSendEvent(message));
        if (!event.isCancelled()) {
            original.call(event.getMessage());
        }
    }
}
