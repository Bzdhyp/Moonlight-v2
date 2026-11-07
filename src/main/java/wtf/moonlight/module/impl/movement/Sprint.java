package wtf.moonlight.module.impl.movement;

import net.minecraft.world.entity.player.Input;
import wtf.moonlight.event.Subscribe;
import wtf.moonlight.event.impl.player.MoveInputEvent;
import wtf.moonlight.module.Category;
import wtf.moonlight.module.Module;

public final class Sprint extends Module {
    public Sprint() {
        super("Sprint", Category.MOVEMENT);
    }

    @Subscribe
    private void onMoveInput(MoveInputEvent event) {
        Input input = event.getInput();
        event.setInput(new Input(input.forward(), input.backward(), input.left(), input.right(), input.jump(), input.shift(), true));
    }
}
