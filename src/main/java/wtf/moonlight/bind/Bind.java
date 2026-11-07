package wtf.moonlight.bind;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import wtf.moonlight.utils.KeyUtils;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public final class Bind implements Bindable {
    private final String name;
    private final Consumer<Boolean> listener;
    private final List<Hotkey> hotkeys = new CopyOnWriteArrayList<>();
    private BooleanSupplier gate = () -> Minecraft.getInstance().gui.screen() == null;
    private boolean active;

    public Bind(String name, Consumer<Boolean> listener) {
        this.name = name;
        this.listener = listener;
    }

    public String getName() {
        return name;
    }

    public List<Hotkey> getHotkeys() {
        return Collections.unmodifiableList(hotkeys);
    }

    public Hotkey add(Hotkey hotkey) {
        hotkeys.add(hotkey);
        return hotkey;
    }

    public void remove(Hotkey hotkey) {
        if (hotkey.isEngaged()) setActive(hotkey.release());
        hotkeys.remove(hotkey);
    }

    public void clear() {
        for (Hotkey hotkey : hotkeys) remove(hotkey);
    }

    public Hotkey primary() {
        return hotkeys.isEmpty() ? add(new Hotkey(KeyUtils.NONE)) : hotkeys.getFirst();
    }

    public boolean isBound() {
        return hotkeys.stream().anyMatch(hotkey -> hotkey.getKey() != KeyUtils.NONE);
    }

    public boolean isActive() {
        return active;
    }

    public void sync(boolean active) {
        this.active = active;
    }

    public void setGate(BooleanSupplier gate) {
        this.gate = gate;
    }

    @Override
    public void onKey(int code, boolean pressed) {
        for (Hotkey hotkey : hotkeys) {
            if (hotkey.getKey() != code) continue;
            if (!hotkey.isHold()) {
                if (pressed && gate.getAsBoolean()) setActive(!active);
            } else if (!pressed) {
                if (hotkey.isEngaged()) setActive(hotkey.release());
            } else if (!hotkey.isEngaged() && gate.getAsBoolean()) {
                hotkey.engage(active);
                setActive(hotkey.getValue());
            }
        }
    }

    @Override
    public void reconcile() {
        for (Hotkey hotkey : hotkeys) {
            if (hotkey.isEngaged() && !KeyUtils.isDown(hotkey.getKey())) setActive(hotkey.release());
        }
    }

    public JsonArray save() {
        JsonArray json = new JsonArray();
        for (Hotkey hotkey : hotkeys) json.add(hotkey.save());
        return json;
    }

    public void load(JsonElement json) {
        clear();
        if (json instanceof JsonArray array) {
            for (JsonElement element : array) {
                if (element instanceof JsonObject object) hotkeys.add(Hotkey.load(object));
            }
        } else if (json instanceof JsonObject object) {
            Hotkey hotkey = Hotkey.load(object);
            if (hotkey.getKey() != KeyUtils.NONE) hotkeys.add(hotkey);
        }
    }

    private void setActive(boolean active) {
        if (this.active == active) return;
        this.active = active;
        listener.accept(active);
    }
}
