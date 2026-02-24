package demo.Rendering;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.stb.STBImage;

import demo.Annotations.OpenGLThread;
import demo.Enum.TexturesType;
import demo.Managers.ShadersManager;

public class TextureRender {
    private int textureId;

    /**
     * Prépare la mémoire à recevoir des textures
     */
    @OpenGLThread
    public TextureRender(){
        this.textureId = GL11.glGenTextures();
        GL11.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, this.textureId);
        GL30.glTexImage3D(GL30.GL_TEXTURE_2D_ARRAY, 0, GL11.GL_RGBA, 16, 16, TexturesType.values().length, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer)null);
        GL11.glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
    }

    /**
     * Ajoute une texture grâce à son chemin string transformé en ByteBuffer par FileLoader <br>
     * Et le type de texture
     * @param path ByteBuffer
     * @param texture Textures
     */
    public void addTexture(ByteBuffer path, TexturesType texture){
        IntBuffer w = BufferUtils.createIntBuffer(1);
        IntBuffer h = BufferUtils.createIntBuffer(1);
        IntBuffer c = BufferUtils.createIntBuffer(1);
        // récup l'image en mémoire
        ByteBuffer pixels = STBImage.stbi_load_from_memory(path, w, h, c, 4);
        // et l'ajoute au shader
        GL30.glTexSubImage3D(GL30.GL_TEXTURE_2D_ARRAY, 0, 0, 0, texture.ordinal(), 16, 16, 1, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);
        // libère la mémoire
        STBImage.stbi_image_free(pixels);
    }   

    /**
     * dessine la texture
     */
    public void draw(){
        if(this.textureId < 0){System.out.println("TextureRender: draw: pas de textureId"); return;}
        GL30.glActiveTexture(GL30.GL_TEXTURE0);
        GL11.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, this.textureId);
        GL30.glUniform1i(GL30.glGetUniformLocation(ShadersManager.getProgramId(), "tex"), 0);
    }
}
