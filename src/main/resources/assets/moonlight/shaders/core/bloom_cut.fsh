#version 330
#extension GL_ARB_separate_shader_objects : require

uniform sampler2D InSampler;
uniform sampler2D MaskSampler;

layout(std140) uniform PostConfig {
    vec4 Step;
    vec4 Tint;
};

layout(location = 0) in vec2 texCoord;

layout(location = 0) out vec4 fragColor;

void main() {
    vec2 s = Step.xy;
    float sum = texture(InSampler, texCoord + vec2(-2.0 * s.x, 0.0)).r;
    sum += texture(InSampler, texCoord + vec2(-s.x, s.y)).r * 2.0;
    sum += texture(InSampler, texCoord + vec2(0.0, 2.0 * s.y)).r;
    sum += texture(InSampler, texCoord + s).r * 2.0;
    sum += texture(InSampler, texCoord + vec2(2.0 * s.x, 0.0)).r;
    sum += texture(InSampler, texCoord + vec2(s.x, -s.y)).r * 2.0;
    sum += texture(InSampler, texCoord + vec2(0.0, -2.0 * s.y)).r;
    sum += texture(InSampler, texCoord - s).r * 2.0;
    float shadow = sum / 12.0 * (1.0 - min(texture(MaskSampler, texCoord).r * Step.z, 1.0));
    fragColor = vec4(shadow, shadow, shadow, 1.0);
}
