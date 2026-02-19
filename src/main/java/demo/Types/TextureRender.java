package demo.Types;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.stb.STBImage;

import demo.ShaderManager;
import demo.Annotations.OpenGLThread;
import demo.Enum.Textures;

public class TextureRender {

    private int gridWidth;
    private int gridHeight;
    private int width;
    private int height;
    private int textureId;
    private int texturePerRow;

    /**
     * Charge l'image des textures en mémoire <br>
     * @param path String, chemin de l'image
     * @param gWidth int, largeur d'une case de la grille
     * @param gHeight int, hauteur d'une case de la grille
     * @param texPerRow int, nb de texture par ligne
     */

    @OpenGLThread
    public TextureRender(String path, int gWidth, int gHeight, int texPerRow){
        this.gridWidth = gWidth;
        this.gridHeight = gHeight;
        this.texturePerRow = texPerRow;

        // pour opengl les buffers
        IntBuffer width = BufferUtils.createIntBuffer(1);
        IntBuffer height = BufferUtils.createIntBuffer(1);
        IntBuffer channel = BufferUtils.createIntBuffer(1);

        // charge l'image, et remplis les buffers
        ByteBuffer pixels = STBImage.stbi_load(path, width, height, channel, 4);

        if (pixels == null) {
            System.err.println("Erreur : " + STBImage.stbi_failure_reason());
            return;
        }

        // récupère la taille de l'image depuis les buffers
        this.width = width.get(0);
        this.height = height.get(0);

        // récupère un id pour l'image
        this.textureId = GL11.glGenTextures();
        if (this.textureId <= 0) { return;}
        // définis la texture
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.textureId);       
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, this.width, this.height, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
            
        // libère les mémoires
        STBImage.stbi_image_free(pixels);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
    }

    /**
     * Retourne les UV correspondant à la texture <br>
     * En partant en haut a gauche puis sens horaire <br>
     * {u,v, u,v, u,v, u,v}
     * @param texture
     * @return
     */
    public float[] getUV(Textures texture){
        int column = 0;
        int row = 0;
        switch (texture) {
            case Textures.GRASS:
                column = 0; row = 0;
                break;
            case Textures.COBBLESTONE:
                column = 1; row = 0;
                break;
        
            default:
                // la texture en bas a droite NULL
                column = this.texturePerRow - 1;
                row = this.texturePerRow - 1;
        }
        return new float[]{
            (float)(column * this.gridWidth) / this.width                                     , (float)(row * this.gridHeight) / this.height,                                        // coin haut gauche
            (float)(column * this.gridWidth) / this.width + (float)this.gridWidth / this.width, (float)(row * this.gridHeight) / this.height,                                        // coin haut droite
            (float)(column * this.gridWidth) / this.width + (float)this.gridWidth / this.width, (float)(row * this.gridHeight) / this.height + (float)this.gridHeight / this.height, // coin bas droite
            (float)(column * this.gridWidth) / this.width                                     , (float)(row * this.gridHeight) / this.height + (float)this.gridHeight / this.height  // coin bas gauche
        };
    }

    public void draw(){
        if(this.textureId < 0){System.out.println("TextureRender: draw: pas de textureId"); return;}
        GL20.glActiveTexture(GL20.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.textureId);
        GL20.glUniform1i(GL20.glGetUniformLocation(ShaderManager.getProgramId(), "tex"), 0); // 0 = texture unit 0
    }
}
