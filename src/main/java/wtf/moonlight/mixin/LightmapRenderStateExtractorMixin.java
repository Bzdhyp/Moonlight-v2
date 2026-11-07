package wtf.moonlight.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;
import wtf.moonlight.Moonlight;
import wtf.moonlight.event.impl.render.GammaEvent;

@Mixin(LightmapRenderStateExtractor.class)
public abstract class LightmapRenderStateExtractorMixin {
    @ModifyExpressionValue(
            method = "extract",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/OptionInstance;get()Ljava/lang/Object;", ordinal = 0),
            slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/client/Options;gamma()Lnet/minecraft/client/OptionInstance;"))
    )
    private Object moonlight$gamma(Object original) {
        if (!Moonlight.EVENT_BUS.hasListeners(GammaEvent.class)) return original;
        return Moonlight.EVENT_BUS.post(new GammaEvent((Double) original)).getGamma();
    }
}
