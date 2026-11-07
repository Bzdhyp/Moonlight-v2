package wtf.moonlight.value.impl;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import org.jspecify.annotations.Nullable;
import wtf.moonlight.value.Value;

import java.util.Locale;

public final class ColorValue extends Value<Integer> {
    private final int initial;

    public ColorValue(String name, int value) {
        super(name, value, () -> true);
        initial = value;
    }

    public boolean isInitial() {
        return value == initial;
    }

    public void reset() {
        set(initial);
    }

    @Override
    public JsonElement save() {
        return new JsonPrimitive(hex(value));
    }

    @Override
    public void load(JsonElement json) {
        if (!json.isJsonPrimitive()) return;
        Integer color = parse(json.getAsString());
        if (color != null) set(color);
    }

    public static String hex(int color) {
        return String.format(Locale.ROOT, "#%02X%02X%02X%02X", color >>> 24, color & 0xFF, color >>> 8 & 0xFF, color >>> 16 & 0xFF);
    }

    public static @Nullable Integer parse(String text) {
        String hex = text.strip();
        if (hex.startsWith("#")) hex = hex.substring(1);
        if (!hex.matches("[0-9A-Fa-f]{6}|[0-9A-Fa-f]{8}")) return null;
        long argb = Long.parseLong(hex, 16);
        if (hex.length() == 6) argb |= 0xFF000000L;
        return (int) (argb >>> 16 & 0xFF | (argb & 0xFF00) | (argb & 0xFF) << 16 | (argb >>> 24 & 0xFF) << 24);
    }
}
