package demo.Geometry;

import demo.Annotations.OpenGLThread;
import demo.Meshing.Mesh;
import demo.Types.Color4f;

public class Object3D extends Node3D{

    private Color4f color = new Color4f(0);
    public Mesh mesh = new Mesh();

    @OpenGLThread
    public void setupObjectBuffers(){
        this.mesh.fillFloatBufferVertex();
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
     * Application de la couleur,
     * <br>
     * et dessin de la maille.
     */
    @OpenGLThread
    @Override
    public void drawSpecific(){
        this.color.apply();
        this.mesh.drawMesh();
    }
}
