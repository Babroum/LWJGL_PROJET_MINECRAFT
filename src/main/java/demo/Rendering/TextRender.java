package demo.Rendering;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.lwjgl.system.MemoryUtil;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.stb.*;

import demo.Annotations.OpenGLThread;
import demo.Managers.BuffersManager;
import demo.Managers.ShadersManager;
import demo.Types.Color4f;

/**
 * Affichage du texte à l'écran
 */
public class TextRender {

    private int gridWidth;
    private int gridHeight;
    private int width;
    private int height;
    private int textureId;
    private int fontPerRow;
    private String chars;

    private int[] ids;
    private FloatBuffer vertexsBuffer;
    private IntBuffer indicesBuffer;
    private final int BUFFERSIZE = 512;
    private int texUniformLocation;

    private float[] data = new float[BUFFERSIZE * 16];
    private int[] indices = new int[BUFFERSIZE * 6];
    
    private FloatBuffer buffer;

    /**
     * Charge l'image des caractères en mémoire <br>
     * <b>TAILLE MAX TEXT 512 CHAR</b>
     * @param path String, chemin de l'image
     * @param gWidth int, largeur d'une case de la grille
     * @param gHeight int, hauteur d'une case de la grille
     * @param ftPerRow int, nb de caractère par ligne
     * @param chars String, liste des caractères de l'image
     */
    @OpenGLThread
    public TextRender(ByteBuffer path, int gWidth, int gHeight, int ftPerRow, String chars){
        this.gridWidth = gWidth;
        this.gridHeight = gHeight;
        this.fontPerRow = ftPerRow;
        this.chars = chars;

        this.buffer = MemoryUtil.memAllocFloat(16);

        // pour opengl les buffers          
        IntBuffer width   = MemoryUtil.memAllocInt(1);
        IntBuffer height  = MemoryUtil.memAllocInt(1);
        IntBuffer channel = MemoryUtil.memAllocInt(1);

        // charge l'image, et remplis les buffers
        ByteBuffer pixels = STBImage.stbi_load_from_memory(path, width, height, channel, 4);

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
        MemoryUtil.memFree(width);
        MemoryUtil.memFree(height);
        MemoryUtil.memFree(channel);
        MemoryUtil.memFree(path); 

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);

        // Initialise les buffers / shaders / vbo vao ebo
        ShadersManager.createTextShader();
        this.vertexsBuffer = MemoryUtil.memAllocFloat(this.BUFFERSIZE * 16);
        this.indicesBuffer = MemoryUtil.memAllocInt(this.BUFFERSIZE * 6);
        this.ids = BuffersManager.createTextVBOVAOEBO(vertexsBuffer, indicesBuffer);
        
        this.texUniformLocation = GL20.glGetUniformLocation(ShadersManager.getProgramTextId(), "tex");
    }

    /**
     * Dessine un texte à des coordonnées données
     * @param text String
     * @param x int
     * @param y int
     * @param fontSize int, taille d'un caractère
     * @param color Color4f
     */
    @OpenGLThread
    public void drawText(String text, int x, int y, int fontSize, Color4f color){   

        if (text == null) return;

        ShadersManager.useTextShader();
        ShadersManager.runTextOrtho(this.buffer, Window.width, Window.height);
        ShadersManager.runTextFragmentColor(color.red, color.green, color.blue, color.alpha);

        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL20.glActiveTexture(GL20.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.textureId);
        GL20.glUniform1i(this.texUniformLocation, 0);

        for (int i = 0; i < text.length(); i++){
            this.drawChar(text.charAt(i), this.chars, x + i * fontSize, y, fontSize, i);
        }

        for(int i = 0; i < text.length(); i++){
            int v = i * 4;
            this.indices[i*6+0] = v;
            this.indices[i*6+1] = v+1;
            this.indices[i*6+2] = v+2;
            this.indices[i*6+3] = v+2;
            this.indices[i*6+4] = v+3;
            this.indices[i*6+5] = v;
        }      

        this.vertexsBuffer.clear();
        this.vertexsBuffer.put(data).flip(); 

        this.indicesBuffer.clear();               
        this.indicesBuffer.put(indices).flip();

        // update avec les nouvelle données
        BuffersManager.updateTextVBOEBO(vertexsBuffer, indicesBuffer, this.ids[0], this.ids[2]);

        GL30.glBindVertexArray(ids[1]);
        GL11.glDrawElements(GL11.GL_TRIANGLES, text.length() * 6, GL11.GL_UNSIGNED_INT, 0);
        GL30.glBindVertexArray(0);

        GL11.glEnable(GL11.GL_DEPTH_TEST);

        ShadersManager.useShader();      
    }

    /**
     * Dessine 1 caractère à des coordonnées données
     * @param c char
     * @param text String, la liste des caractères disponible dans le même ordre que l'image
     * @param x int
     * @param y int
     * @param fontSize int
     * @param charIndex int
     */
    @OpenGLThread
    private void drawChar(char c, String text, int x, int y, int fontSize, int charIndex){
        float row = 0f;
        int offset = charIndex * 16;

        // pour chaque caractère dans la liste de caractère
        for (int i = 0; i < text.length(); i++){
            // si i dépasse le nombre de caractère par ligne
            if (i % this.fontPerRow == 0 && i != 0){row++;} 
            // si le caractère égale à un de la liste
            if (c == text.charAt(i)){

                // défini la position du caractère en 0.0 et 1.0
                float column = (i % this.fontPerRow) * this.gridWidth / (float)this.width;
                row = this.gridHeight * row / (float)this.height;

                // écrit directement dans this.data au lieu de créer un nouveau tableau
                this.data[offset]    = x;            this.data[offset+1]  = y;
                this.data[offset+2]  = column;       this.data[offset+3]  = row;
                this.data[offset+4]  = x + fontSize; this.data[offset+5]  = y;
                this.data[offset+6]  = column + (float)this.gridWidth / this.width;
                this.data[offset+7]  = row;
                this.data[offset+8]  = x + fontSize; this.data[offset+9]  = y + fontSize;
                this.data[offset+10] = column + (float)this.gridWidth / this.width;
                this.data[offset+11] = row + (float)this.gridHeight / this.height;
                this.data[offset+12] = x;            this.data[offset+13] = y + fontSize;
                this.data[offset+14] = column;       this.data[offset+15] = row + (float)this.gridHeight / this.height;

                return;

            }
        }
    }

    /**
     * Libère tous les buffers alloués avec MemoryUtil.
     */
    public void cleanup() {
        MemoryUtil.memFree(this.vertexsBuffer);
        MemoryUtil.memFree(this.indicesBuffer);
        MemoryUtil.memFree(this.buffer);
        GL11.glDeleteTextures(this.textureId);
    }
}
