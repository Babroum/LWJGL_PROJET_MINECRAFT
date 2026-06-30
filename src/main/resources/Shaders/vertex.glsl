// Le vertex shader reçoit en entrée les positions des sommets, leurs couleurs, leurs coordonnées de texture,
// et produit en sortie la position finale du sommet sur l'écran, 
//ainsi que toutes les données qui seront transmises au fragment shader.


#version 330 core
layout(location = 0) in int packedData;
layout(location = 1) in int packedUV;

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

void main()
{
    // unpack les données 
    // donc décale tout et applique un masque pour ne pas récupèrer les valeurs des autres variables
    int x      = (packedData >> 27) & 0x1F;
    int y      = (packedData >> 22) & 0x1F;
    int z      = (packedData >> 17) & 0x1F;
    int u_bit  = (packedData >> 16) & 0x1;
    int v_bit  = (packedData >> 15) & 0x1;
    dir        = (packedData >> 12) & 0x7;
    texId      =  packedData        & 0xFFF; // F = 1111, F = 1111 F = 1111 ça fait 12bits

    int w     = (packedUV >> 27) & 0x1F;
    int h     = (packedUV >> 22) & 0x1F;
    int ao    = (packedUV >> 20) & 0x3;
    int sky   = (packedUV >> 16) & 0xF;
    int block = (packedUV >> 12) & 0xF;

    isBorder = (texId == 0 && u_bit == 0 && v_bit == 0) ? 1 : 0; // détecte bordure

    vec3 position = vec3(float(x), float(y), float(z));

    gl_Position = projection * view * model * vec4(position, 1.0); // un vec3 est utilisé pour construire un vec4
    uv = vec2(u_bit == 0 ? 0.0 : float(w),
              v_bit == 0 ? 0.0 : float(h));
    AO = float(ao);
    skyLight = float(sky);
    blockLight = float(block);
    fogDistance = length((view * model * vec4(position, 1.0)).xyz);
}