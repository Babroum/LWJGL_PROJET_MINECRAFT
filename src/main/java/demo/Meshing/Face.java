package demo.Meshing;

/**
 * Une face décrivant un ensemble de 4 Sommets d'une maille.
 */
@Deprecated
public class Face {
    
    /**
     * Tableau des sommets de la face.
     */
    // {x1, y1, z1, u1, v1, direction1, texId,  x2, y2, z2, u2, v2, direction2, texId, ...}
    public int[] vertexs = new int[42]; // 7 données pour 6 sommets
    public int texture;
    public String direction;
    public int nbCoords; // nombre de coordonnées dans la face

    public static final int[] QUAD_UV = {
        1, 1,  // v0 bas droite
        0, 1,  // v1 bas gauche
        0, 0,  // v2 haut gauche

        0, 0,  // v3 haut gauche
        1, 0,  // v4 haut droite
        1, 1   // v5 bas droite
    };

    /**
     * Ajoute un sommet grâce à ses coordonnées X, Y, Z au tableau des sommets.
     * @param X float
     * @param Y float
     * @param Z float
     * @throws ArrayFull
     */
    public void addVertex(int X, int Y, int Z){
        int d = 0;
        switch (this.direction) {
            case "north":
                d = 0;
                break;
            case "south":
                d = 1;
                break;
            case "west":
                d = 2;
                break;
            case "east":
                d = 3;
                break;
            case "up":
                d = 4;
                break;
            case "down":
                d = 5;
                break;
        
            default:
                break;
        }
        if (this.nbCoords == this.vertexs.length){
            throw new RuntimeException("Array Full");
        }
        int vertexIndex = this.nbCoords / 7;
        this.vertexs[this.nbCoords]     = X;
        this.vertexs[this.nbCoords + 1] = Y;
        this.vertexs[this.nbCoords + 2] = Z;
        this.vertexs[this.nbCoords + 3] = Face.QUAD_UV[vertexIndex * 2];
        this.vertexs[this.nbCoords + 4] = Face.QUAD_UV[vertexIndex * 2 + 1];
        this.vertexs[this.nbCoords + 5] = d;
        this.vertexs[this.nbCoords + 6] = texture;
        this.nbCoords += 7;
    }

}
