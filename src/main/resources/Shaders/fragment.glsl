// Son rôle est de déterminer la couleur finale de chaque pixel.
// C'est là que se passe la magie visuelle : application des textures, calculs d'éclairage, ombres, reflets, transparence, effets spéciaux.
// Le fragment shader reçoit les données interpolées depuis le vertex shader et calcule une couleur RGBA pour chaque pixel.

#version 330 core
flat in int texId; // id de texture de la face
flat in int dir; // direction de la face
flat in int isBorder; // détermine si c'est les bordures de subchunk à afficher ou non
in vec2 uv; // coordonnées uv 
in float AO;

out vec4 FragColor;

uniform sampler2DArray tex; // la texture correspondante à l'id

void main()
{
    if(isBorder == 1){
        FragColor = vec4(1.0, 0.0, 0.0, 1.0); // bordure rouge pour les subchunks
        return;
    }
    
    // lumière simple de base
    
    float light;
         if (dir == 0) light = 0.8; // NORTH
    else if (dir == 1) light = 0.8; // SOUTH
    else if (dir == 2) light = 0.8; // WEST 
    else if (dir == 3) light = 0.8; // EAST
    else if (dir == 4) light = 1.0; // UP, pleine lumière
    else               light = 0.5; // DOWN, sombre

    float aoFactor = 1.0 - AO  * 0.2;
    // ao=0 -> 1.0 - 0*0.2 = 1.0 -> pleine lumière
    // ao=1 -> 1.0 - 1*0.2 = 0.8 -> légèrement sombre
    // ao=2 -> 1.0 - 2*0.2 = 0.6 -> plus sombre
    // ao=3 -> 1.0 - 3*0.2 = 0.4 -> coin très sombre

    // calcule la taille texture par rapport au coordonnées uv et la texture
    vec4 texColor = texture(tex, vec3(fract(uv), float(texId))); 
    FragColor = vec4(texColor.rgb * light * aoFactor, texColor.a);
}