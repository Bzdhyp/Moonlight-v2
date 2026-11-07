package wtf.moonlight.module.impl.render;

import wtf.moonlight.module.Anchor;
import wtf.moonlight.module.Module;
import wtf.moonlight.module.Subcategory;
import wtf.moonlight.value.impl.ListValue;
import wtf.moonlight.value.impl.NumberValue;
import wtf.moonlight.value.impl.TextValue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.SequencedMap;

public final class Watermark extends Module {
    private final TextValue title = add(new TextValue("Title", "ML", () -> true));
    private final ListValue items = add(new ListValue("Items", options(),
            List.of(Item.LOGO.label, Item.FPS.label, Item.USER.label, Item.TIME.label, Item.AVATAR.label)));
    private final Anchor anchor = new Anchor(this, 12, 12);
    private final NumberValue scale = add(new NumberValue("Scale", 0.5, 3, 0.05, 1.5, "%.2f"));
    private final NumberValue animation = add(new NumberValue("Animation", 100, 1000, 50, 300, "%.0f ms"));

    public Watermark() {
        super("Watermark", Subcategory.OVERLAY);
    }

    public String title() {
        return title.get().strip();
    }

    public List<Item> items() {
        List<Item> shown = new ArrayList<>();
        for (String label : items.getOptions()) {
            for (Item item : Item.values()) {
                if (item.label.equals(label) && items.has(label) && (item != Item.LOGO || !title().isEmpty())) shown.add(item);
            }
        }
        return shown;
    }

    public Anchor anchor() {
        return anchor;
    }

    public float scale() {
        return scale.get().floatValue();
    }

    public float animation() {
        return animation.get().floatValue() / 1000.0F;
    }

    private static SequencedMap<String, String> options() {
        SequencedMap<String, String> options = new LinkedHashMap<>();
        for (Item item : Item.values()) options.put(item.label, item.icon);
        return options;
    }

    public enum Item {
        LOGO("Logo", 0xE51C),
        FPS("FPS", 0xE26B),
        USER("User", 0xE7FD),
        SERVER("Server", 0xE875),
        PING("Ping", 0xE63E),
        TPS("TPS", 0xE9E4),
        COORDINATES("Coordinates", 0xE0C8),
        SPEED("Speed", 0xEA0B),
        VERSION("Version", 0xF720),
        TIME("Time", 0xF3E6),
        AVATAR("Avatar", 0xE853);

        private final String label;
        private final String icon;

        Item(String label, int icon) {
            this.label = label;
            this.icon = Character.toString(icon);
        }

        public String icon() {
            return icon;
        }
    }
}
