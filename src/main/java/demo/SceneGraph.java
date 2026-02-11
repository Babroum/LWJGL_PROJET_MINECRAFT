package demo;

import org.lwjgl.opengl.GL11;

/**
 * Graph de la scène.
 */
public class SceneGraph {

    // gérer couleur background
    public final Node3D root = new Node3D();
    private Color4i backGroundColor = Color4i.WHITE;

    public void setBackGroundColor(Color4i color){
        this.backGroundColor = color;
    }

    public Color4i getBackGroundColor(){
        return this.backGroundColor;
    }

    @OpenGLThread
    public void drawScene(){
        // Couleur du background de la scene
        GL11.glClearColor(this.backGroundColor.red, this.backGroundColor.green, this.backGroundColor.blue, 1f);
        this.root.drawNode();
    }
}
