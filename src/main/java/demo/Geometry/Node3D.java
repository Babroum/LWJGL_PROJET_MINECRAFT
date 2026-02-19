package demo.Geometry;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;

import demo.ShaderManager;
import demo.Annotations.OpenGLThread;
import demo.Exceptions.ArrayFull;

/**
 * Un nœud définit par des coordonnées, angles et tailles.
* Le nœud est le parent de chaque enfant, 
* il permet aux enfants d'avoir des coordonnées locales.
 */
public class Node3D {
    /**
     * Coordonnée X du nœud.
     */
    public int x = 0;
    /**
     * Coordonnée Y du nœud.
     */
    public int y = 0;
    /**
     * Coordonnée Z du nœud.
     */
    public int z = 0;

    /**
     * Angle X du nœud.
     */
    public int angleX = 0;
    /**
     * Angle Y du nœud.
     */
    public int angleY = 0;
    /**
     * Angle Z du nœud.
     */
    public int angleZ = 0;

    /**
     * Taille X du nœud.
     */
    public int scaleX = 1;
    /**
     * Taille Y du nœud.
     */
    public int scaleY = 1;
    /**
     * Taille Z du nœud.
     */
    public int scaleZ = 1;

    /**
     * Tableau de nœud enfant.
     */
    private Node3D[] children = new Node3D[100];
    private FloatBuffer buffer = BufferUtils.createFloatBuffer(16);
    private int nbChildren;

    /**
     * Ajoute un nœud au tableau de nœud.
     * @param node
     * @throws ArrayFull
     */
    public void addChild(Node3D node) throws ArrayFull{
        if (this.nbChildren == this.children.length){
            throw new ArrayFull();
        }
        synchronized (this.children)
        {
            this.children[this.nbChildren] = node;
        }       
        this.nbChildren++;
    }

    /**
     * Dessine le nœud actuel et tous ses enfants.
     * @param Matrix4f , si un nœud parent est bougé les enfants vont le suivre
     */
    @OpenGLThread
    public void drawNode(Matrix4f parentModel){
        // Crée la model matrix   
        Matrix4f model = new Matrix4f(parentModel)
            .translate(this.x, this.y, this.z) // Position aux coordonnées X, Y, Z
            .rotateX((float) Math.toRadians(this.angleX)) // Rotation autour de l'axe X
            .rotateY((float) Math.toRadians(this.angleY)) // Rotation autour de l'axe Y
            .rotateZ((float) Math.toRadians(this.angleZ)) // Rotation autour de l'axe Z
            .scale(this.scaleX, this.scaleY, this.scaleZ); // Application des facteurs d'échelle
        model.get(this.buffer);

        ShaderManager.runModel(this.buffer);

        // Dessin spécifique
        this.drawSpecific();

        // Placement des nœuds enfants relativement à la position de ce nœud
        synchronized (this.children)
        {
            for (int i = 0; i < this.nbChildren; i++){
                this.children[i].drawNode(model);
            }
        }          
    }

    /**
     * Retourne le tableau d'enfant du nœud
     * @return
     */
    public Node3D[] getChildren(){
        return this.children;
    }

    /**
     * Dessin supplémentaire pour le nœud 
     * <p>
     * A surcharger par les classes enfants
     * <p>
     * Ne fait rien par défaut
     */
    @OpenGLThread
    public void drawSpecific(){
    }
}
