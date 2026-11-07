package wtf.moonlight.mixin;

import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wtf.moonlight.Moonlight;
import wtf.moonlight.event.impl.render.Render2DEvent;

@Mixin(Hud.class)
public abstract class HudMixin {
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void moonlight$render2D(CallbackInfo ci) {
        Moonlight.EVENT_BUS.post(Render2DEvent.INSTANCE);
    }
}
