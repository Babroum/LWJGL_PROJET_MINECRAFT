package demo.Types;

import demo.Annotations.OpenGLThread;

/**
 * Définis une couleur RGBA
 */
public class Color4f {

    // Couleur simple préfètes
    /** Blanc */
    public static final Color4f WHITE = new Color4f(255);
    /** Noir */
    public static final Color4f BLACK = new Color4f(0);

    /** Rouge */
    public static final Color4f RED = new Color4f(255,0,0);
    /** Vert */
    public static final Color4f GREEN = new Color4f(0,255,0);
    /** Bleu */
    public static final Color4f BLUE = new Color4f(0,0,255);
    
    /** Cyan */
    public static final Color4f CYAN = new Color4f(0,255,255);
    /** Violet */
    public static final Color4f PURPLE = new Color4f(255,0,255);
    /** Jaune */
    public static final Color4f YELLOW = new Color4f(255,255,0);

    /** Couleur Ciel */
    public static final Color4f SKY = new Color4f(207,236,247);

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
     * Définis une couleur par intensité de rouge, vert et bleu, et un niveau d'opacité
     * @param r rouge
     * @param g vert
     * @param b bleu
     * @param a alpha
     * @throws RuntimeException
     */
    public Color4f(float r, float g, float b, float a) throws RuntimeException{
        if ((r < 0 || r > 255) ||
            (g < 0 || g > 255) ||
            (b < 0 || b > 255) ||
            (a < 0 || a > 100)){
            throw new RuntimeException("RGB doit être entre 0 et 255, A doit être entre 0 et 100");
        }
        this.red = r / 255;
        this.green = g / 255;
        this.blue = b / 255;
        this.alpha = a / 100;
    }
    /**
     * Définis une couleur opaque par intensité de rouge, vert et bleu
     * @param r float
     * @param g float
     * @param b float
     * @throws RuntimeException
     */
    public Color4f(float r, float g, float b) throws RuntimeException{
        this(r, g, b, 100);
    }
    /**
     * Définis une couleur par intensité de gris et un niveau d'opacité
     * @param gray float
     * @param a float
     * @throws RuntimeException
     */
    public Color4f(float gray, float a) throws RuntimeException{
        this(gray, gray, gray, a);
    }
    /**
     * Définis une couleur opaque par intensité de gris
     * @param gray float
     * @throws RuntimeException
     */
    public Color4f(float gray) throws RuntimeException{
        this(gray, gray, gray,100);
    }
    /**
     * Demande à OpenGL d'utilisé cette couleur
     */
    @OpenGLThread
    public void apply(){
        // donne la couleur au shader
        //ShadersManager.runFragmentColor(this.red, this.green, this.blue, this.alpha);
        //GL11.glColor4f(this.red, this.green, this.blue, this.alpha);
    }
    
}