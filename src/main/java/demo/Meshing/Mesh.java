package demo.Meshing;

import org.lwjgl.opengl.GL11;

import demo.Annotations.OpenGLThread;
import demo.Exceptions.ArrayEmpty;
import demo.Exceptions.ArrayFull;
import demo.Exceptions.IncorrectIndex;

/**
 * Une Maille décrivant un ensemble de face composées de sommet.
 */
public class Mesh {

    /**
     * Tableau des faces de la maille.
     */
    private Face[] faces = new Face[1000];
    public int nbFaces;

    /**
     * Ajoute une face au tableau de face.
     * @param face
     * @throws ArrayFull
     */
    public void addFaces(Face face) throws ArrayFull{
        if (this.nbFaces == this.faces.length){
            throw new ArrayFull();
        }
        synchronized (this.faces)
        {
            this.faces[this.nbFaces] = face;
        }        
        this.nbFaces++;
    }

    /**
     * Retourne une face grâce à un indice.
     * @param index
     * @return
     * @throws IncorrectIndex
     */
    public Face getFace(int index) throws IncorrectIndex{
        if (index < 0 || index >= this.faces.length){
            throw new IncorrectIndex("L'index doit être compris entre 0 et 999");
        }
        return this.faces[index];
    }
    // pas opti
    /**
     * @deprecated Pas optimisé, ne fait rien.
     * @param face
     * @throws IncorrectIndex
     * @throws ArrayEmpty
     */
    public void delFace(Face face) throws IncorrectIndex, ArrayEmpty{
        // à faire si besoin
    }
    /**
     * Supprime une face du tableau de face.
     * @param index
     * @throws IncorrectIndex
     * @throws ArrayEmpty
     */
    public void delFace(int index) throws IncorrectIndex, ArrayEmpty{
        if (index < 0 || index >= this.faces.length){
            throw new IncorrectIndex("L'index doit être compris entre 0 et 999");
        }else if (this.nbFaces == 0){
            throw new ArrayEmpty();
        }
        this.delGap(index);
        this.nbFaces--;
    }

    @OpenGLThread
    public void drawMesh(){
        synchronized (this.faces)
        {
            for(int i = 0; i < this.nbFaces; i++){
                // Annonce à OpenGL le début du polygone
                GL11.glBegin(GL11.GL_POLYGON);

                for(int j = 0; j < this.faces[i].nbVertexs; j++){
                    // Dessine chaque sommet de chaque face de la maille
                    GL11.glVertex3f(this.faces[i].getVertex(j).x,
                                    this.faces[i].getVertex(j).y,
                                    this.faces[i].getVertex(j).z); 
                }

                // Annonce à OpenGL la fin du polygone
                GL11.glEnd();
            } 
        }        
    }

    // fait le décalage des valeurs dans le tableau
    private void delGap(int index) throws IncorrectIndex{
        if (index < 0 || index >= this.faces.length){
            throw new IncorrectIndex("L'index doit être compris entre 0 et 999");
        }
        for (int i = index; i < this.faces.length - 1; i++){
            this.faces[i] = this.faces[i + 1];
        }
    }
}