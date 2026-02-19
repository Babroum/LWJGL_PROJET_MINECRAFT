package demo.Rendering;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.stb.STBImage;

import demo.ShaderManager;
import demo.Annotations.OpenGLThread;
import demo.Enum.Textures;

public class TextureRender {
    private int textureId;

    /**
     * Prépare la mémoire à recevoir des textures
     */
    @OpenGLThread
    public TextureRender(){
        this.textureId = GL11.glGenTextures();
        GL11.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, this.textureId);
        GL30.glTexImage3D(GL30.GL_TEXTURE_2D_ARRAY, 0, GL11.GL_RGBA, 16, 16, Textures.values().length, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer)null);
        GL11.glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
    }

    /**
     * Retourne l'index de la texture
     * @param texture Textures
     * @return int
     */
    public int getIndex(Textures texture){
        return texture.ordinal(); // retourne 0 pour GRASS, 1 pour COBBLESTONE etc.
    }

    public void addTexture(String path, Textures texture){
        IntBuffer w = BufferUtils.createIntBuffer(1);
        IntBuffer h = BufferUtils.createIntBuffer(1);
        IntBuffer c = BufferUtils.createIntBuffer(1);
        ByteBuffer pixels = STBImage.stbi_load(path, w, h, c, 4);
        GL30.glTexSubImage3D(GL30.GL_TEXTURE_2D_ARRAY, 0, 0, 0, texture.ordinal(), 16, 16, 1, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);
        STBImage.stbi_image_free(pixels);
    }   

    public void draw(){
        if(this.textureId < 0){System.out.println("TextureRender: draw: pas de textureId"); return;}
        GL30.glActiveTexture(GL30.GL_TEXTURE0);
        GL11.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, this.textureId);
        GL30.glUniform1i(GL30.glGetUniformLocation(ShaderManager.getProgramId(), "tex"), 0);
    }
}
