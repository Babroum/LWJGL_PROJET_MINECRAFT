package demo;


/**
* Un Vertex décrivant un sommet d'une face d'un mesh.
*/
public class Vertex {

    /**
     * Position X du sommet.
     */
    public float x;
    /**
     * Position Y du sommet.
     */
    public float y;
    /**
     * Position Z du sommet.
     */
    public float z;

    /**
     * Créer un sommet grâce à des coordonées X, Y, Z.
     * @param X
     * @param Y
     * @param Z
     */
    public Vertex(float X, float Y, float Z){
        this.x = X;
        this.y = Y;
        this.z = Z;
    }
}
