package wtf.moonlight.value.impl;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import wtf.moonlight.value.Value;

import java.util.function.BooleanSupplier;

public final class TextValue extends Value<String> {
    public TextValue(String name, String value, BooleanSupplier visibility) {
        super(name, value, visibility);
    }

    @Override
    public JsonElement save() {
        return new JsonPrimitive(value);
    }

    @Override
    public void load(JsonElement json) {
        if (json.isJsonPrimitive()) set(json.getAsString());
    }
}
