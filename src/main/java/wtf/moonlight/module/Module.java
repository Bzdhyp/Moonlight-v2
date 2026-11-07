package wtf.moonlight.module;

import org.jspecify.annotations.Nullable;
import wtf.moonlight.Moonlight;
import wtf.moonlight.bind.Bind;
import wtf.moonlight.value.Value;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class Module {
    private final String name;
    private final Category category;
    private final @Nullable Subcategory subcategory;
    private final Bind bind;
    private final List<Value<?>> values = new ArrayList<>();
    private boolean enabled;

    protected Module(String name, Category category) {
        this(name, category, null);
    }

    protected Module(String name, Subcategory subcategory) {
        this(name, subcategory.getCategory(), subcategory);
    }

    private Module(String name, Category category, @Nullable Subcategory subcategory) {
        this.name = name;
        this.category = category;
        this.subcategory = subcategory;
        this.bind = new Bind(name, this::setEnabled);
    }

    public String getName() {
        return name;
    }

    public Category getCategory() {
        return category;
    }

    public @Nullable Subcategory getSubcategory() {
        return subcategory;
    }

    public Bind getBind() {
        return bind;
    }

    public List<Value<?>> getValues() {
        return Collections.unmodifiableList(values);
    }

    protected <V extends Value<?>> V add(V value) {
        values.add(value);
        return value;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) return;
        this.enabled = enabled;
        bind.sync(enabled);
        if (enabled) {
            onEnable();
            Moonlight.EVENT_BUS.register(this);
        } else {
            Moonlight.EVENT_BUS.unregister(this);
            onDisable();
        }
    }

    public boolean isPersistent() {
        return true;
    }

    protected void onEnable() {
    }

    protected void onDisable() {
    }
}
