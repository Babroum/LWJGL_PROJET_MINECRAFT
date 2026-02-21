// Son rôle est de déterminer la couleur finale de chaque pixel.
// C'est là que se passe la magie visuelle : application des textures, calculs d'éclairage, ombres, reflets, transparence, effets spéciaux.
// Le fragment shader reçoit les données interpolées depuis le vertex shader et calcule une couleur RGBA pour chaque pixel.

#version 330 core
flat in int texId;
flat in int dir;
flat in int isBorder;
in vec2 uv;

out vec4 FragColor;

uniform sampler2DArray tex;

void main()
{
    if(isBorder == 1){
        FragColor = vec4(1.0, 0.0, 0.0, 1.0);
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

    vec4 texColor = texture(tex, vec3(uv, float(texId))); 
    FragColor = vec4(texColor.rgb * light, texColor.a);
    //FragColor = vec4(1.0,0.0,0.0,1.0); // pour test
}