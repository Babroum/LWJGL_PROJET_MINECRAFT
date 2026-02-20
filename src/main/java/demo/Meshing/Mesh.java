package demo.Meshing;

import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import demo.Annotations.OpenGLThread;
import demo.Exceptions.ArrayFull;
import demo.Exceptions.Empty;
import demo.Exceptions.IncorrectIndex;
import demo.Managers.BuffersManager;
import demo.Managers.TexturesManager;
import demo.Rendering.Render;

/**
 * Une Maille décrivant un ensemble de face composées de sommet.
 */
public class Mesh {

    // DELETE
    private IntBuffer vertexs; 
    private IntBuffer indices;
    // DELETE
    private int[] ids; // ids des vao vbo et ebo si la maille en a besoin

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

    // DELETE
    public void setupBuffers(){
        this.vertexs = BufferUtils.createIntBuffer(this.faces.length * 4 * 7); // 4 sommets, 7 données
        this.indices = BufferUtils.createIntBuffer(this.faces.length * 6); // 6 indices par face
    }

    /**
     * Définis les ids des buffers créer avec les sommets et indices 
     */
    // DELETE
    @OpenGLThread
    public void setupVBOVAOEBO(){
        this.ids = new int[3];
        this.ids = BuffersManager.createVBOVAOEBO(this.vertexs, this.indices, 6);
    }

    /**
     * Remplis le FloatBuffer avec tout les sommets de la maille
     */
    // DELETE
    public void fillIntBufferVertex(){
        this.vertexs.clear();
        this.vertexCount = 0;
        synchronized (this.faces){
            for (int i = 0; i < this.faceCount; i++){
                for (int j = 0; j < this.faces[i].nbCoords; j++){
                    this.vertexs.put(this.faces[i].vertexs[j]);
                }
                this.vertexCount += this.faces[i].nbCoords / 7;
            }
            this.vertexs.flip();
        }
    }   
    /**
     * Remplis le tableau avec tout les sommets de la maille
     */
    public void fillVertexArray(){
        this.vertexCount = 0;
        for (int i = 0; i < this.faceCount; i++){
            for (int j = 0; j < this.faces[i].nbCoords; j++){
                this.vertexArray[this.vertexCount] = this.faces[i].vertexs[j];
            }
            this.vertexCount += this.faces[i].nbCoords / 7;
        }
    }

    /**
     * Remplis le IntBuffer avec tout les indices de la maille<br>
     * Fonctionne seulement pour des faces de 2 triangles
     */
    // DELETE
    public void fillIntBufferIndices(){
        this.indices.clear();
        int[] indicesArray = new int[this.faceCount * 6];
        for (int i = 0; i < this.faceCount; i++){
            int v = i * 4;
            indicesArray[i * 6 + 0] = v;
            indicesArray[i * 6 + 1] = v + 1;
            indicesArray[i * 6 + 2] = v + 2;
            indicesArray[i * 6 + 3] = v + 2;
            indicesArray[i * 6 + 4] = v + 3;
            indicesArray[i * 6 + 5] = v;
        }
        this.indices.put(indicesArray).flip();
    }

    /**
     * Dessine la maille si les vbo vao et ebo ont bien étaient initialisé
     * @throws Empty
     */
    // DELETE
    @OpenGLThread
    public void drawMesh() throws Empty{   
        if (this.ids == null) throw new Empty("les vbo vao et ebo ne sont pas initialisé");
        TexturesManager.getTextureRender().draw();
        GL30.glBindVertexArray(ids[1]);
        GL11.glDrawElements(GL11.GL_TRIANGLES, this.indices.limit(), GL11.GL_UNSIGNED_INT, 0); // limit c comme length
        GL30.glBindVertexArray(0);    
        // enregistre le nombre d'appele de dessin 
        Render.monitor.incrementDrawCalls();
        // et le nombre de triangle dessiné
        Render.monitor.addTriangles(this.indices.limit() / 3);                     
    }        
}