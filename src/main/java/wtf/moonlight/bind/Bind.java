package wtf.moonlight.bind;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import wtf.moonlight.utils.KeyUtils;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public final class Bind implements Bindable {
    private final String name;
    private final Consumer<Boolean> listener;
    private BooleanSupplier gate = () -> Minecraft.getInstance().gui.screen() == null;
    private int key = KeyUtils.NONE;
    private boolean hold;
    private boolean active;
    private boolean holdPressed;

    public Bind(String name, Consumer<Boolean> listener) {
        this.name = name;
        this.listener = listener;
    }

    public String getName() {
        return name;
    }

    @Override
    public int getKey() {
        return key;
    }

    public void setKey(int key) {
        if (this.key == key) return;
        if (holdPressed) onBind(false);
        this.key = key;
    }

    public boolean isHold() {
        return hold;
    }

    public void setHold(boolean hold) {
        if (this.hold == hold) return;
        holdPressed = false;
        setActive(false);
        this.hold = hold;
    }

    public boolean isActive() {
        return active;
    }

    public void sync(boolean active) {
        this.active = active;
        if (!active) holdPressed = false;
    }

    @Override
    public boolean triggersOnRelease() {
        return hold;
    }

    @Override
    public void onBind(boolean pressed) {
        if (hold) {
            if (!pressed) {
                holdPressed = false;
                setActive(false);
                return;
            }
            if (holdPressed || !gate.getAsBoolean()) return;
            holdPressed = true;
            setActive(true);
            return;
        }
        if (!pressed || !gate.getAsBoolean()) return;
        setActive(!active);
    }

    public void setGate(BooleanSupplier gate) {
        this.gate = gate;
    }

    public void reconcileHold(boolean physicallyDown) {
        if (hold && holdPressed && !physicallyDown) onBind(false);
    }

    public JsonObject save() {
        JsonObject json = new JsonObject();
        json.addProperty("key", key);
        json.addProperty("hold", hold);
        return json;
    }

    public void load(JsonElement element) {
        if (!element.isJsonObject()) return;
        JsonObject json = element.getAsJsonObject();
        if (json.has("key")) setKey(json.get("key").getAsInt());
        if (json.has("hold")) setHold(json.get("hold").getAsBoolean());
    }

    private void setActive(boolean active) {
        if (this.active == active) return;
        this.active = active;
        listener.accept(active);
    }
}
