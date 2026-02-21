package demo.Geometry;

import demo.Annotations.OpenGLThread;
import demo.Enum.BlocksType;
import demo.Meshing.Mesh;

public class Object3D extends Node3D{

    public Mesh mesh = new Mesh();
    public BlocksType blockType = BlocksType.AIR;

    @OpenGLThread
    /**
     * Remplit les buffer avec les vertex et indice <br>
     * Puis créer les vbo vao ebo avec les buffers précédement remplit
     */
    public void setupObjectBuffers(){
        //DELETE
        /*
        this.mesh.fillIntBufferVertex();
        this.mesh.fillIntBufferIndices();
        this.mesh.setupVBOVAOEBO();*/
        this.mesh.setupArray();
        this.mesh.fillVertexArray();
    }
}
