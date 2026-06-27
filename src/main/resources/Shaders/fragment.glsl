// Son rôle est de déterminer la couleur finale de chaque pixel.
// C'est là que se passe la magie visuelle : application des textures, calculs d'éclairage, ombres, reflets, transparence, effets spéciaux.
// Le fragment shader reçoit les données interpolées depuis le vertex shader et calcule une couleur RGBA pour chaque pixel.

#version 330 core
flat in int texId; // id de texture de la face
flat in int dir; // direction de la face
flat in int isBorder; // détermine si c'est les bordures de subchunk à afficher ou non
in vec2 uv; // coordonnées uv 
in float AO;
in float skyLight;
in float blockLight;

out vec4 FragColor;

uniform sampler2DArray tex; // la texture correspondante à l'id

void main()
{
    if(isBorder == 1){
        FragColor = vec4(1.0, 0.0, 0.0, 1.0); // bordure rouge pour les subchunks
        return;
    }     

    float aoFactor = 1.0 - (AO / 3.0) * 0.5;  // fixe, géométrique 0.35

    float normalizedSkyLight   = skyLight   / 15.0;
    float normalizedBlockLight = blockLight / 15.0;

    // calcule la taille texture par rapport au coordonnées uv et la texture
    vec4 texColor = texture(tex, vec3(fract(uv), float(texId))); 
    FragColor = vec4(texColor.rgb * max(normalizedBlockLight, normalizedSkyLight) * aoFactor, texColor.a);
}