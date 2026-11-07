package wtf.moonlight.event.impl.player;

import net.minecraft.world.entity.player.Input;
import wtf.moonlight.event.Event;

public final class MoveInputEvent extends Event {
    private Input input;

    public MoveInputEvent(Input input) {
        this.input = input;
    }

    public Input getInput() {
        return input;
    }

    public void setInput(Input input) {
        this.input = input;
    }
}
