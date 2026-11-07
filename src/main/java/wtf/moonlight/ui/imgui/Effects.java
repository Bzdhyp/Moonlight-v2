package wtf.moonlight.ui.imgui;

import imgui.ImDrawList;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wtf.moonlight.module.impl.render.PostProcess;

public final class Effects {
    static final long BLUR = -2;
    static final long SHADOW = -3;

    private static final Logger LOGGER = LoggerFactory.getLogger("moonlight/post");

    private static @Nullable PostProcess module;
    private static boolean failed;

    private Effects() {
    }

    public static void use(PostProcess value) {
        module = value;
    }

    public static void blur(ImDrawList draw, float minX, float minY, float maxX, float maxY, float rounding, int flags) {
        PostProcess settings = settings();
        if (settings == null || !settings.blur()) return;
        draw.addImageRounded(BLUR, minX, minY, maxX, maxY, 0, 0, 1, 1, 0xFFFFFFFF, rounding, flags);
    }

    public static void shadow(ImDrawList draw, float minX, float minY, float maxX, float maxY, float rounding, int flags, int color) {
        PostProcess settings = settings();
        if (settings == null || !settings.shadow()) return;
        draw.pushTextureID(SHADOW);
        draw.addRectFilled(minX, minY, maxX, maxY, color, rounding, flags);
        draw.popTextureID();
    }

    static @Nullable PostProcess settings() {
        return failed ? null : module;
    }

    static void fail(RuntimeException exception) {
        if (!failed) LOGGER.error("Post-processing stopped after an error", exception);
        failed = true;
    }
}
