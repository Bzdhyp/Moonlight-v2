package wtf.moonlight.ui.clickgui;

import wtf.moonlight.value.Value;
import wtf.moonlight.value.impl.BoolValue;
import wtf.moonlight.value.impl.ModeValue;
import wtf.moonlight.value.impl.NumberValue;
import wtf.moonlight.value.impl.TextValue;

import java.util.List;

public final class ClientSettings {
    private static final String DEFAULT_NAME = "Moonlight";
    private static final int LIGHT = 0;

    private final ModeValue theme = new ModeValue("Theme", 1, "Light", "Dark");
    private final NumberValue size = new NumberValue("Size", 60, 150, 5, 100, "%.0f%%");
    private final BoolValue customName = new BoolValue("Custom Client Name", false);
    private final TextValue name = new TextValue("Client Name", "", customName::get);
    private final List<Value<?>> values = List.of(theme, size, customName, name);

    public List<Value<?>> getValues() {
        return values;
    }

    boolean isLight() {
        return theme.get() == LIGHT;
    }

    void toggleTheme() {
        theme.set(isLight() ? 1 : LIGHT);
    }

    Theme colors() {
        return isLight() ? Theme.LIGHT : Theme.DARK;
    }

    float guiScale() {
        return (float) (size.get() / 100.0);
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
