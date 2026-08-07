#version 330 core
layout (location = 0) in vec2 aPos;
layout (location = 1) in vec2 aUV;
layout (location = 2) in vec4 aRGBA;

uniform mat4 ortho;

out vec2 uv;
out vec4 RGBA;

void main(){
    gl_Position = ortho * vec4(aPos, 0.0, 1.0);
    uv = aUV;
    RGBA = aRGBA;
}