package wtf.moonlight.ui.imgui;

import static org.lwjgl.opengl.GL33.*;

final class Program {
    private final int id;

    Program(String vertex, String fragment) {
        id = glCreateProgram();
        glAttachShader(id, shader(GL_VERTEX_SHADER, vertex));
        glAttachShader(id, shader(GL_FRAGMENT_SHADER, fragment));
        glLinkProgram(id);
        if (glGetProgrami(id, GL_LINK_STATUS) == GL_FALSE) throw new IllegalStateException(glGetProgramInfoLog(id));
    }

    void use() {
        glUseProgram(id);
    }

    void matrix(String name, float[] values) {
        glUniformMatrix4fv(glGetUniformLocation(id, name), false, values);
    }

    void vec2(String name, float x, float y) {
        glUniform2f(glGetUniformLocation(id, name), x, y);
    }

    void vec4(String name, float x, float y, float z, float w) {
        glUniform4f(glGetUniformLocation(id, name), x, y, z, w);
    }

    void sampler(String name, int unit) {
        glUseProgram(id);
        glUniform1i(glGetUniformLocation(id, name), unit);
    }

    void block(String name, int binding) {
        int index = glGetUniformBlockIndex(id, name);
        if (index != GL_INVALID_INDEX) glUniformBlockBinding(id, index, binding);
    }

    private static int shader(int type, String source) {
        int shader = glCreateShader(type);
        glShaderSource(shader, source);
        glCompileShader(shader);
        if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) throw new IllegalStateException(glGetShaderInfoLog(shader));
        return shader;
    }
}
