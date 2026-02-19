// Le vertex shader reçoit en entrée les positions des sommets, leurs couleurs, leurs coordonnées de texture,
// et produit en sortie la position finale du sommet sur l'écran, 
//ainsi que toutes les données qui seront transmises au fragment shader.

#version 330 core
layout (location = 0) in vec3 aPos; // La variable position a l'attribut de position 0
layout (location = 1) in vec2 aUV;
layout (location = 2) in float aTexId; // nouvel attribut

flat out int texId;

uniform mat4 projection;
uniform mat4 view;
uniform mat4 model;

out vec2 uv;

void main()
{
    gl_Position = projection * view * model * vec4(aPos, 1.0); // un vec3 est utilisé pour construire un vec4
    uv = aUV;
    texId = int(aTexId);
}