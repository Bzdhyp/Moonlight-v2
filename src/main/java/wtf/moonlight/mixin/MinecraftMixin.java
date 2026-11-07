package wtf.moonlight.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wtf.moonlight.Moonlight;
import wtf.moonlight.event.impl.client.GameLoadedEvent;
import wtf.moonlight.event.impl.client.TickEvent;
import wtf.moonlight.event.impl.client.WorldChangeEvent;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Inject(method = "onGameLoadFinished", at = @At("TAIL"))
    private void moonlight$gameLoaded(CallbackInfo ci) {
        Moonlight.EVENT_BUS.post(GameLoadedEvent.INSTANCE);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void moonlight$preTick(CallbackInfo ci) {
        Moonlight.EVENT_BUS.post(TickEvent.PRE);
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void moonlight$postTick(CallbackInfo ci) {
        Moonlight.EVENT_BUS.post(TickEvent.POST);
    }

    @Inject(method = {"setLevel", "clearClientLevel"}, at = @At("TAIL"))
    private void moonlight$worldChange(CallbackInfo ci) {
        Moonlight.EVENT_BUS.post(WorldChangeEvent.INSTANCE);
    }
}
