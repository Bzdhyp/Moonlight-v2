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
    vec2 s = Step.xy;
    vec3 sum = texture(InSampler, texCoord + vec2(-2.0 * s.x, 0.0)).rgb;
    sum += texture(InSampler, texCoord + vec2(-s.x, s.y)).rgb * 2.0;
    sum += texture(InSampler, texCoord + vec2(0.0, 2.0 * s.y)).rgb;
    sum += texture(InSampler, texCoord + s).rgb * 2.0;
    sum += texture(InSampler, texCoord + vec2(2.0 * s.x, 0.0)).rgb;
    sum += texture(InSampler, texCoord + vec2(s.x, -s.y)).rgb * 2.0;
    sum += texture(InSampler, texCoord + vec2(0.0, -2.0 * s.y)).rgb;
    sum += texture(InSampler, texCoord - s).rgb * 2.0;
    fragColor = vec4(sum / 12.0, 1.0);
}
