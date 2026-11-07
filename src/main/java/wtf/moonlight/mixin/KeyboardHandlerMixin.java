package wtf.moonlight.mixin;

import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wtf.moonlight.Moonlight;
import wtf.moonlight.event.impl.input.CharInputEvent;
import wtf.moonlight.event.impl.input.KeyInputEvent;

@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerMixin {
    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void moonlight$keyPress(long window, int action, KeyEvent key, CallbackInfo ci) {
        if (!Moonlight.EVENT_BUS.hasListeners(KeyInputEvent.class)) return;
        if (Moonlight.EVENT_BUS.post(new KeyInputEvent(key, action)).isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
    private void moonlight$charTyped(long window, CharacterEvent event, CallbackInfo ci) {
        if (!Moonlight.EVENT_BUS.hasListeners(CharInputEvent.class)) return;
        if (Moonlight.EVENT_BUS.post(new CharInputEvent(event.codepoint())).isCancelled()) {
            ci.cancel();
        }
    }
}
