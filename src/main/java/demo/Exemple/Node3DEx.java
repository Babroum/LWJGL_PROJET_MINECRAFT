package demo.Exemple;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.lwjgl.opengl.GL11;

/**
 * Nœud virtuel de la scènescène
 */
public class Node3DEx {
    /**
     * Coordonne X de la position dans la 3D
     */
    public float x = 0f;
    /**
     * Coordonne Y de la position dans la 3D
     */
    public float y = 0f;
    /**
     * Coordonne Z de la position dans la 3D
     */
    public float z = 0f;

    /**
     * Angle de la rotation autour de l'axe X
     */
    public float angleX = 0f;
    /**
     * Angle de la rotation autour de l'axe Y
     */
    public float angleY = 0f;
    /**
     * Angle de la rotation autour de l'axe Z
     */
    public float angleZ = 0f;

    /**
     * Facteur d'échelle le long de l'axe X <br/>
     * La valeur doit toujours être strictement positive
     */
    public float scaleX = 1f;
    /**
     * Facteur d'échelle le long de l'axe Y <br/>
     * La valeur doit toujours être strictement positive
     */
    public float scaleY = 1f;
    /**
     * Facteur d'échelle le long de l'axe Z <br/>
     * La valeur doit toujours être strictement positive
     */
    public float scaleZ = 1f;

    /**
     * Liste des nœuds placés relativement à celui-ci
     */
    private final List<Node3DEx> children = new ArrayList<>();

    public Node3DEx()
    {

    }

    /**
     * Ajoute un nœud enfant
     */
    public final void addChild(final Node3DEx node)
    {
        Objects.requireNonNull(node, "node must not be null");

        // On synchronize pour éviter l'accès concurrent
        synchronized (this.children)
        {
            this.children.add(node);
        }
    }

    /**
     * Dessine le noeud
     */
    @ThreadOpenGLEx
    final void drawNode()
    {
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
        // On synchronize pour éviter l'accès concurrent
        synchronized (this.children)
        {
            for (final Node3DEx child : this.children)
            {
                child.drawNode();
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
    @ThreadOpenGLEx
    void drawSpecific()
    {
        // Rien à faire d'autres. Les classes enfants y mettront leur code spécifique
    }
}
