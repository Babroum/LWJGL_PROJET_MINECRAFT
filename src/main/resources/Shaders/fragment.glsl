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
in float fogDistance; 

out vec4 FragColor;

uniform vec3 skyColor;
uniform float fogStart;
uniform float fogEnd;

uniform sampler2DArray tex; // la texture correspondante à l'id

// fonction déclarée en dehors de main()
float lightCurve(float l) {
    return pow(l, 1.4);
}

void main()
{
    if(isBorder == 1){
        FragColor = vec4(1.0, 0.0, 0.0, 1.0); // bordure rouge pour les subchunks
        return;
    }     

    // facteur en fonction de la direction de la face
    float dirFactor = 1;
    if(dir == 0 || dir == 1){
        dirFactor = 0.8;
    }else if (dir == 2 || dir == 3){
        dirFactor = 0.6;
    }else if (dir == 4 ){
        dirFactor = 1;
    }else{
        dirFactor = 0.5;
    }

    float aoFactor = 1.0 - (AO / 3.0) * 0.5;  // fixe, géométrique 0.35

    float normalizedSkyLight   = skyLight   / 15.0;
    float normalizedBlockLight = blockLight / 15.0;

    float sl = lightCurve(normalizedSkyLight);
    float bl = lightCurve(normalizedBlockLight);

    // calcule la taille texture par rapport au coordonnées uv et la texture
    vec4 texColor = texture(tex, vec3(fract(uv), float(texId))); 

    if (texColor.a < 0.5) {
        discard;
    }
    
    float fogFactor = clamp((fogEnd - fogDistance) / (fogEnd - fogStart), 0.0, 1.0);
    vec3 litColor = texColor.rgb * max(bl, sl) * aoFactor * dirFactor;
    vec3 finalColor = mix(skyColor, litColor, fogFactor);
    FragColor = vec4(finalColor, texColor.a);
}