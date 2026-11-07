package wtf.moonlight.mixin;

import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wtf.moonlight.Moonlight;
import wtf.moonlight.event.impl.input.KeyInputEvent;

@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerMixin {
    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void moonlight$keyPress(long window, int action, KeyEvent key, CallbackInfo ci) {
        if (!Moonlight.EVENT_BUS.hasListeners(KeyInputEvent.class)) return;
        if (Moonlight.EVENT_BUS.post(new KeyInputEvent(key.key(), action)).isCancelled()) {
            ci.cancel();
        }
    }
}
