package wtf.moonlight.ui.imgui.post;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.renderpearl.api.GpuFormat;
import org.jspecify.annotations.Nullable;

final class Target {
    private final String name;
    private @Nullable RenderTarget target;

    Target(String name) {
        this.name = name;
    }

    RenderTarget ensure(int width, int height) {
        RenderTarget current = target;
        if (current == null) {
            current = new TextureTarget(name, width, height, GpuFormat.RGBA8_UNORM, null);
            target = current;
        } else if (current.width != width || current.height != height) {
            current.resize(width, height);
        }
        return current;
    }

    void close() {
        if (target != null) target.destroyBuffers();
        target = null;
    }
}
