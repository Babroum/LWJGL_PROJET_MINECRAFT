// Son rôle est de déterminer la couleur finale de chaque pixel.
// C'est là que se passe la magie visuelle : application des textures, calculs d'éclairage, ombres, reflets, transparence, effets spéciaux.
// Le fragment shader reçoit les données interpolées depuis le vertex shader et calcule une couleur RGBA pour chaque pixel.

#version 330 core
in vec2 uv;
out vec4 FragColor;

uniform sampler2D tex;
uniform vec4 color;

void main()
{
    FragColor = texture(tex, uv) * color;;
}