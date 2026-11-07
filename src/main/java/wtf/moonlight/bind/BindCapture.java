package wtf.moonlight.bind;

import com.mojang.blaze3d.platform.InputConstants;
import org.jspecify.annotations.Nullable;
import wtf.moonlight.event.Priority;
import wtf.moonlight.event.Subscribe;
import wtf.moonlight.event.impl.input.KeyInputEvent;
import wtf.moonlight.event.impl.input.MouseInputEvent;
import wtf.moonlight.utils.KeyUtils;

public final class BindCapture {
    private @Nullable Hotkey target;
    private @Nullable Runnable onFinish;
    private @Nullable Runnable onCancel;

    public void begin(Hotkey target, Runnable onFinish, Runnable onCancel) {
        cancel();
        this.target = target;
        this.onFinish = onFinish;
        this.onCancel = onCancel;
    }

    public void cancel() {
        Runnable callback = onCancel;
        clear();
        if (callback != null) callback.run();
    }

    public @Nullable Hotkey getTarget() {
        return target;
    }

    @Subscribe(priority = Priority.HIGHEST)
    private void onKey(KeyInputEvent event) {
        if (target == null || !event.isPress()) return;
        event.cancel();
        finish(event.getKey() == InputConstants.KEY_ESCAPE ? KeyUtils.NONE : event.getKey());
    }

    @Subscribe(priority = Priority.HIGHEST)
    private void onMouse(MouseInputEvent event) {
        if (target == null || !event.isPress()) return;
        event.cancel();
        if (event.getButton() == InputConstants.MOUSE_BUTTON_LEFT) {
            cancel();
            return;
        }
        finish(KeyUtils.mouseCode(event.getButton()));
    }

    private void finish(int code) {
        Hotkey hotkey = target;
        Runnable callback = onFinish;
        clear();
        if (hotkey == null) return;
        hotkey.setKey(code);
        if (callback != null) callback.run();
    }

    private void clear() {
        target = null;
        onFinish = null;
        onCancel = null;
    }
}
