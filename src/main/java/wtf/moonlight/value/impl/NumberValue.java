package wtf.moonlight.value.impl;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import wtf.moonlight.value.Value;

import java.util.Locale;
import java.util.function.BooleanSupplier;

public final class NumberValue extends Value<Double> {
    private final double min;
    private final double max;
    private final double step;
    private final String format;

    public NumberValue(String name, double min, double max, double step, double value, String format) {
        this(name, min, max, step, value, format, () -> true);
    }

    public NumberValue(String name, double min, double max, double step, double value, String format, BooleanSupplier visibility) {
        super(name, process(value, min, max, step), visibility);
        this.min = min;
        this.max = max;
        this.step = step;
        this.format = format;
    }

    @Override
    public void set(Double value) {
        super.set(process(value, min, max, step));
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    public String format(double value) {
        return String.format(Locale.ROOT, format, value);
    }

    @Override
    public JsonElement save() {
        return new JsonPrimitive(value);
    }

    @Override
    public void load(JsonElement json) {
        if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isNumber()) set(json.getAsDouble());
    }

    private static double process(double value, double min, double max, double step) {
        double steps = value / step;
        double rounded = steps < 0 ? -Math.floor(-steps + 0.5) : Math.floor(steps + 0.5);
        return Math.clamp(rounded * step, min, max);
    }
}
