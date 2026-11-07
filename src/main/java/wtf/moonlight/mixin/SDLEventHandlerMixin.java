package wtf.moonlight.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.platform.SDLEventHandler;
import org.lwjgl.sdl.SDL_KeyboardEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import wtf.moonlight.utils.KeyUtils;

@Mixin(SDLEventHandler.class)
public abstract class SDLEventHandlerMixin {
    @ModifyExpressionValue(method = "handleKeyEvent", at = @At(value = "INVOKE", target = "Lorg/lwjgl/sdl/SDL_KeyboardEvent;scancode()I"))
    private int moonlight$scancode(int scancode, @Local SDL_KeyboardEvent event) {
        return KeyUtils.scancode(scancode, event.key(), event.raw() & 0xFFFF);
    }
}
