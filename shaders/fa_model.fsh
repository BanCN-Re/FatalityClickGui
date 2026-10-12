#version 150

// FA preview composite: the offscreen player model over its ESP look. The model texture is
// premultiplied (rendered into a cleared target), so the halo is built from its alpha alone and
// only ever shows outside the body, the way the in-world entity glow does.

uniform sampler2D ModelSampler;

layout(std140) uniform FaModel {
    vec4 Rect;   // model target in framebuffer pixels: x, y (bottom-left), width, height
    vec4 ColorA; // halo color at the left edge
    vec4 ColorB; // halo color at the right edge
    vec4 Params; // x: mode (0 none, 1 glow, 2 outline), y: glow radius px, z: opacity, w: outline px
};

out vec4 fragColor;

const int TAPS = 56;
const float GOLDEN_ANGLE = 2.39996323;

vec4 sampleModel(vec2 uv) {
    if (uv.x < 0.0 || uv.y < 0.0 || uv.x > 1.0 || uv.y > 1.0) return vec4(0.0);
    return texture(ModelSampler, uv);
}

void main() {
    vec2 uv = (gl_FragCoord.xy - Rect.xy) / Rect.zw;
    vec2 texel = 1.0 / Rect.zw;
    vec4 model = sampleModel(uv);
    vec4 halo = vec4(0.0);

    if (Params.x > 1.5) {
        // ESP without its shader: the vanilla look, a crisp white line just outside the body.
        if (model.a < 0.5) {
            float reach = max(1.0, Params.w);
            float hit = 0.0;
            for (int i = 0; i < 8; i++) {
                float angle = float(i) * 0.78539816;
                vec2 direction = vec2(cos(angle), sin(angle));
                hit = max(hit, sampleModel(uv + direction * reach * texel).a);
                hit = max(hit, sampleModel(uv + direction * reach * 0.5 * texel).a);
            }
            float line = smoothstep(0.25, 0.6, hit);
            halo = vec4(vec3(1.0) * line, line);
        }
    } else if (Params.x > 0.5) {
        // ESP shader: a soft halo in the theme colors. A golden-angle disc with Gaussian weights
        // stands in for the blur chain used in the world; at preview size it is just as smooth.
        float radius = max(1.0, Params.y);
        float sigma = radius / 2.6;
        float sum = 0.0;
        float weights = 0.0;
        for (int i = 0; i < TAPS; i++) {
            float r = sqrt((float(i) + 0.5) / float(TAPS)) * radius;
            float theta = float(i) * GOLDEN_ANGLE;
            float weight = exp(-(r * r) / (2.0 * sigma * sigma));
            sum += sampleModel(uv + vec2(cos(theta), sin(theta)) * r * texel).a * weight;
            weights += weight;
        }
        float coverage = sum / max(weights, 1e-4);
        // Same tone curve as the entity glow composite (exposure 3).
        float toned = 1.0 - exp(-coverage * 3.0);
        float alpha = clamp(toned * (1.0 - model.a), 0.0, 1.0);
        vec3 color = mix(ColorA.rgb, ColorB.rgb, clamp(uv.x, 0.0, 1.0));
        halo = vec4(color * alpha, alpha);
    }

    vec4 result = model + halo * (1.0 - model.a);
    if (result.a < 0.002) discard;
    fragColor = result * Params.z;
}
