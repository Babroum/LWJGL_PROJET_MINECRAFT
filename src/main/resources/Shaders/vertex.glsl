// Le vertex shader reçoit en entrée les positions des sommets, leurs couleurs, leurs coordonnées de texture,
// et produit en sortie la position finale du sommet sur l'écran, 
//ainsi que toutes les données qui seront transmises au fragment shader.


#version 330 core
layout(location = 0) in int packedData;

flat out int texId;
flat out int dir;
flat out int isBorder;
out vec2 uv;

uniform mat4 projection;
uniform mat4 view;
uniform mat4 model;

void main()
{
    // unpack les données 
    // donc décale tout et applique un masque pour ne pas récupèrer les valeurs des autres variables
    int x = (packedData >> 27) & 0x1F;
    int y = (packedData >> 19) & 0xFF;
    int z = (packedData >> 14) & 0x1F;
    int u = (packedData >> 13) & 0x1;
    int v = (packedData >> 12) & 0x1;
    dir   = (packedData >> 9)  & 0x7;
    texId =  packedData        & 0x1FF;

    isBorder = (texId == 0 && u == 0 && v == 0) ? 1 : 0; // détecte bordure

    vec3 position = vec3(float(x), float(y), float(z));

    gl_Position = projection * view * model * vec4(position, 1.0); // un vec3 est utilisé pour construire un vec4
    uv = vec2(float(u), float(v));;
}