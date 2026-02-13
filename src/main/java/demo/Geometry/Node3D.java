package demo.Geometry;

import org.lwjgl.opengl.GL11;

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
    public float x = 0;
    /**
     * Coordonnée Y du nœud.
     */
    public float y = 0;
    /**
     * Coordonnée Z du nœud.
     */
    public float z = 0;

    /**
     * Angle X du nœud.
     */
    public float angleX = 0;
    /**
     * Angle Y du nœud.
     */
    public float angleY = 0;
    /**
     * Angle Z du nœud.
     */
    public float angleZ = 0;

    /**
     * Taille X du nœud.
     */
    public float scaleX = 1;
    /**
     * Taille Y du nœud.
     */
    public float scaleY = 1;
    /**
     * Taille Z du nœud.
     */
    public float scaleZ = 1;

    /**
     * Tableau de nœud enfant.
     */
    private Node3D[] children = new Node3D[100];
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
     */
    @OpenGLThread
    public void drawNode(){
        // On sauvegarde la matrice actuelle
        GL11.glPushMatrix();

        // *** Position du nœud ***
        // Position aux coordonnées X, Y, Z
        GL11.glTranslatef(this.x, this.y, this.z);
        // Rotation autour de l'axe X
        GL11.glRotatef(this.angleX, 1f, 0f, 0f);
        // Rotation autour de l'axe Y
        GL11.glRotatef(this.angleY, 0f, 1f, 0f);
        // Rotation autour de l'axe Z
        GL11.glRotatef(this.angleZ, 0f, 0f, 1f);
        // Application des facteurs d'échelle
        GL11.glScalef(this.scaleX, this.scaleY, this.scaleZ);

        // Dessin spécifique
        this.drawSpecific();

        // Placement des nœuds enfants relativement à la position de ce nœud
        synchronized (this.children)
        {
            for (int i = 0; i < this.nbChildren; i++){
                this.children[i].drawNode();
            }
        }          

        // On restaure la matrice sauvegardée
        GL11.glPopMatrix();
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
