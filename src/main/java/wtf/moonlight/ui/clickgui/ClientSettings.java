package wtf.moonlight.ui.clickgui;

import org.jspecify.annotations.Nullable;
import wtf.moonlight.value.Value;
import wtf.moonlight.value.impl.BoolValue;
import wtf.moonlight.value.impl.ColorValue;
import wtf.moonlight.value.impl.ModeValue;
import wtf.moonlight.value.impl.NumberValue;
import wtf.moonlight.value.impl.TextValue;

import java.util.List;

public final class ClientSettings {
    private static final String DEFAULT_NAME = "Moonlight";
    private static final int LIGHT = 1;

    private final ModeValue theme = new ModeValue("Theme", 0, "Dark", "Light");
    private final ColorValue themeColor = new ColorValue("Theme Color", Theme.ACCENT);
    private final NumberValue size = new NumberValue("Size", 60, 150, 5, 100, "%.0f%%");
    private final NumberValue hudSize = new NumberValue("HUD Scale", 50, 200, 5, 100, "%.0f%%");
    private final NumberValue hudAlpha = new NumberValue("HUD Alpha", 0, 100, 1, 100, "%.0f%%");
    private final NumberValue hudMenuSize = new NumberValue("HUD Menu Size", 60, 150, 5, 125, "%.0f%%");
    private final NumberValue background = new NumberValue("Background Alpha", 0, 100, 1, 100, "%.0f%%");
    private final BoolValue customName = new BoolValue("Custom Client Name", false);
    private final TextValue name = new TextValue("Client Name", "", customName::get);
    private final List<Value<?>> values = List.of(theme, themeColor, size, hudSize, hudAlpha, background, hudMenuSize, customName, name);
    private @Nullable Theme cached;
    private boolean cachedLight;
    private int cachedAccent;
    private double cachedBackground;

    public List<Value<?>> getValues() {
        return values;
    }

    public boolean isLight() {
        return theme.get() == LIGHT;
    }

    public int accent() {
        return themeColor.get();
    }

    public float hudScale() {
        return (float) (hudSize.get() / 100.0);
    }

    public float hudOpacity() {
        return (float) (hudAlpha.get() / 100.0);
    }

    ModeValue theme() {
        return theme;
    }

    ColorValue themeColor() {
        return themeColor;
    }

    NumberValue menuSize() {
        return size;
    }

    NumberValue hudSize() {
        return hudSize;
    }

    NumberValue hudAlpha() {
        return hudAlpha;
    }

    NumberValue background() {
        return background;
    }

    Theme colors() {
        Theme current = cached;
        if (current == null || cachedLight != isLight() || cachedAccent != accent() || cachedBackground != background.get()) {
            cachedLight = isLight();
            cachedAccent = accent();
            cachedBackground = background.get();
            current = Theme.of(cachedLight, cachedAccent, (float) (cachedBackground / 100.0));
            cached = current;
        }
        return current;
    }

    float guiScale() {
        return (float) (size.get() / 100.0);
    }

    float hudMenuScale() {
        return (float) (hudMenuSize.get() / 100.0);
    }

    String clientName() {
        if (!customName.get()) return DEFAULT_NAME;
        StringBuilder clean = new StringBuilder(name.get());
        for (int i = 0; i < clean.length(); i++) {
            if (clean.charAt(i) < 32) clean.setCharAt(i, ' ');
        }
        String trimmed = clean.toString().strip();
        return trimmed.isEmpty() ? DEFAULT_NAME : trimmed;
    }
}
