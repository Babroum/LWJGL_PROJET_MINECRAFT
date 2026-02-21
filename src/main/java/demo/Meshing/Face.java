package demo.Meshing;

import demo.Exceptions.ArrayFull;
import demo.Managers.TexturesManager;
import demo.Enum.Directions;
import demo.Enum.TexturesType;

/**
 * Une face décrivant un ensemble de 4 Sommets d'une maille.
 */
public class Face {
    
    /**
     * Tableau des sommets de la face.
     */
    // {x1, y1, z1, u1, v1, direction1, texId,  x2, y2, z2, u2, v2, direction2, texId, ...}
    public int[] vertexs = new int[42]; // 7 données pour 6 sommets
    public TexturesType texture;
    public Directions direction;
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
    public void addVertex(int X, int Y, int Z) throws ArrayFull{
        if (this.nbCoords == this.vertexs.length){
            throw new ArrayFull();
        }
        int vertexIndex = this.nbCoords / 7;
        this.vertexs[this.nbCoords]     = X;
        this.vertexs[this.nbCoords + 1] = Y;
        this.vertexs[this.nbCoords + 2] = Z;
        this.vertexs[this.nbCoords + 3] = Face.QUAD_UV[vertexIndex * 2];
        this.vertexs[this.nbCoords + 4] = Face.QUAD_UV[vertexIndex * 2 + 1];
        this.vertexs[this.nbCoords + 5] = this.direction.ordinal();
        this.vertexs[this.nbCoords + 6] = TexturesManager.getIndex(texture);
        this.nbCoords += 7;
    }
}
