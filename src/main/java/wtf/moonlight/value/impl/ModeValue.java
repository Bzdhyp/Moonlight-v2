package wtf.moonlight.value.impl;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import wtf.moonlight.value.Value;

import java.util.List;

public final class ModeValue extends Value<Integer> {
    private final List<String> modes;

    public ModeValue(String name, int value, String... modes) {
        super(name, value, () -> true);
        this.modes = List.of(modes);
    }

    @Override
    public void set(Integer value) {
        if (value >= 0 && value < modes.size()) super.set(value);
    }

    public List<String> getModes() {
        return modes;
    }

    @Override
    public JsonElement save() {
        return new JsonPrimitive(modes.get(value));
    }

    @Override
    public void load(JsonElement json) {
        if (json.isJsonPrimitive()) set(modes.indexOf(json.getAsString()));
    }
}
