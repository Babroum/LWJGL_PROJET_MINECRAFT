package demo.Exemple;

import org.lwjgl.opengl.GL11;

/**
 * Représente une couleur
 */
public class Color4fEx
{
    public static final Color4fEx BLACK      = new Color4fEx(0f);
    public static final Color4fEx DARK_GREY  = new Color4fEx(0.25f);
    public static final Color4fEx GREY       = new Color4fEx(0.5f);
    public static final Color4fEx LIGHT_GREY = new Color4fEx(0.75f);
    public static final Color4fEx WHITE      = new Color4fEx(1f);

    public static final Color4fEx DARK_RED  = new Color4fEx(0.5f, 0f, 0f);
    public static final Color4fEx RED       = new Color4fEx(1f, 0f, 0f);
    public static final Color4fEx LIGHT_RED = new Color4fEx(1f, 0.5f, 0.5f);

    public static final Color4fEx DARK_GREEN  = new Color4fEx(0f, 0.5f, 0f);
    public static final Color4fEx GREEN       = new Color4fEx(0f, 1f, 0f);
    public static final Color4fEx LIGHT_GREEN = new Color4fEx(0.5f, 1f, 0.5f);

    public static final Color4fEx DARK_BLUE  = new Color4fEx(0f, 0f, 0.5f);
    public static final Color4fEx BLUE       = new Color4fEx(0f, 0f, 1f);
    public static final Color4fEx LIGHT_BLUE = new Color4fEx(0.5f, 0.5f, 1f);

    /**
     * Intensité de rouge
     */
    public final float red;
    /**
     * Intensité de vert
     */
    public final float green;
    /**
     * Intensité de bleu
     */
    public final float blue;
    /**
     * Niveau d'opacité
     */
    public final float alpha;

    /**
     * Construit la couleur à partir d'un description ARGB
     *
     * @param argb Description ARGB
     */
    public Color4fEx(int argb)
    {
        this((float) ((argb >> 16) & 0xFF) / 255f,
             (float) ((argb >> 8) & 0xFF) / 255f,
             (float) (argb & 0xFF) / 255f,
             (float) ((argb >> 24) & 0xFF) / 255f);
    }

    /**
     * Construit une couleur avec niveau de gris opaque
     *
     * @param grey Niveau de gris
     */
    public Color4fEx(float grey)
    {
        this(grey, grey, grey, 1f);
    }

    /**
     * Construit une couleur avec niveau de gris
     *
     * @param grey  Niveau de gris
     * @param alpha Niveau d'opacité
     */
    public Color4fEx(float grey, float alpha)
    {
        this(grey, grey, grey, alpha);
    }

    /**
     * Construit une couleur opaque
     *
     * @param red   Intensité de rouge
     * @param green Intensité de vert
     * @param blue  Intensité de bleu
     */
    public Color4fEx(float red, float green, float blue)
    {
        this(red, green, blue, 1f);
    }

    /**
     * Construit une couleur
     *
     * @param red   Intensité de rouge
     * @param green Intensité de vert
     * @param blue  Intensité de bleu
     * @param alpha Niveau d'opacité
     */
    public Color4fEx(float red, float green, float blue, float alpha)
    {
        this.red   = red;
        this.green = green;
        this.blue  = blue;
        this.alpha = alpha;
    }

    /**
     * Demande à OpenGL d'utilisé cette couleur
     */
    @ThreadOpenGLEx
    void apply()
    {
        GL11.glColor4f(this.red, this.green, this.blue, this.alpha);
    }
}