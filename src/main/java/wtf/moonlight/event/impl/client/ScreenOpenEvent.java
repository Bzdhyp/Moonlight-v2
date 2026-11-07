package wtf.moonlight.event.impl.client;

import net.minecraft.client.gui.screens.Screen;
import org.jspecify.annotations.Nullable;
import wtf.moonlight.event.CancellableEvent;

public final class ScreenOpenEvent extends CancellableEvent {
    private final @Nullable Screen screen;

    public ScreenOpenEvent(@Nullable Screen screen) {
        this.screen = screen;
    }

    public @Nullable Screen getScreen() {
        return screen;
    }
}
