package demo;

import org.lwjgl.opengl.GL11;

import demo.Annotations.OpenGLThread;
import demo.Geometry.Node3D;
import demo.Types.Color4f;

/**
 *La scène.
 */
public class SceneGraph {

    public final Node3D root = new Node3D();
    private Color4f backGroundColor = Color4f.WHITE;    
    private Camera camera = new Camera();

    /**
     * Définis la couleur de la scène
     * @param color Color4f
     */
    public void setBackGroundColor(Color4f color){
        this.backGroundColor = color;
    }

    /**
     * Retourne la couleur de la scène
     * @return Color4f
     */
    public Color4f getBackGroundColor(){
        return this.backGroundColor;
    }

    /**
     * Retourne la camera de la scène
     * @return Camera
     */
    public Camera getCamera(){
        return this.camera;
    }

    /**
     * Dessine la scène en appelant le nœud principale
     */
    @OpenGLThread
    public void drawScene(){
        // Couleur du background de la scène
        GL11.glClearColor(this.backGroundColor.red, this.backGroundColor.green, this.backGroundColor.blue, 1f);
        this.root.drawNode();
    }
}
