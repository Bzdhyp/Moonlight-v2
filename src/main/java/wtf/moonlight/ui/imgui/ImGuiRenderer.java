package wtf.moonlight.ui.imgui;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.CommandEncoder;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.device.GpuDevice;
import com.mojang.renderpearl.api.pipeline.BlendFactor;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.IndexType;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuSampler;
import com.mojang.renderpearl.api.textures.GpuTexture;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import imgui.ImDrawData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Projection;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;
import wtf.moonlight.Moonlight;
import wtf.moonlight.module.impl.render.PostProcess;
import wtf.moonlight.ui.imgui.post.Area;
import wtf.moonlight.ui.imgui.post.Bloom;
import wtf.moonlight.ui.imgui.post.Blur;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

final class ImGuiRenderer {
    private static final RenderPipeline PIPELINE = pipeline("pipeline/imgui").build();
    private static final RenderPipeline MASK = pipeline("pipeline/imgui_mask")
            .withFragmentShader(Identifier.fromNamespaceAndPath(Moonlight.MOD_ID, "core/imgui_mask"))
            .withColorTargetState(new ColorTargetState(new BlendFunction(BlendFactor.ONE, BlendFactor.ONE_MINUS_SRC_ALPHA, BlendFactor.ONE, BlendFactor.ONE_MINUS_SRC_ALPHA)))
            .build();
    private static final RenderPipeline BLUR = pipeline("pipeline/imgui_blur")
            .withVertexShader(Identifier.fromNamespaceAndPath(Moonlight.MOD_ID, "core/imgui_blur"))
            .withFragmentShader(Identifier.fromNamespaceAndPath(Moonlight.MOD_ID, "core/imgui_blur"))
            .build();
    private static final int VERTEX_SIZE = 24;
    private static final int ALIGNMENT = 16;
    private static final int STATUS_OK = 0;
    private static final int STATUS_DESTROYED = 1;
    private static final int STATUS_WANT_CREATE = 2;
    private static final int STATUS_WANT_UPDATES = 3;
    private static final int STATUS_WANT_DESTROY = 4;

    private final Map<Long, Texture> textures = new HashMap<>();
    private final Projection projection = new Projection();
    private final ProjectionMatrixBuffer projectionBuffer = new ProjectionMatrixBuffer("imgui");
    private final IndexType indexType = ImDrawData.sizeOfImDrawIdx() == 2 ? IndexType.SHORT : IndexType.INT;
    private final Blur blur = new Blur();
    private final Bloom bloom = new Bloom();
    private @Nullable Texture white;
    private long nextId = 1;

    void render(ImDrawData data) {
        GpuDevice device = RenderSystem.getDevice();
        CommandEncoder encoder = device.createCommandEncoder();
        updateTextures(data, device, encoder);
        PostProcess settings = Effects.settings();
        if (settings != null && (settings.blur() || settings.shadow())) {
            blur.beginFrame();
            bloom.beginFrame();
        } else {
            release();
        }
        int totalVertices = Natives.totalVertices(data);
        int totalIndices = Natives.totalIndices(data);
        if (totalVertices == 0 || totalIndices == 0) return;

        RenderTarget target = Minecraft.getInstance().gameRenderer.mainRenderTarget();
        GpuTextureView color = target.getColorTextureView();
        if (color == null) return;

        ByteBuffer vertices = MemoryUtil.memAlloc(totalVertices * VERTEX_SIZE);
        ByteBuffer indices = MemoryUtil.memAlloc(totalIndices * indexType.bytes);
        GpuBufferSlice vertexSlice;
        GpuBufferSlice indexSlice;
        try {
            long vertexOut = MemoryUtil.memAddress(vertices);
            long indexOut = MemoryUtil.memAddress(indices);
            for (int n = 0; n < Natives.listCount(data); n++) {
                long list = Natives.list(data, n);
                long vertex = Natives.vertices(list);
                for (int i = Natives.vertexCount(list); i > 0; i--, vertex += Natives.VERTEX_SIZE, vertexOut += VERTEX_SIZE) {
                    MemoryUtil.memPutFloat(vertexOut, MemoryUtil.memGetFloat(vertex));
                    MemoryUtil.memPutFloat(vertexOut + 4, MemoryUtil.memGetFloat(vertex + 4));
                    MemoryUtil.memPutFloat(vertexOut + 8, 0);
                    MemoryUtil.memPutFloat(vertexOut + 12, MemoryUtil.memGetFloat(vertex + 8));
                    MemoryUtil.memPutFloat(vertexOut + 16, MemoryUtil.memGetFloat(vertex + 12));
                    MemoryUtil.memPutInt(vertexOut + 20, MemoryUtil.memGetInt(vertex + 16));
                }
                long bytes = (long) Natives.indexCount(list) * indexType.bytes;
                MemoryUtil.memCopy(Natives.indices(list), indexOut, bytes);
                indexOut += bytes;
            }
            vertexSlice = encoder.transientMemory().uploadGpu(vertices, ALIGNMENT, GpuBuffer.USAGE_VERTEX);
            indexSlice = encoder.transientMemory().uploadGpu(indices, ALIGNMENT, GpuBuffer.USAGE_INDEX);
        } finally {
            MemoryUtil.memFree(vertices);
            MemoryUtil.memFree(indices);
        }

        projection.setupOrtho(1000.0F, 11000.0F, target.width, target.height, true);
        GpuBufferSlice projectionSlice = projectionBuffer.getBuffer(projection);
        GpuBufferSlice transforms = RenderSystem.getDynamicUniforms().writeTransform(new Matrix4f().setTranslation(0.0F, 0.0F, -11000.0F));
        new Frame(data, encoder, target, color, vertexSlice, indexSlice, projectionSlice, transforms,
                (int) (indexSlice.offset() / indexType.bytes)).run();
    }

    private void release() {
        blur.close();
        bloom.close();
        if (white != null) {
            white.view().close();
            white.texture().close();
        }
        white = null;
    }

    private GpuTextureView white(CommandEncoder encoder) {
        Texture current = white;
        if (current != null) return current.view();
        GpuDevice device = RenderSystem.getDevice();
        GpuTexture texture = device.createTexture("ImGui white", GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING,
                GpuFormat.RGBA8_UNORM, 1, 1, 1, 1);
        ByteBuffer pixel = MemoryUtil.memAlloc(4);
        try {
            pixel.putInt(0, 0xFFFFFFFF);
            encoder.writeToTexture(texture, pixel, 0, 0, 0, 0, 1, 1);
        } finally {
            MemoryUtil.memFree(pixel);
        }
        current = new Texture(texture, device.createTextureView(texture));
        white = current;
        return current.view();
    }

    private void updateTextures(ImDrawData data, GpuDevice device, CommandEncoder encoder) {
        long vector = Natives.textures(data);
        if (vector == 0) return;
        long items = MemoryUtil.memGetAddress(vector + 8);
        for (int i = 0, n = MemoryUtil.memGetInt(vector); i < n; i++) {
            long texture = MemoryUtil.memGetAddress(items + 8L * i);
            switch (MemoryUtil.memGetInt(texture + Natives.TEXTURE_STATUS)) {
                case STATUS_WANT_CREATE -> create(texture, device, encoder);
                case STATUS_WANT_UPDATES -> update(texture, encoder);
                case STATUS_WANT_DESTROY -> {
                    if (MemoryUtil.memGetInt(texture + Natives.TEXTURE_UNUSED_FRAMES) > 0) destroy(texture);
                }
                default -> {
                }
            }
        }
    }

    private void create(long texture, GpuDevice device, CommandEncoder encoder) {
        int width = MemoryUtil.memGetInt(texture + Natives.TEXTURE_WIDTH);
        int height = MemoryUtil.memGetInt(texture + Natives.TEXTURE_HEIGHT);
        GpuTexture gpu = device.createTexture("ImGui texture", GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING,
                GpuFormat.RGBA8_UNORM, width, height, 1, 1);
        GpuTextureView view = device.createTextureView(gpu);
        ByteBuffer pixels = MemoryUtil.memByteBuffer(MemoryUtil.memGetAddress(texture + Natives.TEXTURE_PIXELS), width * height * 4);
        encoder.writeToTexture(gpu, pixels, 0, 0, 0, 0, width, height);
        long id = nextId++;
        textures.put(id, new Texture(gpu, view));
        MemoryUtil.memPutLong(texture + Natives.TEXTURE_ID, id);
        MemoryUtil.memPutInt(texture + Natives.TEXTURE_STATUS, STATUS_OK);
    }

    private void update(long texture, CommandEncoder encoder) {
        Texture target = textures.get(MemoryUtil.memGetLong(texture + Natives.TEXTURE_ID));
        if (target != null) {
            int width = MemoryUtil.memGetInt(texture + Natives.TEXTURE_WIDTH);
            long pixels = MemoryUtil.memGetAddress(texture + Natives.TEXTURE_PIXELS);
            long rects = MemoryUtil.memGetAddress(texture + Natives.TEXTURE_UPDATES + 8);
            for (int i = 0, n = MemoryUtil.memGetInt(texture + Natives.TEXTURE_UPDATES); i < n; i++) {
                long rect = rects + 8L * i;
                int x = MemoryUtil.memGetShort(rect) & 0xFFFF;
                int y = MemoryUtil.memGetShort(rect + 2) & 0xFFFF;
                int w = MemoryUtil.memGetShort(rect + 4) & 0xFFFF;
                int h = MemoryUtil.memGetShort(rect + 6) & 0xFFFF;
                if (w == 0 || h == 0) continue;
                ByteBuffer region = MemoryUtil.memAlloc(w * h * 4);
                try {
                    long out = MemoryUtil.memAddress(region);
                    for (int row = 0; row < h; row++) {
                        MemoryUtil.memCopy(pixels + ((long) (y + row) * width + x) * 4, out + (long) row * w * 4, (long) w * 4);
                    }
                    encoder.writeToTexture(target.texture(), region, 0, 0, x, y, w, h);
                } finally {
                    MemoryUtil.memFree(region);
                }
            }
        }
        MemoryUtil.memPutInt(texture + Natives.TEXTURE_STATUS, STATUS_OK);
    }

    private void destroy(long texture) {
        Texture removed = textures.remove(MemoryUtil.memGetLong(texture + Natives.TEXTURE_ID));
        if (removed != null) {
            removed.view().close();
            removed.texture().close();
        }
        MemoryUtil.memPutLong(texture + Natives.TEXTURE_ID, 0);
        boolean recreate = MemoryUtil.memGetByte(texture + Natives.TEXTURE_DESTROY_NEXT_FRAME) == 0
                && MemoryUtil.memGetAddress(texture + Natives.TEXTURE_PIXELS) != 0;
        MemoryUtil.memPutInt(texture + Natives.TEXTURE_STATUS, recreate ? STATUS_WANT_CREATE : STATUS_DESTROYED);
    }

    private static RenderPipeline.Builder pipeline(String location) {
        return RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
                .withLocation(Identifier.fromNamespaceAndPath(Moonlight.MOD_ID, location))
                .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
                .withCull(false);
    }

    private static boolean skipped(long command) {
        return MemoryUtil.memGetInt(command + Natives.COMMAND_ELEMENTS) == 0 || MemoryUtil.memGetAddress(command + Natives.COMMAND_CALLBACK) != 0;
    }

    private record Texture(GpuTexture texture, GpuTextureView view) {
    }

    private record Shape(Area area, float opacity, int color) {
    }

    private final class Frame {
        private final ImDrawData data;
        private final CommandEncoder encoder;
        private final RenderTarget target;
        private final GpuTextureView color;
        private final GpuBufferSlice vertices;
        private final GpuBufferSlice indices;
        private final GpuBufferSlice projection;
        private final GpuBufferSlice transforms;
        private final int firstIndex;
        private final GpuSampler sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
        private final GpuSampler nearest = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);
        private @Nullable RenderPass pass;
        private boolean drawn;
        private boolean dirty = true;
        private boolean blurred;

        private Frame(ImDrawData data, CommandEncoder encoder, RenderTarget target, GpuTextureView color, GpuBufferSlice vertices,
                      GpuBufferSlice indices, GpuBufferSlice projection, GpuBufferSlice transforms, int firstIndex) {
            this.data = data;
            this.encoder = encoder;
            this.target = target;
            this.color = color;
            this.vertices = vertices;
            this.indices = indices;
            this.projection = projection;
            this.transforms = transforms;
            this.firstIndex = firstIndex;
        }

        private void run() {
            open();
            try {
                int globalVertex = 0;
                int globalIndex = 0;
                for (int n = 0; n < Natives.listCount(data); n++) {
                    long list = Natives.list(data, n);
                    int count = Natives.commandCount(list);
                    long command = Natives.commands(list);
                    for (int c = 0; c < count; c++, command += Natives.COMMAND_SIZE) {
                        if (skipped(command)) continue;
                        long id = Natives.textureId(command);
                        if (id == Effects.SHADOW) {
                            int length = 1;
                            while (c + length < count && Natives.textureId(command + (long) length * Natives.COMMAND_SIZE) == Effects.SHADOW) length++;
                            shadow(list, command, length, globalVertex, globalIndex);
                            c += length - 1;
                            command += (long) (length - 1) * Natives.COMMAND_SIZE;
                        } else if (id == Effects.BLUR) {
                            blur(n, c, command, globalVertex, globalIndex);
                        } else {
                            Texture texture = textures.get(id);
                            Identifier path = texture == null ? Textures.path(id) : null;
                            if (texture != null) draw(pass(), command, globalVertex, globalIndex, texture.view(), sampler);
                            else if (path != null) draw(pass(), command, globalVertex, globalIndex,
                                    Minecraft.getInstance().getTextureManager().getTexture(path).getTextureView(), nearest);
                            else continue;
                            drawn = true;
                            dirty = true;
                        }
                    }
                    globalVertex += Natives.vertexCount(list);
                    globalIndex += Natives.indexCount(list);
                }
            } finally {
                close();
            }
        }

        private void shadow(long list, long command, int length, int globalVertex, int globalIndex) {
            PostProcess settings = Effects.settings();
            if (settings == null || !settings.shadow()) return;
            Area area = Area.EMPTY;
            float opacity = 0;
            int color = 0;
            for (int i = 0; i < length; i++) {
                long at = command + (long) i * Natives.COMMAND_SIZE;
                if (skipped(at)) continue;
                Shape shape = shape(list, at);
                area = area.union(shape.area());
                if (shape.opacity() > opacity) {
                    opacity = shape.opacity();
                    color = shape.color();
                }
            }
            if (area.isEmpty() || opacity <= 0) return;
            close();
            try {
                GpuTextureView fill = white(encoder);
                bloom.render(encoder, target, area, settings.bloomAmount(), settings.bloomOffset(), opacity, Natives.withAlpha(color, settings.shadowAlpha()), mask -> {
                    setup(mask, MASK);
                    for (int i = 0; i < length; i++) {
                        long at = command + (long) i * Natives.COMMAND_SIZE;
                        if (!skipped(at)) draw(mask, at, globalVertex, globalIndex, fill, sampler);
                    }
                });
                dirty = true;
            } catch (RuntimeException exception) {
                Effects.fail(exception);
            } finally {
                open();
            }
        }

        private void blur(int list, int index, long command, int globalVertex, int globalIndex) {
            PostProcess settings = Effects.settings();
            if (settings == null || !settings.blur()) return;
            if (dirty) {
                Area area = blurArea(list, index);
                close();
                try {
                    blurred = blur.bake(encoder, target, area, settings.blurAmount(), settings.blurOffset(), !drawn);
                } catch (RuntimeException exception) {
                    blurred = false;
                    Effects.fail(exception);
                } finally {
                    open();
                }
                dirty = false;
            }
            if (!blurred) return;
            RenderPass current = pass();
            setup(current, BLUR);
            draw(current, command, globalVertex, globalIndex, blur.view(), sampler);
            setup(current, PIPELINE);
        }

        private Area blurArea(int startList, int startCommand) {
            Area area = Area.EMPTY;
            for (int n = startList; n < Natives.listCount(data); n++) {
                long list = Natives.list(data, n);
                long command = Natives.commands(list);
                for (int c = 0; c < Natives.commandCount(list); c++, command += Natives.COMMAND_SIZE) {
                    if (n == startList && c < startCommand || skipped(command)) continue;
                    long id = Natives.textureId(command);
                    if (id == Effects.BLUR) area = area.union(shape(list, command).area());
                    else if (id != Effects.SHADOW) return area;
                }
            }
            return area;
        }

        private Shape shape(long list, long command) {
            long vertexBase = Natives.vertices(list) + (long) MemoryUtil.memGetInt(command + Natives.COMMAND_VERTEX_OFFSET) * Natives.VERTEX_SIZE;
            long indexBase = Natives.indices(list) + (long) MemoryUtil.memGetInt(command + Natives.COMMAND_INDEX_OFFSET) * indexType.bytes;
            float minX = Float.MAX_VALUE;
            float minY = Float.MAX_VALUE;
            float maxX = -Float.MAX_VALUE;
            float maxY = -Float.MAX_VALUE;
            int alpha = 0;
            int color = 0;
            for (int i = 0, n = MemoryUtil.memGetInt(command + Natives.COMMAND_ELEMENTS); i < n; i++) {
                long at = indexBase + (long) i * indexType.bytes;
                int index = indexType == IndexType.SHORT ? MemoryUtil.memGetShort(at) & 0xFFFF : MemoryUtil.memGetInt(at);
                long vertex = vertexBase + (long) index * Natives.VERTEX_SIZE;
                float x = MemoryUtil.memGetFloat(vertex);
                float y = MemoryUtil.memGetFloat(vertex + 4);
                minX = Math.min(minX, x);
                minY = Math.min(minY, y);
                maxX = Math.max(maxX, x);
                maxY = Math.max(maxY, y);
                int vertexColor = MemoryUtil.memGetInt(vertex + 16);
                if (vertexColor >>> 24 > alpha) {
                    alpha = vertexColor >>> 24;
                    color = vertexColor & 0xFFFFFF;
                }
            }
            Area clip = new Area(MemoryUtil.memGetFloat(command), MemoryUtil.memGetFloat(command + 4),
                    MemoryUtil.memGetFloat(command + 8), MemoryUtil.memGetFloat(command + 12));
            return new Shape(new Area(minX, minY, maxX, maxY).intersect(clip), alpha / 255.0F, color);
        }

        private void draw(RenderPass into, long command, int globalVertex, int globalIndex, GpuTextureView view, GpuSampler filter) {
            int width = target.width;
            int height = target.height;
            float minX = Math.max(0, MemoryUtil.memGetFloat(command));
            float minY = Math.max(0, MemoryUtil.memGetFloat(command + 4));
            float maxX = Math.min(width, MemoryUtil.memGetFloat(command + 8));
            float maxY = Math.min(height, MemoryUtil.memGetFloat(command + 12));
            int scissorWidth = (int) (maxX - minX);
            int scissorHeight = (int) (maxY - minY);
            if (scissorWidth <= 0 || scissorHeight <= 0) return;
            into.enableScissor((int) minX, (int) (height - maxY), scissorWidth, scissorHeight);
            into.setUniform("Sampler0", view, filter);
            into.drawIndexed(MemoryUtil.memGetInt(command + Natives.COMMAND_ELEMENTS), 1,
                    firstIndex + globalIndex + MemoryUtil.memGetInt(command + Natives.COMMAND_INDEX_OFFSET),
                    globalVertex + MemoryUtil.memGetInt(command + Natives.COMMAND_VERTEX_OFFSET), 0);
        }

        private void setup(RenderPass into, RenderPipeline pipeline) {
            RenderSystem.bindDefaultUniforms(into);
            into.setUniform("Projection", projection);
            into.setUniform("DynamicTransforms", transforms);
            into.setPipeline(RenderSystem.getCompiledPipeline(pipeline));
            into.setVertexBuffer(0, vertices);
            into.setIndexBuffer(indices.buffer(), indexType);
        }

        private void open() {
            RenderPass opened = encoder.createRenderPass(() -> "ImGui", color, Optional.empty());
            pass = opened;
            setup(opened, PIPELINE);
        }

        private void close() {
            RenderPass current = pass;
            pass = null;
            if (current != null) current.close();
        }

        private RenderPass pass() {
            RenderPass current = pass;
            if (current == null) throw new IllegalStateException("ImGui pass is closed");
            return current;
        }
    }
}
