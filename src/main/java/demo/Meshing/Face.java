package demo.Meshing;

import demo.Exceptions.ArrayEmpty;
import demo.Exceptions.ArrayFull;
import demo.Exceptions.IncorrectIndex;
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
    // {x1, y1, z1, u1, v1, texId,  x2, y2, z2, u2, v2, texId, ...}
    public int[] vertexs = new int[24];
    public TexturesType texture;
    public Directions direction;
    public int nbCoords; // nombre de coordonnées dans la face

    public static final int[] QUAD_UV = {
        1, 1,  // v0 bas droite
        0, 1,  // v1 bas gauche
        0, 0,  // v2 haut gauche
        1, 0   // v3 haut droite
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
        int vertexIndex = this.nbCoords / 6;
        this.vertexs[this.nbCoords]     = X;
        this.vertexs[this.nbCoords + 1] = Y;
        this.vertexs[this.nbCoords + 2] = Z;
        this.vertexs[this.nbCoords + 3] = Face.QUAD_UV[vertexIndex * 2];
        this.vertexs[this.nbCoords + 4] = Face.QUAD_UV[vertexIndex * 2 + 1];
        this.vertexs[this.nbCoords + 5] = TexturesManager.getTextureRender().getIndex(texture);
        this.nbCoords += 6;
    }
    
    /**
     * Supprime un sommet du tableau de sommet grâce à l'index du x, supprime le y et le z après le x.
     * @param int
     * @throws IncorrectIndex
     * @throws ArrayEmpty
     */
    public void delVertex(int x) throws IncorrectIndex, ArrayEmpty{
        if (x < 0 || x > this.vertexs.length - 3){
            throw new IncorrectIndex("L'index doit être compris entre 0 et 9");
        }else if (this.nbCoords == 0){
            throw new ArrayEmpty();
        }
        this.delGap(x);        
        this.nbCoords -= 3;      
    }    

    // fait le décalage des valeurs dans le tableau.
    private void delGap(int index) throws IncorrectIndex{
        if (index < 0 || index >= this.vertexs.length){
            throw new IncorrectIndex("L'index doit être compris entre 0 et 3");
        }
        // -3 car trois composant x, y, z
        for (int i = index; i < this.vertexs.length - 3; i++){
            this.vertexs[i] = this.vertexs[i + 3];
        }
    }
}
