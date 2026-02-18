package demo.Meshing;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import demo.ShaderManager;
import demo.Annotations.OpenGLThread;
import demo.Exceptions.ArrayEmpty;
import demo.Exceptions.ArrayFull;
import demo.Exceptions.Empty;
import demo.Exceptions.IncorrectIndex;

/**
 * Une Maille décrivant un ensemble de face composées de sommet.
 */
public class Mesh {

    /**
     * Tableau des faces de la maille.
     */
    private Face[] faces = new Face[6]; // a modif 6 facces par cube
    private FloatBuffer vertexs; 
    private IntBuffer indices;
    private int nbFaces;
    private int nbvertex;
    // ids des vao vbo et ebo si la maille en a besoin
    private int[] ids;

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

    /**
     * Retourne le nombre de sommet de la maille
     * @return int
     */
    public int getVertexCount(){
        return this.nbvertex;
    }
    /**
     * Retourne le nombre de face de la maille
     * @return int
     */
    public int getFaceCount(){
        return this.nbFaces;
    }

    /**
     * Définis les ids des buffers créer avec les vertexs et indices 
     */
    @OpenGLThread
    public void setupVBOVAOEBO(){
        this.ids = new int[3];
        this.ids = ShaderManager.createVBOVAOEBO(this.vertexs, this.indices, 3);
    }

    /**
     * Remplis le FloatBuffer avec tout les sommets de la maille
     */
    // a faire dans un autre thread
    @OpenGLThread
    public void fillFloatBufferVertex(){
        this.vertexs = BufferUtils.createFloatBuffer(this.nbFaces * 4 * 3);
        this.nbvertex = 0;
        synchronized (this.faces){
            for (int i = 0; i < this.nbFaces; i++){
                for (int j = 0; j < this.faces[i].nbCoords; j++){
                    this.vertexs.put(this.faces[i].vertexs[j]);
                }
                this.nbvertex += this.faces[i].nbCoords / 3;
            }
            this.vertexs.flip();
        }       
    }

    /**
     * Remplis le IntBuffer avec tout les indices de la maille<br>
     * Fonctionne seulement pour des faces de 2 triangles
     */
    // a faire dans un autre thread
    @OpenGLThread
    public void fillIntBufferIndices(){
        this.indices = BufferUtils.createIntBuffer(this.nbFaces * 6);
        int[] indicesArray = new int[this.nbFaces * 6];
        for (int i = 0; i < this.nbFaces; i++){
            int v = i * 4;
            indicesArray[i * 6 + 0] = v;
            indicesArray[i * 6 + 1] = v + 1;
            indicesArray[i * 6 + 2] = v + 2;
            indicesArray[i * 6 + 3] = v + 2;
            indicesArray[i * 6 + 4] = v + 3;
            indicesArray[i * 6 + 5] = v;
        }
        this.indices.put(indicesArray);
        this.indices.flip();
    }

    @OpenGLThread
    public void drawMesh() throws Empty{   
        if (this.ids == null) throw new Empty("les vbo vao et ebo ne sont pas initialisé");
        GL30.glBindVertexArray(ids[0]);
        GL11.glDrawElements(GL11.GL_TRIANGLES, this.indices.limit(), GL11.GL_UNSIGNED_INT, 0); // limit c comme length
        GL30.glBindVertexArray(0);             
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