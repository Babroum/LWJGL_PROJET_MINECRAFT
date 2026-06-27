#version 330 core
in vec2 uv;
out vec4 FragColor;

uniform sampler2D tex;
uniform vec4 color;

void main(){
    vec4 texColor = texture(tex, uv) * color;

    // équivalent moderne de l'ancien glAlphaFunc(GL_GREATER, 0.01f)
    // jette les pixels quasi-transparents (fond de la police) au lieu de les écrire en noir opaque
    if (texColor.a < 0.01){
        discard;
    }

    FragColor = texColor;

}