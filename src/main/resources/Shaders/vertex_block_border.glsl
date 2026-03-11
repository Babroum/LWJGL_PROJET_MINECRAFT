#version 330 core
layout(location = 0) in int packedData;

uniform mat4 projection;
uniform mat4 view;
uniform vec3 blockPos;

void main()
{
    int x = (packedData >> 27) & 0x1F;
    int y = (packedData >> 22) & 0x1F;
    int z = (packedData >> 17) & 0x1F;
    vec3 position = vec3(float(x), float(y), float(z)) + blockPos;
    gl_Position = projection * view * vec4(position, 1.0);
}