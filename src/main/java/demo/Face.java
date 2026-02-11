package demo;

import demo.Exceptions.ArrayEmpty;
import demo.Exceptions.ArrayFull;
import demo.Exceptions.IncorrectIndex;

/**
 * Une face décrivant un ensemble de 4 Sommets d'un mesh.
 */
public class Face {
    
    /**
     * Tableau des sommets de la face.
     */
    private Vertex[] vertexs = new Vertex[4];
    public int nbVertexs;

    /**
     * Ajoute un sommet grâce à ses coordonnées X, Y, Z au tableau des sommets.
     * @param X
     * @param Y
     * @param Z
     * @throws ArrayFull
     */
    public void addVertex(float X, float Y, float Z) throws ArrayFull{
        this.addVertex(new Vertex(X, Y, Z));
    }
    /**
     * Ajoute un sommet au tableau des sommets.
     * @param vertex
     * @throws ArrayFull
     */
    public void addVertex(Vertex vertex) throws ArrayFull{
        if (this.nbVertexs == this.vertexs.length){
            throw new ArrayFull();
        }
        this.vertexs[this.nbVertexs] = vertex;
        this.nbVertexs++;
    }

    /**
     * Retourne un sommet grâce à un indice.
     * @param index
     * @return Un sommet
     * @throws IncorrectIndex
     */
    public Vertex getVertex(int index) throws IncorrectIndex{
        if (index < 0 || index >= this.vertexs.length){
            throw new IncorrectIndex("L'index doit être compris entre 0 et 3");
        }
        return this.vertexs[index];
    }

    /**
     * Supprime un sommet du tableau de sommet grâce à ses coordonées X, Y, Z.
     * @param X
     * @param Y
     * @param Z
     * @throws IncorrectIndex
     * @throws ArrayEmpty
     */
    public void delVertex(float X, float Y, float Z) throws IncorrectIndex, ArrayEmpty{
        this.delVertex(new Vertex(X, Y, Z));
    }   
    /**
     * Supprime un sommet du tableau de sommet.
     * @param vertex
     * @throws IncorrectIndex
     * @throws ArrayEmpty
     */
    public void delVertex(Vertex vertex) throws IncorrectIndex, ArrayEmpty{
        for(int i = 0; i < this.nbVertexs; i++){
            if (this.vertexs[i].x == vertex.x &&
                this.vertexs[i].y == vertex.y &&
                this.vertexs[i].z == vertex.z 
            ){
                this.delVertex(i);
            }
        }
    }
    /**
     * Supprime un sommet du tableau de sommet grâce à un indice.
     * @param index
     * @throws IncorrectIndex
     * @throws ArrayEmpty
     */
    public void delVertex(int index) throws IncorrectIndex, ArrayEmpty{
        if (index < 0 || index >= this.vertexs.length){
            throw new IncorrectIndex("L'index doit être compris entre 0 et 3");
        }else if (this.nbVertexs == 0){
            throw new ArrayEmpty();
        }
        this.delGap(index);        
        this.nbVertexs--;      
    }    

    // fait le décalage des valeurs dans le tableau.
    private void delGap(int index) throws IncorrectIndex{
        if (index < 0 || index >= this.vertexs.length){
            throw new IncorrectIndex("L'index doit être compris entre 0 et 3");
        }
        for (int i = index; i < this.vertexs.length - 1; i++){
            this.vertexs[i] = this.vertexs[i + 1];
        }
    }
}
