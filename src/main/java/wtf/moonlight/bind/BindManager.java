package wtf.moonlight.bind;

import org.jspecify.annotations.Nullable;
import wtf.moonlight.event.Priority;
import wtf.moonlight.event.Subscribe;
import wtf.moonlight.event.impl.client.TickEvent;
import wtf.moonlight.event.impl.input.KeyInputEvent;
import wtf.moonlight.event.impl.input.MouseInputEvent;
import wtf.moonlight.utils.KeyUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public final class BindManager {
    private final List<Bindable> bindables = new CopyOnWriteArrayList<>();
    private final Map<String, Bind> binds = new LinkedHashMap<>();

    public void register(Bindable bindable) {
        if (bindables.contains(bindable)) return;
        bindables.add(bindable);
        if (bindable instanceof Bind bind) binds.put(key(bind.getName()), bind);
    }

    public @Nullable Bind getBind(String name) {
        return binds.get(key(name));
    }

    public Collection<Bind> getBinds() {
        return Collections.unmodifiableCollection(binds.values());
    }

    @Subscribe(priority = Priority.HIGH)
    private void onKey(KeyInputEvent event) {
        if (event.isCancelled() || event.isRepeat()) return;
        dispatch(event.getKey(), event.isPress());
    }

    @Subscribe(priority = Priority.HIGH)
    private void onMouse(MouseInputEvent event) {
        if (event.isCancelled()) return;
        dispatch(KeyUtils.mouseCode(event.getButton()), event.isPress());
    }

    @Subscribe
    private void onTick(TickEvent event) {
        if (!event.getPhase().isPre()) return;
        for (Bindable bindable : bindables) bindable.reconcile();
    }

    private void dispatch(int code, boolean pressed) {
        if (code == KeyUtils.NONE) return;
        for (Bindable bindable : bindables) bindable.onKey(code, pressed);
    }

    private static String key(String name) {
        StringBuilder builder = new StringBuilder(name.length());
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isLetterOrDigit(c)) builder.append(c);
        }
        return builder.toString().toLowerCase(Locale.ROOT);
    }
}
