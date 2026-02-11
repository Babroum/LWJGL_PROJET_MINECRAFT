package demo.Exemple;

/**
 * Vertex décrivant un sommet d'une face d'une maille.
 * <br/>
 * Le vertex contient non seulement les coordonnées du point,
 * mais également d'autre informations utiles pour les textures et la lumiere
 */
public class VertexEx {

    public float x = 0f;
    public float y = 0f;
    public float z = 0f;

    public VertexEx()
    {
    }

    public VertexEx(float x, float y, float z)
    {
        this.x = x;
        this.y = y;
        this.z = z;
    }
}
