package demo.Geometry;

import demo.Annotations.OpenGLThread;
import demo.Meshing.Mesh;
import demo.Types.Color4f;

public class Object3D extends Node3D{

    private Color4f color = Color4f.WHITE;
    public Mesh mesh = new Mesh();

    @OpenGLThread
    /**
     * Remplit les buffer avec les vertex et indice <br>
     * Puis créer les vbo vao ebo avec les buffers précédement remplit
     */
    public void setupObjectBuffers(){
        this.mesh.setupBuffers();
        this.mesh.fillIntBufferVertex();
        this.mesh.fillIntBufferIndices();
        this.mesh.setupVBOVAOEBO();
    }

    /**
     * Définis la couleur de l'objet3D
     * @param color Color4f
     */
    public void setColor(Color4f color){
        this.color = color;
    }

    /**
     * Retourne la couleur de l'objet3D
     * @return Color4f
     */
    public Color4f getColor(){
        return this.color;
    }

    /**
     * Applique la couleur et dessine la maille
     */
    @OpenGLThread
    @Override
    public void drawSpecific(){
        this.color.apply();
        this.mesh.drawMesh();
    }
}
