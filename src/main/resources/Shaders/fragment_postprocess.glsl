#version 330 core
in vec2 uv;
out vec4 FragColor;

uniform sampler2D screenTex;
uniform float saturationAmount;

vec3 adjustSaturation(vec3 color, float amount) {
    float luma = dot(color, vec3(0.2126, 0.7152, 0.0722));
    return mix(vec3(luma), color, amount);
}

void main() {
    vec2 centered = uv - vec2(0.5);
    float dist = length(centered);

    // --- aberration chromatique ---
    float amt = 0.006 * dist;
    vec3 color;
    color.r = texture(screenTex, uv - centered * amt).r;
    color.g = texture(screenTex, uv).g;
    color.b = texture(screenTex, uv + centered * amt).b;

    // --- saturation ---
    color = adjustSaturation(color, saturationAmount);
    color = clamp(color, 0.0, 1.0);

    // --- vignette (ton code existant) ---
    float vignette = smoothstep(0.8, 0.6, dist);
    color *= vignette;

    // --- dithering (anti-banding) ---
    float dither = (fract(sin(dot(gl_FragCoord.xy, vec2(12.9898,78.233))) * 43758.5453) - 0.5) / 255.0; // hash devenu quasi-standard, copié dans énormément de shaders
    color += dither;

    FragColor = vec4(color, 1.0);
}