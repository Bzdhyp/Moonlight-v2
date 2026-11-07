#version 330
#extension GL_ARB_separate_shader_objects : require

uniform sampler2D InSampler;

layout(std140) uniform PostConfig {
    vec4 Step;
    vec4 Tint;
};

layout(location = 0) in vec2 texCoord;

layout(location = 0) out vec4 fragColor;

void main() {
    fragColor = vec4(Tint.rgb, texture(InSampler, texCoord).r * Tint.a);
}
