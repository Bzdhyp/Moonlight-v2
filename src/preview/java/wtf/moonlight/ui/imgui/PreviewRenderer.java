package wtf.moonlight.ui.imgui;

import imgui.ImDrawData;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;
import wtf.moonlight.module.impl.render.PostProcess;
import wtf.moonlight.ui.imgui.post.Area;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static org.lwjgl.opengl.GL33.*;

final class PreviewRenderer {
    private static final int STATUS_OK = 0;
    private static final int STATUS_DESTROYED = 1;
    private static final int STATUS_WANT_CREATE = 2;
    private static final int STATUS_WANT_UPDATES = 3;
    private static final int STATUS_WANT_DESTROY = 4;
    private static final String VERTEX = """
            #version 330 core
            layout(location = 0) in vec2 Position;
            layout(location = 1) in vec2 UV;
            layout(location = 2) in vec4 Color;
            uniform mat4 Projection;
            out vec2 uv;
            out vec4 color;
            void main() {
                uv = UV;
                color = Color;
                gl_Position = Projection * vec4(Position, 0.0, 1.0);
            }
            """;
    private static final String TEXTURED = """
            #version 330 core
            in vec2 uv;
            in vec4 color;
            uniform sampler2D Texture;
            out vec4 fragColor;
            void main() {
                fragColor = color * texture(Texture, uv);
            }
            """;
    private static final String COVERAGE = """
            #version 330 core
            in vec4 color;
            out vec4 fragColor;
            void main() {
                fragColor = vec4(color.a);
            }
            """;
    private static final String GLASS = """
            #version 330 core
            in vec4 color;
            uniform sampler2D Texture;
            uniform vec2 Size;
            out vec4 fragColor;
            void main() {
                fragColor = vec4(texture(Texture, gl_FragCoord.xy / Size).rgb, color.a);
            }
            """;
    private static final String BACKDROP = """
            #version 330 core
            uniform sampler2D Texture;
            uniform vec4 Fit;
            out vec4 fragColor;
            void main() {
                vec2 uv = (gl_FragCoord.xy - Fit.xy) / Fit.zw;
                fragColor = texture(Texture, vec2(uv.x, 1.0 - uv.y));
            }
            """;

    private final Map<Long, Integer> textures = new HashMap<>();
    private final Map<Identifier, Integer> game = new HashMap<>();
    private final Function<Identifier, BufferedImage> images;
    private final Program ui = new Program(VERTEX, TEXTURED);
    private final Program coverage = new Program(VERTEX, COVERAGE);
    private final Program glass = new Program(VERTEX, GLASS);
    private final Program background = new Program(PreviewPost.SCREEN, BACKDROP);
    private final PreviewPost post = new PreviewPost();
    private final Target scene = new Target();
    private final int vao = glGenVertexArrays();
    private final int vbo = glGenBuffers();
    private final int ebo = glGenBuffers();
    private final int indexSize = ImDrawData.sizeOfImDrawIdx();
    private final int indexType = indexSize == 2 ? GL_UNSIGNED_SHORT : GL_UNSIGNED_INT;
    private final int backdropWidth;
    private final int backdropHeight;
    private final int backdrop;
    private final float[] projection = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, 0, -1, 1, 0, 1};
    private int width;
    private int height;
    private boolean dirty;
    private boolean blurred;
    private long nextId = 1;

    PreviewRenderer(Function<Identifier, BufferedImage> images, @Nullable BufferedImage backdrop) {
        this.images = images;
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glEnableVertexAttribArray(0);
        glEnableVertexAttribArray(1);
        glEnableVertexAttribArray(2);
        glVertexAttribPointer(0, 2, GL_FLOAT, false, Natives.VERTEX_SIZE, 0);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, Natives.VERTEX_SIZE, 8);
        glVertexAttribPointer(2, 4, GL_UNSIGNED_BYTE, true, Natives.VERTEX_SIZE, 16);
        glBindVertexArray(0);
        backdropWidth = backdrop != null ? backdrop.getWidth() : 0;
        backdropHeight = backdrop != null ? backdrop.getHeight() : 0;
        this.backdrop = backdrop != null ? upload(backdrop, GL_LINEAR) : 0;
    }

    void render(ImDrawData data, int width, int height) {
        updateTextures(data);
        this.width = width;
        this.height = height;
        projection[0] = 2.0F / width;
        projection[5] = -2.0F / height;
        scene.ensure(width, height).bind();
        glDisable(GL_SCISSOR_TEST);
        glDisable(GL_BLEND);
        glDisable(GL_CULL_FACE);
        glDisable(GL_DEPTH_TEST);
        glClearColor(0.07F, 0.08F, 0.11F, 1);
        glClear(GL_COLOR_BUFFER_BIT);
        if (backdrop != 0) {
            float fill = Math.max((float) width / backdropWidth, (float) height / backdropHeight);
            background.use();
            background.vec4("Fit", (width - backdropWidth * fill) * 0.5F, (height - backdropHeight * fill) * 0.5F,
                    backdropWidth * fill, backdropHeight * fill);
            post.screen(background, backdrop);
        }
        dirty = true;
        blurred = false;
        begin();
        for (int n = 0; n < Natives.listCount(data); n++) {
            long list = Natives.list(data, n);
            glBindBuffer(GL_ARRAY_BUFFER, vbo);
            nglBufferData(GL_ARRAY_BUFFER, (long) Natives.vertexCount(list) * Natives.VERTEX_SIZE, Natives.vertices(list), GL_STREAM_DRAW);
            nglBufferData(GL_ELEMENT_ARRAY_BUFFER, (long) Natives.indexCount(list) * indexSize, Natives.indices(list), GL_STREAM_DRAW);
            int count = Natives.commandCount(list);
            long command = Natives.commands(list);
            for (int c = 0; c < count; c++, command += Natives.COMMAND_SIZE) {
                if (skipped(command)) continue;
                long id = Natives.textureId(command);
                if (id == Effects.SHADOW) {
                    int length = 1;
                    while (c + length < count && Natives.textureId(command + (long) length * Natives.COMMAND_SIZE) == Effects.SHADOW) length++;
                    shadow(list, command, length);
                    c += length - 1;
                    command += (long) (length - 1) * Natives.COMMAND_SIZE;
                } else if (id == Effects.BLUR) {
                    blur(data, n, c, command);
                } else {
                    Integer texture = texture(id);
                    if (texture == null) continue;
                    draw(command, texture);
                    dirty = true;
                }
            }
        }
        scene.present();
    }

    static void filter(int filter) {
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, filter);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, filter);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
    }

    private void begin() {
        scene.bind();
        glEnable(GL_BLEND);
        glBlendEquation(GL_FUNC_ADD);
        glBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
        ui.use();
        ui.matrix("Projection", projection);
        glActiveTexture(GL_TEXTURE0);
        glBindVertexArray(vao);
    }

    private void shadow(long list, long command, int length) {
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
        post.shadow(scene, area, settings.bloomAmount(), settings.bloomOffset(), opacity, Natives.withAlpha(color, settings.shadowAlpha()), () -> {
            coverage.use();
            coverage.matrix("Projection", projection);
            glEnable(GL_BLEND);
            glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
            glBindVertexArray(vao);
            for (int i = 0; i < length; i++) {
                long at = command + (long) i * Natives.COMMAND_SIZE;
                if (!skipped(at)) draw(at, 0);
            }
        });
        dirty = true;
        begin();
    }

    private void blur(ImDrawData data, int list, int index, long command) {
        PostProcess settings = Effects.settings();
        if (settings == null || !settings.blur()) return;
        if (dirty) {
            blurred = post.blur(scene, blurArea(data, list, index), settings.blurAmount(), settings.blurOffset());
            dirty = false;
            begin();
        }
        if (!blurred) return;
        glass.use();
        glass.matrix("Projection", projection);
        glass.vec2("Size", width, height);
        draw(command, post.blurred());
        ui.use();
    }

    private Area blurArea(ImDrawData data, int startList, int startCommand) {
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
        long indexBase = Natives.indices(list) + (long) MemoryUtil.memGetInt(command + Natives.COMMAND_INDEX_OFFSET) * indexSize;
        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;
        int alpha = 0;
        int color = 0;
        for (int i = 0, n = MemoryUtil.memGetInt(command + Natives.COMMAND_ELEMENTS); i < n; i++) {
            long at = indexBase + (long) i * indexSize;
            int index = indexSize == 2 ? MemoryUtil.memGetShort(at) & 0xFFFF : MemoryUtil.memGetInt(at);
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

    private void draw(long command, int texture) {
        float minX = Math.max(0, MemoryUtil.memGetFloat(command));
        float minY = Math.max(0, MemoryUtil.memGetFloat(command + 4));
        float maxX = Math.min(width, MemoryUtil.memGetFloat(command + 8));
        float maxY = Math.min(height, MemoryUtil.memGetFloat(command + 12));
        if (maxX <= minX || maxY <= minY) return;
        glEnable(GL_SCISSOR_TEST);
        glScissor((int) minX, (int) (height - maxY), (int) (maxX - minX), (int) (maxY - minY));
        glBindTexture(GL_TEXTURE_2D, texture);
        glDrawElementsBaseVertex(GL_TRIANGLES, MemoryUtil.memGetInt(command + Natives.COMMAND_ELEMENTS), indexType,
                (long) MemoryUtil.memGetInt(command + Natives.COMMAND_INDEX_OFFSET) * indexSize,
                MemoryUtil.memGetInt(command + Natives.COMMAND_VERTEX_OFFSET));
    }

    private @Nullable Integer texture(long id) {
        Integer texture = textures.get(id);
        if (texture != null) return texture;
        Identifier path = Textures.path(id);
        return path == null ? null : game.computeIfAbsent(path, key -> upload(images.apply(key),
                key.getPath().startsWith("textures/entity/") ? GL_NEAREST : GL_LINEAR));
    }

    private void updateTextures(ImDrawData data) {
        long vector = Natives.textures(data);
        if (vector == 0) return;
        long items = MemoryUtil.memGetAddress(vector + 8);
        for (int i = 0, n = MemoryUtil.memGetInt(vector); i < n; i++) {
            long texture = MemoryUtil.memGetAddress(items + 8L * i);
            switch (MemoryUtil.memGetInt(texture + Natives.TEXTURE_STATUS)) {
                case STATUS_WANT_CREATE -> create(texture);
                case STATUS_WANT_UPDATES -> update(texture);
                case STATUS_WANT_DESTROY -> {
                    if (MemoryUtil.memGetInt(texture + Natives.TEXTURE_UNUSED_FRAMES) > 0) destroy(texture);
                }
                default -> {
                }
            }
        }
    }

    private void create(long texture) {
        int name = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, name);
        filter(GL_LINEAR);
        nglTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, MemoryUtil.memGetInt(texture + Natives.TEXTURE_WIDTH), MemoryUtil.memGetInt(texture + Natives.TEXTURE_HEIGHT),
                0, GL_RGBA, GL_UNSIGNED_BYTE, MemoryUtil.memGetAddress(texture + Natives.TEXTURE_PIXELS));
        long id = nextId++;
        textures.put(id, name);
        MemoryUtil.memPutLong(texture + Natives.TEXTURE_ID, id);
        MemoryUtil.memPutInt(texture + Natives.TEXTURE_STATUS, STATUS_OK);
    }

    private void update(long texture) {
        Integer name = textures.get(MemoryUtil.memGetLong(texture + Natives.TEXTURE_ID));
        if (name != null) {
            int width = MemoryUtil.memGetInt(texture + Natives.TEXTURE_WIDTH);
            long pixels = MemoryUtil.memGetAddress(texture + Natives.TEXTURE_PIXELS);
            long rects = MemoryUtil.memGetAddress(texture + Natives.TEXTURE_UPDATES + 8);
            glBindTexture(GL_TEXTURE_2D, name);
            glPixelStorei(GL_UNPACK_ROW_LENGTH, width);
            for (int i = 0, n = MemoryUtil.memGetInt(texture + Natives.TEXTURE_UPDATES); i < n; i++) {
                long rect = rects + 8L * i;
                int x = MemoryUtil.memGetShort(rect) & 0xFFFF;
                int y = MemoryUtil.memGetShort(rect + 2) & 0xFFFF;
                int w = MemoryUtil.memGetShort(rect + 4) & 0xFFFF;
                int h = MemoryUtil.memGetShort(rect + 6) & 0xFFFF;
                if (w == 0 || h == 0) continue;
                nglTexSubImage2D(GL_TEXTURE_2D, 0, x, y, w, h, GL_RGBA, GL_UNSIGNED_BYTE, pixels + ((long) y * width + x) * 4);
            }
            glPixelStorei(GL_UNPACK_ROW_LENGTH, 0);
        }
        MemoryUtil.memPutInt(texture + Natives.TEXTURE_STATUS, STATUS_OK);
    }

    private void destroy(long texture) {
        Integer name = textures.remove(MemoryUtil.memGetLong(texture + Natives.TEXTURE_ID));
        if (name != null) glDeleteTextures(name);
        MemoryUtil.memPutLong(texture + Natives.TEXTURE_ID, 0);
        boolean recreate = MemoryUtil.memGetByte(texture + Natives.TEXTURE_DESTROY_NEXT_FRAME) == 0
                && MemoryUtil.memGetAddress(texture + Natives.TEXTURE_PIXELS) != 0;
        MemoryUtil.memPutInt(texture + Natives.TEXTURE_STATUS, recreate ? STATUS_WANT_CREATE : STATUS_DESTROYED);
    }

    private static boolean skipped(long command) {
        return MemoryUtil.memGetInt(command + Natives.COMMAND_ELEMENTS) == 0 || MemoryUtil.memGetAddress(command + Natives.COMMAND_CALLBACK) != 0;
    }

    private static int upload(BufferedImage image, int filter) {
        int width = image.getWidth();
        int height = image.getHeight();
        int[] argb = image.getRGB(0, 0, width, height, null, 0, width);
        ByteBuffer pixels = MemoryUtil.memAlloc(argb.length * 4);
        for (int color : argb) pixels.put((byte) (color >> 16)).put((byte) (color >> 8)).put((byte) color).put((byte) (color >>> 24));
        pixels.flip();
        int name = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, name);
        filter(filter);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, pixels);
        MemoryUtil.memFree(pixels);
        return name;
    }

    private record Shape(Area area, float opacity, int color) {
    }
}
