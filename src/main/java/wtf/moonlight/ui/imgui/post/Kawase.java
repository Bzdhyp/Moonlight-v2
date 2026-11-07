package wtf.moonlight.ui.imgui.post;

import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.CommandEncoder;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.pipeline.BindGroupLayout;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.UniformType;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import net.minecraft.client.renderer.DynamicGpuDataStorage;
import net.minecraft.client.renderer.DynamicGpuDataStorageMapped;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.joml.Vector4f;
import org.joml.Vector4fc;
import org.jspecify.annotations.Nullable;
import wtf.moonlight.Moonlight;

import java.nio.ByteBuffer;
import java.util.Optional;

final class Kawase {
    static final int MAX_LEVELS = 8;
    static final Vector4fc CLEAR = new Vector4f();
    static final RenderPipeline DOWN = pipeline("blur_down", false, ColorTargetState.DEFAULT);
    static final RenderPipeline UP = pipeline("blur_up", false, ColorTargetState.DEFAULT);
    static final RenderPipeline CUT = pipeline("bloom_cut", true, ColorTargetState.DEFAULT);

    private static final RenderPipeline SHADOW = pipeline("shadow", false, new ColorTargetState(BlendFunction.TRANSLUCENT));
    private static final int CONFIG_SIZE = new Std140SizeCalculator().putVec4().putVec4().get();
    private static final int MARGIN = 2;

    private @Nullable DynamicGpuDataStorageMapped<Config> configs;

    void beginFrame() {
        if (configs != null) configs.endFrame();
    }

    void run(CommandEncoder encoder, RenderPipeline pipeline, RenderTarget out, Area area, int width, int height,
             float stepX, float stepY, GpuTextureView in, @Nullable GpuTextureView mask, float cut) {
        GpuBufferSlice config = config(new Config(stepX, stepY, cut, 0));
        try (RenderPass pass = encoder.createRenderPass(() -> "Moonlight post", view(out), Optional.of(CLEAR))) {
            if (!area.scissor(pass, out.width, out.height, width, height, out.width == width ? 0 : MARGIN)) return;
            pass.setPipeline(RenderSystem.getCompiledPipeline(pipeline));
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("PostConfig", config);
            pass.setUniform("InSampler", in, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
            if (mask != null) pass.setUniform("MaskSampler", mask, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
            pass.draw(3, 1, 0, 0);
        }
    }

    void composite(CommandEncoder encoder, RenderTarget main, Area area, GpuTextureView in, int color) {
        GpuBufferSlice config = config(new Config(0, 0, 0, color));
        try (RenderPass pass = encoder.createRenderPass(() -> "Moonlight shadow", view(main), Optional.empty())) {
            if (!area.scissor(pass, main.width, main.height, main.width, main.height, 0)) return;
            pass.setPipeline(RenderSystem.getCompiledPipeline(SHADOW));
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("PostConfig", config);
            pass.setUniform("InSampler", in, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
            pass.draw(3, 1, 0, 0);
        }
    }

    void close() {
        if (configs != null) configs.close();
        configs = null;
    }

    static GpuTextureView view(RenderTarget target) {
        GpuTextureView view = target.getColorTextureView();
        if (view == null) throw new IllegalStateException("Render target has no color texture");
        return view;
    }

    private GpuBufferSlice config(Config config) {
        DynamicGpuDataStorageMapped<Config> storage = configs;
        if (storage == null) {
            storage = new DynamicGpuDataStorageMapped<>("Moonlight post config", CONFIG_SIZE, GpuBuffer.USAGE_UNIFORM, 32);
            configs = storage;
        }
        return storage.writeData(config);
    }

    private static RenderPipeline pipeline(String name, boolean masked, ColorTargetState target) {
        BindGroupLayout.Builder layout = BindGroupLayout.builder()
                .withUniform("PostConfig", UniformType.UNIFORM_BUFFER)
                .withUniform("InSampler", UniformType.COMBINED_IMAGE_SAMPLER);
        if (masked) layout.withUniform("MaskSampler", UniformType.COMBINED_IMAGE_SAMPLER);
        return RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
                .withLocation(Identifier.fromNamespaceAndPath(Moonlight.MOD_ID, "pipeline/" + name))
                .withVertexShader("core/screenquad")
                .withFragmentShader(Identifier.fromNamespaceAndPath(Moonlight.MOD_ID, "core/" + name))
                .withBindGroupLayout(layout.build())
                .withColorTargetState(target)
                .withCull(false)
                .build();
    }

    private record Config(float x, float y, float cut, int color) implements DynamicGpuDataStorage.DynamicGpuData {
        @Override
        public void write(ByteBuffer data) {
            Std140Builder.intoBuffer(data)
                    .putVec4(x, y, cut, 0)
                    .putVec4((color & 0xFF) / 255.0F, (color >>> 8 & 0xFF) / 255.0F, (color >>> 16 & 0xFF) / 255.0F, (color >>> 24) / 255.0F);
        }
    }
}
