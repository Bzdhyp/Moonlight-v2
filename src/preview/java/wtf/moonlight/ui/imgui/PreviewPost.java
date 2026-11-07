package wtf.moonlight.ui.imgui;

import org.lwjgl.system.MemoryStack;
import wtf.moonlight.ui.imgui.post.Area;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import static org.lwjgl.opengl.GL33.*;

final class PreviewPost {
    static final String SCREEN = """
            #version 330
            #extension GL_ARB_separate_shader_objects : require
            layout(location = 0) out vec2 texCoord;
            void main() {
                vec2 uv = vec2((gl_VertexID << 1) & 2, gl_VertexID & 2);
                gl_Position = vec4(uv * 2.0 - 1.0, 0.0, 1.0);
                texCoord = uv;
            }
            """;
    private static final int MAX_LEVELS = 8;
    private static final int MARGIN = 2;
    private static final float MAX_STEP = 2;

    private final Program down = pass("blur_down");
    private final Program up = pass("blur_up");
    private final Program cut = pass("bloom_cut");
    private final Program tint = pass("shadow");
    private final int uniforms = glGenBuffers();
    private final int empty = glGenVertexArrays();
    private final Target[] blurLevels = targets(MAX_LEVELS);
    private final Target[] bloomLevels = targets(MAX_LEVELS + 3);
    private final Target blurred = new Target();
    private final Target mask = new Target();
    private final Target shadow = new Target();

    PreviewPost() {
        glBindBuffer(GL_UNIFORM_BUFFER, uniforms);
        glBufferData(GL_UNIFORM_BUFFER, 32, GL_DYNAMIC_DRAW);
        glBindBufferBase(GL_UNIFORM_BUFFER, 0, uniforms);
    }

    boolean blur(Target scene, Area area, int amount, int offset) {
        int width = scene.width();
        int height = scene.height();
        int passes = Math.clamp(amount, 1, MAX_LEVELS);
        Area region = expand(area, 3 * passes * offset + (2 << passes) + 2, width, height);
        if (region.isEmpty()) return false;
        float stepX = offset / (float) width;
        float stepY = offset / (float) height;
        int source = scene.texture();
        for (int k = 1; k <= passes; k++) {
            Target level = blurLevels[k - 1].ensure(Math.max(1, width >> k), Math.max(1, height >> k));
            run(down, level, region, width, height, stepX, stepY, source, 0, 0);
            source = level.texture();
        }
        for (int k = passes - 1; k >= 1; k--) {
            Target level = blurLevels[k - 1].ensure(Math.max(1, width >> k), Math.max(1, height >> k));
            run(up, level, region, width, height, stepX, stepY, source, 0, 0);
            source = level.texture();
        }
        run(up, blurred.ensure(width, height), region, width, height, stepX * 0.5F, stepY * 0.5F, source, 0, 0);
        return true;
    }

    int blurred() {
        return blurred.texture();
    }

    void shadow(Target scene, Area area, int amount, int offset, float opacity, int color, Runnable shapes) {
        int width = scene.width();
        int height = scene.height();
        int passes = Math.clamp(amount, 1, MAX_LEVELS);
        float step = Math.max(1, offset);
        while (step > MAX_STEP) {
            step /= 2;
            passes++;
        }
        Area region = expand(area, step * (4 << passes) + 2, width, height);
        if (region.isEmpty()) return;
        mask.ensure(width, height).bind();
        glDisable(GL_SCISSOR_TEST);
        glClearColor(0, 0, 0, 0);
        glClear(GL_COLOR_BUFFER_BIT);
        shapes.run();
        int source = mask.texture();
        for (int k = 1; k <= passes; k++) {
            Target level = bloomLevels[k - 1].ensure(Math.max(1, width >> k), Math.max(1, height >> k));
            run(down, level, region, width, height, step / level.width(), step / level.height(), source, 0, 0);
            source = level.texture();
        }
        for (int k = passes - 1; k >= 1; k--) {
            Target level = bloomLevels[k - 1].ensure(Math.max(1, width >> k), Math.max(1, height >> k));
            run(up, level, region, width, height, step / level.width(), step / level.height(), source, 0, 0);
            source = level.texture();
        }
        run(cut, shadow.ensure(width, height), region, width, height, step / width, step / height, source, mask.texture(), 1 / opacity);
        scene.bind();
        if (!scissor(region, width, height, width, height, 0)) return;
        glEnable(GL_BLEND);
        glBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
        config(0, 0, 0, color);
        draw(tint, shadow.texture(), 0);
    }

    void screen(Program program, int texture) {
        program.use();
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, texture);
        glBindVertexArray(empty);
        glDrawArrays(GL_TRIANGLES, 0, 3);
    }

    private void run(Program program, Target out, Area area, int width, int height, float stepX, float stepY, int in, int maskTexture, float cutoff) {
        out.bind();
        glDisable(GL_SCISSOR_TEST);
        glClearColor(0, 0, 0, 0);
        glClear(GL_COLOR_BUFFER_BIT);
        if (!scissor(area, out.width(), out.height(), width, height, out.width() == width ? 0 : MARGIN)) return;
        glDisable(GL_BLEND);
        config(stepX, stepY, cutoff, 0);
        draw(program, in, maskTexture);
    }

    private void draw(Program program, int in, int maskTexture) {
        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, maskTexture);
        screen(program, in);
    }

    private void config(float stepX, float stepY, float cutoff, int color) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            glBindBuffer(GL_UNIFORM_BUFFER, uniforms);
            glBufferSubData(GL_UNIFORM_BUFFER, 0, stack.floats(stepX, stepY, cutoff, 0,
                    (color & 0xFF) / 255.0F, (color >>> 8 & 0xFF) / 255.0F, (color >>> 16 & 0xFF) / 255.0F, (color >>> 24) / 255.0F));
        }
    }

    private static boolean scissor(Area area, int targetWidth, int targetHeight, int width, int height, int margin) {
        float scaleX = targetWidth / (float) width;
        float scaleY = targetHeight / (float) height;
        int x0 = Math.max(0, (int) Math.floor(area.minX() * scaleX) - margin);
        int y0 = Math.max(0, (int) Math.floor(area.minY() * scaleY) - margin);
        int x1 = Math.min(targetWidth, (int) Math.ceil(area.maxX() * scaleX) + margin);
        int y1 = Math.min(targetHeight, (int) Math.ceil(area.maxY() * scaleY) + margin);
        if (x1 <= x0 || y1 <= y0) return false;
        glEnable(GL_SCISSOR_TEST);
        glScissor(x0, targetHeight - y1, x1 - x0, y1 - y0);
        return true;
    }

    private static Area expand(Area area, float pad, int width, int height) {
        return new Area(area.minX() - pad, area.minY() - pad, area.maxX() + pad, area.maxY() + pad).intersect(new Area(0, 0, width, height));
    }

    private static Target[] targets(int count) {
        Target[] targets = new Target[count];
        for (int i = 0; i < count; i++) targets[i] = new Target();
        return targets;
    }

    private static Program pass(String name) {
        String resource = "/assets/moonlight/shaders/core/" + name + ".fsh";
        String source;
        try (InputStream stream = PreviewPost.class.getResourceAsStream(resource)) {
            if (stream == null) throw new IOException("Missing " + resource);
            source = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
        Program program = new Program(SCREEN, source);
        program.block("PostConfig", 0);
        program.sampler("InSampler", 0);
        program.sampler("MaskSampler", 1);
        return program;
    }
}
