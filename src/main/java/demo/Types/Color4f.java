package demo.Types;

import org.lwjgl.opengl.GL11;

import demo.OpenGLThread;
import demo.Exceptions.IncorrectColorValue;

/**
 * Définit une couleur RGBA
 */
public class Color4f {

    // Couleur simple préfaite
    public static final Color4f WHITE = new Color4f(255);
    public static final Color4f BLACK = new Color4f(0);

    public static final Color4f RED = new Color4f(255,0,0);
    public static final Color4f GREEN = new Color4f(0,255,0);
    public static final Color4f BLUE = new Color4f(0,0,255);

    
    public static final Color4f CYAN = new Color4f(0,255,255);
    public static final Color4f PURPLE = new Color4f(255,0,255);
    public static final Color4f YELLOW = new Color4f(255,255,0);

    /**
     * Intensité de rouge (0 à 255)
     */
    public float red;
    /*
    * Intensité de vert (0 à 255)
    */
    public float green;
    /*
    * Intensité de bleu (0 à 255)
    */
    public float blue;
    /*
    * Niveau d'opacité (0 à 100)
    */
    public float alpha;

    /**
     * Définit une couleur par intensité de rouge, vert et bleu, et un niveau d'opacité
     * @param r rouge
     * @param g vert
     * @param b bleu
     * @param a alpha
     * @throws IncorrectColorValue
     */
    public Color4f(int r, int g, int b, int a) throws IncorrectColorValue{
        if ((r < 0 || r > 255) ||
            (g < 0 || g > 255) ||
            (b < 0 || b > 255) ||
            (a < 0 || a > 100)){
            throw new IncorrectColorValue("RGB doit être entre 0 et 255, A doit être entre 0 et 100");
        }
        this.red = r / 255;
        this.green = g / 255;
        this.blue = b / 255;
        this.alpha = a / 100;
    }
    /**
     * Définit une couleur opaque par intensité de rouge, vert et bleu
     * @param r rouge
     * @param g vert
     * @param b bleu
     * @throws IncorrectColorValue
     */
    public Color4f(int r, int g, int b) throws IncorrectColorValue{
        this(r, g, b, 100);
    }
    /**
     * Définit une couleur par intensité de gris et un niveau d'opacité
     * @param gray gris
     * @param a alpha
     * @throws IncorrectColorValue
     */
    public Color4f(int gray, int a) throws IncorrectColorValue{
        this(gray, gray, gray, a);
    }
    /**
     * Définit une couleur opaque par intensité de gris
     * @param gray gris
     * @throws IncorrectColorValue
     */
    public Color4f(int gray) throws IncorrectColorValue{
        this(gray, gray, gray,100);
    }
    /**
     * Demande à OpenGL d'utilisé cette couleur
     */
    @OpenGLThread
    public void apply(){
        GL11.glColor4f(this.red, this.green, this.blue, this.alpha);
    }
    
}