package demo.Meshing;

import demo.Exceptions.ArrayEmpty;
import demo.Exceptions.ArrayFull;
import demo.Exceptions.IncorrectIndex;
import demo.Enum.Direction;

/**
 * Une face décrivant un ensemble de 4 Sommets d'une maille.
 */
public class Face {
    
    /**
     * Tableau des sommets de la face.
     */
    // {x1, y1, z1, x2, y2, z2, ...}
    public float[] vertexs = new float[12];
    public Direction direction;
    public int nbVertexs;

    /**
     * Ajoute un sommet grâce à ses coordonnées X, Y, Z au tableau des sommets.
     * @param X
     * @param Y
     * @param Z
     * @throws ArrayFull
     */
    public void addVertex(float X, float Y, float Z) throws ArrayFull{
        if (this.nbVertexs == this.vertexs.length){
            throw new ArrayFull();
        }
        this.vertexs[this.nbVertexs] = X;
        this.vertexs[this.nbVertexs + 1] = Y;
        this.vertexs[this.nbVertexs + 2] = Z;
        this.nbVertexs += 3;
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
        }else if (this.nbVertexs == 0){
            throw new ArrayEmpty();
        }
        this.delGap(x);        
        this.nbVertexs--;      
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
