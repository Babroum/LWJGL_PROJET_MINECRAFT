package demo.Rendering;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.stb.*;

import demo.ShaderManager;
import demo.Annotations.OpenGLThread;
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

    /**
     * Charge l'image des caractères en mémoire
     * @param path String, chemin de l'image
     * @param gWidth int, largeur de la grille
     * @param gHeight int, hauteur de la grille
     * @param ftPerRow int, nb de caractère par ligne
     * @param chars String, liste des caractères de l'image
     */
    @OpenGLThread
    public TextRender(String path, int gWidth, int gHeight, int ftPerRow, String chars){
        this.gridWidth = gWidth;
        this.gridHeight = gHeight;
        this.fontPerRow = ftPerRow;
        this.chars = chars;

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

        ShaderManager.createTextShader();
    }

    /**
     * Dessine un texte à des coordonnées données
     * @param window Window
     * @param text String
     * @param x int
     * @param y int
     * @param ftSize int, taille d'un caractère
     */
    @OpenGLThread
    public void drawText(Window window, String text, int x, int y, int fontSize, Color4f color){   

        ShaderManager.useTextShader();
        ShaderManager.runTextOrtho(window.width, window.height);
        ShaderManager.runTextFragmentColor(color.red, color.green, color.blue, color.alpha);

        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL20.glActiveTexture(GL20.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.textureId);
        GL20.glUniform1i(GL20.glGetUniformLocation(ShaderManager.getProgramTextId(), "tex"), 0); // 0 = texture unit 0

        float[] data = new float[text.length() * 16]; // 6 vertices * 4 (x,y,u,v) par caractère
        for (int i = 0; i < text.length(); i++){
            float[] charData = this.drawChar(text.charAt(i), this.chars, x + i * fontSize, y, fontSize);
            System.arraycopy(charData, 0, data, i * 16, 16);
        }

        int[] indices = new int[text.length() * 6];
        for(int i = 0; i < text.length(); i++){
            int v = i * 4;
            indices[i*6+0] = v;
            indices[i*6+1] = v+1;
            indices[i*6+2] = v+2;
            indices[i*6+3] = v+2;
            indices[i*6+4] = v+3;
            indices[i*6+5] = v;
        }

        FloatBuffer vertexsBuffer = BufferUtils.createFloatBuffer(text.length() * 16);
        vertexsBuffer.put(data).flip();

        IntBuffer indicesBuffer = BufferUtils.createIntBuffer(text.length() * 6);
        indicesBuffer.put(indices).flip();

        int[] ids = ShaderManager.createTextVBOVAOEBO(vertexsBuffer, indicesBuffer);

        GL30.glBindVertexArray(ids[0]);
        GL11.glDrawElements(GL11.GL_TRIANGLES, indices.length, GL11.GL_UNSIGNED_INT, 0);
        GL30.glBindVertexArray(0);

        ShaderManager.deleteVBOVAOEBO(ids); // a modifier par des vbo dynamique

        GL11.glEnable(GL11.GL_DEPTH_TEST);

        ShaderManager.useShader();      
    }

    /**
     * Dessine 1 caractère à des coordonnées données
     * @param c char
     * @param text String, la liste des caractères disponible dans le même ordre que l'image
     * @param x int
     * @param y int
     */
    @OpenGLThread
    private float[] drawChar(char c, String text, int x, int y, int fontSize){
        float row = 0f;

        // pour chaque caractère dans la liste de caractère
        for (int i = 0; i < text.length(); i++){
            // si i dépasse le nombre de caractère par ligne
            if (i % this.fontPerRow == 0 && i != 0){row++;} 
            // si le caractère égale à un de la liste
            if (c == text.charAt(i)){

                // défini la position du caractère en 0.0 et 1.0
                float column = (i % this.fontPerRow) * this.gridWidth / (float)this.width;
                row = this.gridHeight * row / (float)this.height;

                return new float[]{x, y, column, row, 
                                   x + fontSize, y, column + (float)this.gridWidth / this.width, row,
                                   x + fontSize, y + fontSize, column + (float)this.gridWidth / this.width, row + (float)this.gridHeight / height,
                                   x, y + fontSize, column, row + (float)this.gridHeight / height
                };

            }
        }
        return null;
    }
}
