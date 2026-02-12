package demo;

import org.lwjgl.opengl.GL11;

import demo.Types.Color4f;

/**
 * Graph de la scène.
 */
public class SceneGraph {

    // gérer couleur background
    public final Node3D root = new Node3D();
    private Color4f backGroundColor = Color4f.WHITE;

    /**
     * Définit la couleur de la scene
     * @param color Color4f
     */
    public void setBackGroundColor(Color4f color){
        this.backGroundColor = color;
    }

    /**
     * Retourne la couleur de la scene
     * @return Color4f
     */
    public Color4f getBackGroundColor(){
        return this.backGroundColor;
    }

    /**
     * Dessine la scene en appelant le noeud principale
     */
    @OpenGLThread
    public void drawScene(){
        // Couleur du background de la scene
        GL11.glClearColor(this.backGroundColor.red, this.backGroundColor.green, this.backGroundColor.blue, 1f);
        this.root.drawNode();
    }
}
