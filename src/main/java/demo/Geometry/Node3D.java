package demo.Geometry;

import java.nio.FloatBuffer;

import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;

import demo.Annotations.OpenGLThread;
import demo.Managers.ShadersManager;

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

    // PRIVATE CAR PAS NECESSAIRE DANS UN JEU MINECRAFT .___.
    
    /*
    private int angleX = 0;
    private int angleY = 0;
    private int angleZ = 0;

    private int scaleX = 1;
    private int scaleY = 1;
    private int scaleZ = 1;
    */

    /**
     * Tableau de nœud enfant.
     */
    private Node3D[] children = null; // alloué seulement si addChild() est appelé
    /**
     * Matrice de transformation du nœud, réutilisée à chaque frame (zéro allocation).
     */
    private final Matrix4f model = new Matrix4f(); // réutilisé à chaque frame, zéro allocation
    /**
     * Buffer natif (hors du tas Java) contenant la matrice modèle envoyée au shader.
     */
    public FloatBuffer buffer = MemoryUtil.memAllocFloat(16);
    /**
     * Nombre d'enfants actuellement présents dans {@link #children}.
     */
    private int nbChildren;

    /** Constructeur implicite vide */
    public Node3D(){}

    /**
     * Ajoute un nœud au tableau de nœud.
     * @param node nœud à ajouté au nœud actuel
     * @throws RuntimeException si le tableau d'enfants est plein
     */
    public void addChild(Node3D node){
        // Alloue le tableau seulement au premier addChild()
        if (this.children == null){
        this.children = new Node3D[SubChunk.MAXBLOCK]; //SubChunk.MAXBLOCK
        }
        if (this.nbChildren == this.children.length){
            throw new RuntimeException("Array Full");
        }

        this.children[this.nbChildren] = node;     
        this.nbChildren++;
    }

    /**
     * Retire un nœud enfant du tableau d'enfants.
     * Ne fait rien si le nœud n'est pas trouvé.
     * @param node nœud enfant à retirer
     */
    public void removeChild(Node3D node) {
        for (int i = 0; i < nbChildren; i++) {
            if (children[i] == node) {
                children[i] = children[--nbChildren]; // swap avec le dernier
                children[nbChildren] = null;
                return;
            }
        }
    }

    /**
     * Dessine le nœud actuel et tous ses enfants.
     * @param parentModel Matrix4f, si un nœud parent est bougé les enfants vont le suivre
     */
    @OpenGLThread
    public void drawNode(Matrix4f parentModel){
        // Crée la model matrix   
        // Placement des nœuds enfants relativement à la position de ce nœud
        this.model.set(parentModel)
            .translate(this.x, this.y, this.z); // Position aux coordonnées X, Y, Z
            //.rotateX((float) Math.toRadians(this.angleX)) // Rotation autour de l'axe X
            //.rotateY((float) Math.toRadians(this.angleY)) // Rotation autour de l'axe Y
            //.rotateZ((float) Math.toRadians(this.angleZ)) // Rotation autour de l'axe Z
            //.scale(this.scaleX, this.scaleY, this.scaleZ); // Application des facteurs d'échelle
        this.model.get(this.buffer);
        ShadersManager.runModel(this.buffer);

        // Dessin spécifique
        this.drawSpecific();

        if (this.children != null){
            for (int i = 0; i < this.nbChildren; i++) {
                this.children[i].drawNode(this.model);
            }
        }   
    }

    /**
     * Retourne le tableau d'enfant du nœud
     * @return le tableau des nœuds enfants, ou un tableau vide si aucun enfant n'a été ajouté
     */
    public Node3D[] getChildren(){
        return this.children != null ? this.children : new Node3D[0];
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

    /**
     * Libère le buffer natif {@link #buffer} alloué hors du tas Java.
     * A appeler quand le nœud n'est plus utilisé pour éviter les fuites mémoire.
     */
    public void cleanup(){
        if (this.buffer != null){
            MemoryUtil.memFree(this.buffer);
            this.buffer = null;
        }
    }
}