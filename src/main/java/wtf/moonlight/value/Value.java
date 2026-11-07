package wtf.moonlight.value;

import com.google.gson.JsonElement;

import java.util.function.BooleanSupplier;

public abstract class Value<T> {
    private final String name;
    private final BooleanSupplier visibility;
    protected T value;

    protected Value(String name, T value, BooleanSupplier visibility) {
        this.name = name;
        this.value = value;
        this.visibility = visibility;
    }

    public String getName() {
        return name;
    }

    public T get() {
        return value;
    }

    public void set(T value) {
        this.value = value;
    }

    public boolean isVisible() {
        return visibility.getAsBoolean();
    }

    public abstract JsonElement save();

    public abstract void load(JsonElement json);
}
