package demo.Rendering;

import org.lwjgl.opengl.GL11;

import demo.Annotations.OpenGLThread;
import demo.Geometry.Clone3D;
import demo.Geometry.Node3D;
import demo.Geometry.Object3D;
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
     * Retourne le nombre de sommet dans la scène
     * @return int
     */
    public int getTotalVertexCount(){
        return this.getNumberOfVertex(root);
    }

    /**
     * Retourne le nombre de nœud dans la scène
     * @return int
     */
    public int getTotalNodeCount(){
        return this.getNumberOfNode(root) - 1;
    }

    // calcule récursivement le nombre de sommet
    private int getNumberOfVertex(Node3D node){
        int count = 0;
        // si le nœud est un object3D/Clone et qu'il a un mesh alors on récupère le nombre de sommet du mesh
        if (node instanceof Object3D){
            Object3D obj = (Object3D) node;
            if (obj.mesh != null){
                count += obj.mesh.getVertexCount();         
            }         
        }else if (node instanceof Clone3D){
            Clone3D obj = (Clone3D) node;
            if (obj.getCloned().mesh != null){
                count += obj.getCloned().mesh.getVertexCount();
            }
        }

        // pour chaque enfant du nœud on calcule le nombre de sommet récursivement
        Node3D[] children = node.getChildren();
        if (children != null){
            for (Node3D child : children){
                if (child != null){
                    count += this.getNumberOfVertex(child);
                }
            }
        }
        return count;
    }

    // calcule récursivement le nombre de nœud
    private int getNumberOfNode(Node3D node){
        int count = 1;

        Node3D[] children = node.getChildren();
        if (children != null){
            for (Node3D child : children){
                if (child != null){
                    count += this.getNumberOfNode(child);
                }
            }
        }

        return count;
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
