package wtf.moonlight.ui.imgui.post;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.renderpearl.api.commands.CommandEncoder;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import org.jspecify.annotations.Nullable;

public final class Blur {
    private static final long REFRESH_NANOS = 1_000_000_000L / 60;

    private final Kawase kawase = new Kawase();
    private final Target[] levels = new Target[Kawase.MAX_LEVELS];
    private final Target result = new Target("Moonlight blur");
    private @Nullable Area cached;
    private long cachedAt;
    private int cachedAmount;
    private int cachedOffset;
    private int cachedWidth;
    private int cachedHeight;

    public Blur() {
        for (int i = 0; i < levels.length; i++) levels[i] = new Target("Moonlight blur " + (i + 1));
    }

    public void beginFrame() {
        kawase.beginFrame();
    }

    public boolean bake(CommandEncoder encoder, RenderTarget main, Area area, int amount, int offset, boolean base) {
        int width = main.width;
        int height = main.height;
        int passes = Math.clamp(amount, 1, Kawase.MAX_LEVELS);
        Area region = area.expand(3 * passes * offset + (2 << passes) + 2, width, height);
        if (region.isEmpty()) return false;
        long now = System.nanoTime();
        if (base && cached != null && now - cachedAt < REFRESH_NANOS && cachedAmount == passes && cachedOffset == offset
                && cachedWidth == width && cachedHeight == height && cached.contains(region)) {
            return true;
        }
        float stepX = offset / (float) width;
        float stepY = offset / (float) height;
        GpuTextureView source = Kawase.view(main);
        for (int k = 1; k <= passes; k++) {
            RenderTarget level = levels[k - 1].ensure(Math.max(1, width >> k), Math.max(1, height >> k));
            kawase.run(encoder, Kawase.DOWN, level, region, width, height, stepX, stepY, source, null, 0);
            source = Kawase.view(level);
        }
        for (int k = passes - 1; k >= 1; k--) {
            RenderTarget level = levels[k - 1].ensure(Math.max(1, width >> k), Math.max(1, height >> k));
            kawase.run(encoder, Kawase.UP, level, region, width, height, stepX, stepY, source, null, 0);
            source = Kawase.view(level);
        }
        RenderTarget out = result.ensure(width, height);
        kawase.run(encoder, Kawase.UP, out, region, width, height, stepX * 0.5f, stepY * 0.5f, source, null, 0);
        cached = base ? region : null;
        cachedAt = now;
        cachedAmount = passes;
        cachedOffset = offset;
        cachedWidth = width;
        cachedHeight = height;
        return true;
    }

    public GpuTextureView view() {
        return Kawase.view(result.ensure(cachedWidth, cachedHeight));
    }

    public void close() {
        for (Target level : levels) level.close();
        result.close();
        kawase.close();
        cached = null;
    }
}
