package wtf.moonlight.bind;

import com.google.gson.JsonObject;
import wtf.moonlight.utils.KeyUtils;

public final class Hotkey {
    private int key;
    private boolean hold;
    private boolean value = true;
    private boolean visible = true;
    private boolean engaged;
    private boolean restore;

    public Hotkey(int key) {
        this.key = key;
    }

    public int getKey() {
        return key;
    }

    public void setKey(int key) {
        this.key = key;
    }

    public boolean isHold() {
        return hold;
    }

    public void setHold(boolean hold) {
        this.hold = hold;
        engaged = false;
    }

    public boolean getValue() {
        return value;
    }

    public void setValue(boolean value) {
        this.value = value;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public boolean isEngaged() {
        return engaged;
    }

    public String label() {
        return (hold ? "Hold" : "Toggle") + " \"" + KeyUtils.getName(key) + "\"";
    }

    void engage(boolean before) {
        engaged = true;
        restore = before;
    }

    boolean release() {
        engaged = false;
        return restore;
    }

    JsonObject save() {
        JsonObject json = new JsonObject();
        json.addProperty("key", key);
        json.addProperty("hold", hold);
        json.addProperty("value", value);
        json.addProperty("visible", visible);
        return json;
    }

    static Hotkey load(JsonObject json) {
        Hotkey hotkey = new Hotkey(json.has("key") ? json.get("key").getAsInt() : KeyUtils.NONE);
        if (json.has("hold")) hotkey.hold = json.get("hold").getAsBoolean();
        if (json.has("value")) hotkey.value = json.get("value").getAsBoolean();
        if (json.has("visible")) hotkey.visible = json.get("visible").getAsBoolean();
        return hotkey;
    }
}
