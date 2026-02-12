package demo;

import demo.Types.Color4f;

public class Object3D extends Node3D{

    private Color4f color = new Color4f(0);
    public Mesh mesh = new Mesh();

    public void setColor(Color4f color){
        this.color = color;
    }

    public Color4f getColor(){
        return this.color;
    }

    /**
     * Dessin spécifique.
     */
    @OpenGLThread
    @Override
    public void drawSpecific(){
        this.color.apply();
        this.mesh.drawMesh();
    }
}
