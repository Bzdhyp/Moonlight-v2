package wtf.moonlight.ui.imgui;

import imgui.ImDrawList;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Textures {
    private static final long FIRST = 1L << 40;

    private static final Map<Identifier, Long> ids = new HashMap<>();
    private static final List<Identifier> paths = new ArrayList<>();

    private Textures() {
    }

    public static long of(Identifier path) {
        Long id = ids.get(path);
        if (id != null) return id;
        long next = FIRST + paths.size();
        paths.add(path);
        ids.put(path, next);
        return next;
    }

    public static void face(ImDrawList draw, Identifier skin, float centerX, float centerY, float radius, int tint) {
        long texture = of(skin);
        draw.addImageRounded(texture, centerX - radius, centerY - radius, centerX + radius, centerY + radius,
                0.125F, 0.125F, 0.25F, 0.25F, tint, radius);
        draw.addImageRounded(texture, centerX - radius, centerY - radius, centerX + radius, centerY + radius,
                0.625F, 0.125F, 0.75F, 0.25F, tint, radius);
    }

    static @Nullable Identifier path(long id) {
        long index = id - FIRST;
        return index >= 0 && index < paths.size() ? paths.get((int) index) : null;
    }
}
