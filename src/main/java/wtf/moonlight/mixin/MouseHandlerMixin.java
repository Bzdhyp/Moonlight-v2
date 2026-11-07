package wtf.moonlight.mixin;

import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wtf.moonlight.Moonlight;
import wtf.moonlight.event.impl.input.MouseInputEvent;
import wtf.moonlight.event.impl.input.MouseScrollEvent;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    @Inject(method = "onButton", at = @At("HEAD"), cancellable = true)
    private void moonlight$onButton(long window, MouseButtonInfo info, int action, CallbackInfo ci) {
        if (!Moonlight.EVENT_BUS.hasListeners(MouseInputEvent.class)) return;
        if (Moonlight.EVENT_BUS.post(new MouseInputEvent(info.button(), action)).isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void moonlight$onScroll(CallbackInfo ci) {
        if (!Moonlight.EVENT_BUS.hasListeners(MouseScrollEvent.class)) return;
        if (Moonlight.EVENT_BUS.post(new MouseScrollEvent()).isCancelled()) {
            ci.cancel();
        }
    }
}
