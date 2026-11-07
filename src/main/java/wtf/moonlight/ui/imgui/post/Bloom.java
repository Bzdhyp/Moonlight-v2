package wtf.moonlight.ui.imgui.post;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.renderpearl.api.commands.CommandEncoder;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.textures.GpuTextureView;

import java.util.Optional;
import java.util.function.Consumer;

public final class Bloom {
    private static final float MAX_STEP = 2;

    private final Kawase kawase = new Kawase();
    private final Target[] levels = new Target[Kawase.MAX_LEVELS + 3];
    private final Target mask = new Target("Moonlight shadow mask");
    private final Target result = new Target("Moonlight shadow");

    public Bloom() {
        for (int i = 0; i < levels.length; i++) levels[i] = new Target("Moonlight shadow " + (i + 1));
    }

    public void beginFrame() {
        kawase.beginFrame();
    }

    public void render(CommandEncoder encoder, RenderTarget main, Area area, int amount, int offset, float opacity, int color, Consumer<RenderPass> shapes) {
        int width = main.width;
        int height = main.height;
        int passes = Math.clamp(amount, 1, Kawase.MAX_LEVELS);
        float step = Math.max(1, offset);
        while (step > MAX_STEP) {
            step /= 2;
            passes++;
        }
        Area region = area.expand(step * (4 << passes) + 2, width, height);
        if (region.isEmpty()) return;
        RenderTarget shape = mask.ensure(width, height);
        try (RenderPass pass = encoder.createRenderPass(() -> "Moonlight shadow mask", Kawase.view(shape), Optional.of(Kawase.CLEAR))) {
            shapes.accept(pass);
        }
        GpuTextureView source = Kawase.view(shape);
        for (int k = 1; k <= passes; k++) {
            RenderTarget level = levels[k - 1].ensure(Math.max(1, width >> k), Math.max(1, height >> k));
            kawase.run(encoder, Kawase.DOWN, level, region, width, height, step / level.width, step / level.height, source, null, 0);
            source = Kawase.view(level);
        }
        for (int k = passes - 1; k >= 1; k--) {
            RenderTarget level = levels[k - 1].ensure(Math.max(1, width >> k), Math.max(1, height >> k));
            kawase.run(encoder, Kawase.UP, level, region, width, height, step / level.width, step / level.height, source, null, 0);
            source = Kawase.view(level);
        }
        RenderTarget out = result.ensure(width, height);
        kawase.run(encoder, Kawase.CUT, out, region, width, height, step / width, step / height, source, Kawase.view(shape), 1 / opacity);
        kawase.composite(encoder, main, region, Kawase.view(out), color);
    }

    public void close() {
        for (Target level : levels) level.close();
        mask.close();
        result.close();
        kawase.close();
    }
}
