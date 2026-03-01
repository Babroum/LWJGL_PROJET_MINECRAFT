#version 330 core
layout(location = 0) in int packedData;

uniform mat4 projection;
uniform mat4 view;
uniform vec3 blockPos;

void main()
{
    int x = (packedData >> 27) & 0x1F;
    int y = (packedData >> 19) & 0xFF;
    int z = (packedData >> 14) & 0x1F;
    vec3 position = vec3(float(x), float(y), float(z)) + blockPos;
    gl_Position = projection * view * vec4(position, 1.0);
}