#version 330 core

flat in int dir;
in vec2 uv;
in float skyLight;
in float blockLight;
in float fogDistance;

out vec4 FragColor;

uniform vec3 skyColor;
uniform float fogStart;
uniform float fogEnd;
uniform vec3 blockLightColor;
uniform sampler2DArray tex;

const int WATER_TEX_ID = 16;

// fonction déclarée en dehors de main()
float lightCurve(float l) {
    return pow(l, 1.4);
}

void main()
{
    float dirFactor = (dir == 4) ? 1.0 : 0.7;

    float normalizedSkyLight   = skyLight   / 15.0;
    float normalizedBlockLight = blockLight / 15.0;

    float sl = lightCurve(normalizedSkyLight);
    float bl = lightCurve(normalizedBlockLight);

    float lightIntensity = max(sl, bl);
    vec3 lightTint = mix(blockLightColor, vec3(1.0), step(bl, sl));
    vec3 lightColor = lightTint * lightIntensity;

    vec4 texColor = texture(tex, vec3(fract(uv), float(WATER_TEX_ID)));
    texColor.a = 0.5;

    float fogFactor = clamp((fogEnd - fogDistance) / (fogEnd - fogStart), 0.0, 1.0);
    vec3 litColor = texColor.rgb * lightColor * dirFactor;
    vec3 finalColor = mix(skyColor, litColor, fogFactor);
    FragColor = vec4(finalColor, texColor.a);
}