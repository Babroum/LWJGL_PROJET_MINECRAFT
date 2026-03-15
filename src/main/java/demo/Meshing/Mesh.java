package demo.Meshing;

import demo.Exceptions.ArrayFull;

/**
 * Une Maille décrivant un ensemble de face composées de sommet.
 */
public class Mesh {

    private Face[] faces = new Face[6]; // a modif 6 faces par cube
    private int faceCount;
    private int vertexCount;
    private int[] vertexArray;

    /**
     * Ajoute une face au tableau de face.
     * @param face
     * @throws ArrayFull
     */
    public void addFaces(Face face) throws ArrayFull{
        if (this.faceCount == this.faces.length){
            throw new ArrayFull();
        }
        synchronized (this.faces)
        {
            this.faces[this.faceCount] = face;
        }        
        this.faceCount++;
    }

    public int[] getFaceVertex(String direction){
        for(int i = 0; i < faceCount; i++){
            if (this.faces[i].direction == direction){
                return this.faces[i].vertexs;
            }
        }
        return null;
    }

    /**
     * Retourne le tableau des sommets de la maille
     * @return
     */
    public int[] getVertexArray(){
        return this.vertexArray;
    }
    /**
     * Retourne le nombre de sommet de la maille
     * @return int
     */
    public int getVertexCount(){
        return this.vertexCount;
    }
    /**
     * Retourne le nombre de face de la maille
     * @return int
     */
    public int getFaceCount(){
        return this.faceCount;
    }

    /**
     * Créer le tableau de sommet pour la maille 
     */
    public void setupArray(){
        this.vertexArray = new int[this.faceCount * 6 * 7];
    }

    /**
     * Remplis le tableau avec tout les sommets de la maille
     */
    public void fillVertexArray(){
        this.vertexCount = 0;
        int index = 0;
        for (int i = 0; i < this.faceCount; i++){
            // pour chaque données d'une face
            for (int j = 0; j < this.faces[i].nbCoords; j++){
                this.vertexArray[index] = this.faces[i].vertexs[j];
                index++;
            }
            // /7 car 7 données par sommets
            this.vertexCount += this.faces[i].nbCoords / 7;
        }
    }
}