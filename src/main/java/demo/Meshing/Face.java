package demo.Meshing;

import demo.Exceptions.ArrayEmpty;
import demo.Exceptions.ArrayFull;
import demo.Exceptions.IncorrectIndex;
import demo.Rendering.Render;
import demo.Enum.Directions;
import demo.Enum.Textures;

/**
 * Une face décrivant un ensemble de 4 Sommets d'une maille.
 */
public class Face {
    
    /**
     * Tableau des sommets de la face.
     */
    // {x1, y1, z1, u1, v1,  x2, y2, z2, u2, v2, ...}
    public float[] vertexs = new float[20];
    public Textures texture;
    public Directions direction;
    public int nbCoords; // nombre de coordonnées dans la face

    /**
     * Ajoute un sommet grâce à ses coordonnées X, Y, Z au tableau des sommets.
     * @param X
     * @param Y
     * @param Z
     * @throws ArrayFull
     */
    public void addVertex(float X, float Y, float Z) throws ArrayFull{
        if (this.nbCoords == this.vertexs.length){
            throw new ArrayFull();
        }
        this.vertexs[this.nbCoords] = X;
        this.vertexs[this.nbCoords + 1] = Y;
        this.vertexs[this.nbCoords + 2] = Z;
        this.vertexs[this.nbCoords + 3] = 0;
        this.vertexs[this.nbCoords + 4] = 0;
        this.nbCoords += 5;
    }

    public void addUV(){
        float[] uvs = Render.getTextureRender().getUV(this.texture);
        if (this.nbCoords == 0) {System.out.println("Face: addUV: Pas assez de vertex"); return;}
        for (int i = 0, j = 3; i < uvs.length && j < this.nbCoords; i+=2, j+=5){
            this.vertexs[j] = uvs[i];
            this.vertexs[j + 1] = uvs[i + 1];
        }
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
