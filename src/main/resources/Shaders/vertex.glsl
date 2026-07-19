// Le vertex shader reçoit en entrée les positions des sommets, leurs couleurs, leurs coordonnées de texture,
// et produit en sortie la position finale du sommet sur l'écran, 
//ainsi que toutes les données qui seront transmises au fragment shader.


#version 330 core
layout(location = 0) in int packedData;
layout(location = 1) in int packedData2;
layout(location = 2) in int textureId;

flat out int texId;
flat out int dir;
flat out int isBorder;
out vec2 uv;
out float AO;
out float skyLight;
out float blockLight;
out float fogDistance;

uniform mat4 projection;
uniform mat4 view;
uniform mat4 model;
uniform float time;

void main()
{
    // unpack les données 
    // donc décale tout et applique un masque pour ne pas récupèrer les valeurs des autres variables
    int x      = (packedData >> 23) & 0x1FF;
    int y      = (packedData >> 14) & 0x1FF;
    int z      = (packedData >> 5)  & 0x1FF;
    int u_bit  = (packedData >> 4)  & 0x1;
    int v_bit  = (packedData >> 3)  & 0x1;
    dir        =  packedData        & 0x7;    

    int w      = (packedData2 >> 27) & 0x1F;
    int h      = (packedData2 >> 22) & 0x1F;
    int d      = (packedData2 >> 17) & 0x1F;
    int ao     = (packedData2 >> 15) & 0x3;
    int sky    = (packedData2 >> 11) & 0xF;
    int block  = (packedData2 >> 7)  & 0xF;

    isBorder = (textureId == 0 && u_bit == 0 && v_bit == 0) ? 1 : 0; // détecte bordure

    vec3 position = vec3(float(x) / 16, float(y) / 16, float(z) / 16);

    // === vertex waving pour le feuillage ===
    const int LEAVES_TEX_ID = 14;
    if (textureId == LEAVES_TEX_ID) {
        float waveStrength = 0.01;
        position.x += sin(time * 2.0 + position.x * 0.5 + position.z * 0.5) * waveStrength;
        position.z += cos(time * 1.7 + position.x * 0.5 + position.z * 0.5) * waveStrength;
    }

    gl_Position = projection * view * model * vec4(position, 1.0); // un vec3 est utilisé pour construire un vec4

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
        
    AO = float(ao);
    skyLight = float(sky);
    blockLight = float(block);
    fogDistance = length((view * model * vec4(position, 1.0)).xyz);
    texId = textureId;
}