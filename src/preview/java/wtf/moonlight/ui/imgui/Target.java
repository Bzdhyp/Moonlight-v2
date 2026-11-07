package wtf.moonlight.ui.imgui;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL33.*;

final class Target {
    private int texture;
    private int framebuffer;
    private int width;
    private int height;

    Target ensure(int width, int height) {
        if (framebuffer == 0) {
            texture = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, texture);
            PreviewRenderer.filter(GL_LINEAR);
            framebuffer = glGenFramebuffers();
        }
        if (this.width != width || this.height != height) {
            this.width = width;
            this.height = height;
            glBindTexture(GL_TEXTURE_2D, texture);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer) null);
            glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, texture, 0);
        }
        return this;
    }

    void bind() {
        glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);
        glViewport(0, 0, width, height);
    }

    void present() {
        glDisable(GL_SCISSOR_TEST);
        glBindFramebuffer(GL_READ_FRAMEBUFFER, framebuffer);
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, 0);
        glBlitFramebuffer(0, 0, width, height, 0, 0, width, height, GL_COLOR_BUFFER_BIT, GL_NEAREST);
    }

    int texture() {
        return texture;
    }

    int width() {
        return width;
    }

    int height() {
        return height;
    }
}
