package demo;

import org.lwjgl.opengl.GL11;

import demo.Exceptions.ArrayFull;

/**
 * Noeud définit par des coordonées, angles et tailles.
 * Le noeud est le parent de chaque enfant, 
 * il permet aux enfants d'avoir des coordonées local.
 */
public class Node3D {
    /**
     * Coordonée X du noeud.
     */
    public float x = 0;
    /**
     * Coordonée Y du noeud.
     */
    public float y = 0;
    /**
     * Coordonée Z du noeud.
     */
    public float z = 0;

    /**
     * Angle X du noeud.
     */
    public float angleX = 0;
    /**
     * Angle Y du noeud.
     */
    public float angleY = 0;
    /**
     * Angle Z du noeud.
     */
    public float angleZ = 0;

    /**
     * Taille X du noeud.
     */
    public float scaleX = 1;
    /**
     * Taille Y du noeud.
     */
    public float scaleY = 1;
    /**
     * Taille Z du noeud.
     */
    public float scaleZ = 1;

    /**
     * Tableau de noeud enfant.
     */
    private Node3D[] children = new Node3D[100];
    private int nbChildren;

    /**
     * Ajoute un noeud au tableau de noeud.
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
     * Dessine le noeud actuel et tous ses enfants.
     */
    @OpenGLThread
    public void drawNode(){
        // On sauvegarde la matrice actuelle
        GL11.glPushMatrix();

        // *** Position du noeud ***
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

        // Placement des noeuds enfants relativement à la position de ce noeud
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
