#version 330 core
layout(location = 0) in int packedData;
layout(location = 1) in int packedData2;

flat out int dir;
out vec2 uv;
out float skyLight;
out float blockLight;
out float fogDistance;
flat out int liquid;

uniform mat4 projection;
uniform mat4 view;
uniform mat4 model;
uniform float time;

void main()
{
    int x      = (packedData >> 23) & 0x1FF;
    int y      = (packedData >> 14) & 0x1FF;
    int z      = (packedData >> 5)  & 0x1FF;
    int u_bit  = (packedData >> 4)  & 0x1;
    int v_bit  = (packedData >> 3)  & 0x1;
    dir        =  packedData        & 0x7;   

    int w      = (packedData2 >> 27) & 0x1F;
    int h      = (packedData2 >> 22) & 0x1F;
    int d      = (packedData2 >> 17) & 0x1F;
    int sky    = (packedData2 >> 11) & 0xF;
    int block  = (packedData2 >> 7)  & 0xF;
    
    vec3 position = vec3(float(x) / 16, float(y) / 16, float(z) / 16);
    gl_Position = projection * view * model * vec4(position, 1.0);
    
    int baseX = (u_bit == 1 && (dir == 0 || dir == 1 || dir == 4 || dir == 5)) ? x - w : x;
    int baseZ = (u_bit == 1 && (dir == 2 || dir == 3)) ? z - d
              : (v_bit == 1 && (dir == 4 || dir == 5)) ? z - d
              : z;
    int baseY = (v_bit == 1 && (dir == 0 || dir == 1 || dir == 2 || dir == 3)) ? y - h : y;

    int offsetU, offsetV, uExtent, vExtent;

    if (dir == 0 || dir == 1){
        int offU = (u_bit == 1) ? (x - w) & 0xF : x & 0xF;
        int offV = (v_bit == 1) ? (y - h) & 0xF : y & 0xF;

        uv = vec2(u_bit == 0 ? float(offU) / 16.0 : float(offU + w) / 16.0,
                v_bit == 0 ? float(offV) / 16.0 : float(offV + h) / 16.0);

    }else if (dir == 2 || dir == 3){
        int offU = (u_bit == 1) ? (z - d) & 0xF : z & 0xF;
        int offV = (v_bit == 1) ? (y - h) & 0xF : y & 0xF;

        uv = vec2(u_bit == 0 ? float(offU) / 16.0 : float(offU + d) / 16.0,
                v_bit == 0 ? float(offV) / 16.0 : float(offV + h) / 16.0);

    }else{
        int offU = (u_bit == 1) ? (x - w) & 0xF : x & 0xF;
        int offV = (v_bit == 1) ? (z - d) & 0xF : z & 0xF;

        uv = vec2(u_bit == 0 ? float(offU) / 16.0 : float(offU + w) / 16.0,
                v_bit == 0 ? float(offV) / 16.0 : float(offV + d) / 16.0);
    }

    if (dir == 0 || dir == 1 || dir == 2 || dir == 3) {
        uv.y = 1.0 - uv.y;
    }

    skyLight = float(sky);
    blockLight = float(block);
    fogDistance = length((view * model * vec4(position, 1.0)).xyz);
}