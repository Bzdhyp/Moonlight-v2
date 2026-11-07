package wtf.moonlight.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import wtf.moonlight.Moonlight;
import wtf.moonlight.event.impl.client.ScreenOpenEvent;

@Mixin(Gui.class)
public abstract class GuiMixin {
    @WrapMethod(method = "setScreen")
    private void moonlight$setScreen(@Nullable Screen screen, Operation<Void> original) {
        if (!Moonlight.EVENT_BUS.hasListeners(ScreenOpenEvent.class)) {
            original.call(screen);
            return;
        }
        ScreenOpenEvent event = Moonlight.EVENT_BUS.post(new ScreenOpenEvent(screen));
        if (!event.isCancelled()) {
            original.call(event.getScreen());
        }
    }
}
