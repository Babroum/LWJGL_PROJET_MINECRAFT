package demo.Rendering;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.stb.*;

import demo.Annotations.OpenGLThread;

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
    public void drawText(Window window, String text, int x, int y, int fontSize){   
        // permet d'afficher le texte que sur X et Y (coller a l'écran)  
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glOrtho(0, window.width, window.height, 0, -1, 1);

        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();         
        GL11.glLoadIdentity();

        // désactive le teste de profondeur car 2D et active les textures2D et récupe notre texture
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.textureId);

        //this.color.apply();
        // dessine chaque lettre
        GL11.glBegin(GL11.GL_QUADS);  
            for (int i = 0; i < text.length(); i++){
                this.drawChar(text.charAt(i), this.chars, x + i * fontSize, y, fontSize);
            }             
        GL11.glEnd();
        
        // remet comme avant
        GL11.glEnable(GL11.GL_DEPTH_TEST);

        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix(); // restaure la matrice de projection avant modification

        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPopMatrix();
    }

    /**
     * Dessine 1 caractère à des coordonnées données
     * @param c char
     * @param text String, la liste des caractères disponible dans le même ordre que l'image
     * @param x int
     * @param y int
     */
    @OpenGLThread
    private void drawChar(char c, String text, int x, int y, int fontSize){
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

                // dessine le quad du caractère
                GL11.glTexCoord2f(column, row);
                GL11.glVertex2f(x, y);

                GL11.glTexCoord2f(column + (float)this.gridWidth / this.width, row);
                GL11.glVertex2f(x + fontSize, y);

                GL11.glTexCoord2f(column + (float)this.gridWidth / this.width, row + (float)this.gridHeight / height);
                GL11.glVertex2f(x + fontSize, y + fontSize);

                GL11.glTexCoord2f(column, row + (float)this.gridHeight / height);
                GL11.glVertex2f(x, y + fontSize);                

                return;
            }
        }
    }
}
