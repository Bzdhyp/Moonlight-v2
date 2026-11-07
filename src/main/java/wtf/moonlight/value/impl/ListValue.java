package wtf.moonlight.value.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jspecify.annotations.Nullable;
import wtf.moonlight.value.Value;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.SequencedMap;
import java.util.Set;

public final class ListValue extends Value<Set<String>> {
    private final Map<String, String> icons;
    private final boolean sortable;
    private List<String> order;

    public ListValue(String name, SequencedMap<String, String> options, List<String> selected) {
        super(name, Set.copyOf(selected), () -> true);
        icons = Map.copyOf(options);
        order = List.copyOf(options.sequencedKeySet());
        sortable = true;
    }

    public ListValue(String name, List<String> options, List<String> selected) {
        super(name, Set.copyOf(selected), () -> true);
        icons = Map.of();
        order = List.copyOf(options);
        sortable = false;
    }

    public List<String> getOptions() {
        return order;
    }

    public boolean isSortable() {
        return sortable;
    }

    public @Nullable String icon(String option) {
        return icons.get(option);
    }

    public boolean has(String option) {
        return value.contains(option);
    }

    public void toggle(String option) {
        Set<String> next = new LinkedHashSet<>(value);
        if (!next.remove(option)) next.add(option);
        set(Set.copyOf(next));
    }

    public void move(String option, int index) {
        List<String> next = new ArrayList<>(order);
        if (!next.remove(option)) return;
        next.add(Math.clamp(index, 0, next.size()), option);
        order = List.copyOf(next);
    }

    @Override
    public JsonElement save() {
        JsonArray options = new JsonArray();
        JsonArray selected = new JsonArray();
        for (String option : order) {
            options.add(option);
            if (value.contains(option)) selected.add(option);
        }
        JsonObject json = new JsonObject();
        json.add("order", options);
        json.add("selected", selected);
        return json;
    }

    @Override
    public void load(JsonElement json) {
        JsonElement selected = json;
        if (json instanceof JsonObject object) {
            selected = object.get("selected");
            if (sortable && object.get("order") instanceof JsonArray loaded) {
                List<String> next = new ArrayList<>();
                for (String option : names(loaded)) {
                    if (order.contains(option) && !next.contains(option)) next.add(option);
                }
                for (String option : order) {
                    if (!next.contains(option)) next.add(option);
                }
                order = List.copyOf(next);
            }
        }
        if (selected instanceof JsonArray array) set(Set.copyOf(names(array).stream().filter(order::contains).toList()));
    }

    private static List<String> names(JsonArray array) {
        List<String> names = new ArrayList<>();
        for (JsonElement element : array) {
            if (element.isJsonPrimitive()) names.add(element.getAsString());
        }
        return names;
    }
}
