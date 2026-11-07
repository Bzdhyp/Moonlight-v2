package wtf.moonlight.mixin;

import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wtf.moonlight.Moonlight;
import wtf.moonlight.event.impl.render.Render3DEvent;
import wtf.moonlight.event.impl.render.RenderOverlayEvent;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(
            method = "render3dHud",
            at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V", args = "ldc=hand")
    )
    private void moonlight$render3D(CallbackInfo ci) {
        Moonlight.EVENT_BUS.post(Render3DEvent.INSTANCE);
    }

    @Inject(
            method = "render()V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiRenderer;render()V", shift = At.Shift.AFTER)
    )
    private void moonlight$renderOverlay(CallbackInfo ci) {
        Moonlight.EVENT_BUS.post(RenderOverlayEvent.INSTANCE);
    }
}
