#version 330 core
in vec2 uv;
out vec4 FragColor;

uniform sampler2D screenTex;

void main() {
    vec3 color = texture(screenTex, uv).rgb;

    // distance au centre de l'écran (0.5, 0.5)
    vec2 centered = uv - vec2(0.5);
    float dist = length(centered);

    // assombrit vers les bords
    float vignette = smoothstep(0.8, 0.6, dist);
    color *= vignette;

    FragColor = vec4(color, 1.0);
}