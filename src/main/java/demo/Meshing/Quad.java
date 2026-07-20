package demo.Meshing;

import demo.Others.BlockDefinition;
import demo.Others.Blocks;

/**
 * Définit un carré depuis le greedyMeshing
 */
public class Quad {
    /** Position de départ en x. */
    public int x;
    /** Position de départ en y. */
    public int y;
    /** Position de départ en z. */
    public int z;
    /** Largeur du quad sur l'axe X. */
    public int width;
    /** Hauteur du quad sur l'axe Y. */
    public int height;
    /** Profondeur du quad sur l'axe Z. */
    public int depth;
    /** Direction/face du quad (0-5). */
    public int direction;
    /** Type de bloc représenté par ce quad. */
    public BlockDefinition type = Blocks.NULL;
    /** Valeur d'ambient occlusion du coin A. */
    public int ao0;
    /** Valeur d'ambient occlusion du coin B. */
    public int ao1;
    /** Valeur d'ambient occlusion du coin C. */
    public int ao2;
    /** Valeur d'ambient occlusion du coin D. */
    public int ao3;
    /** Niveau de lumière du ciel du quad. */
    public int skyLight;
    /** Niveau de lumière de bloc du quad. */
    public int blockLight;
    /** Identifiant de texture du quad. */
    public int textureId;

    /**
     * Créer un nouveau quad
     * @param x position de départ en x
     * @param y position de départ en y
     * @param z position de départ en z
     * @param width largeur sur l'axe X
     * @param height hauteur sur l'axe Y
     * @param depth profondeur sur l'axe Z
     * @param direction direction/face du quad (0-5)
     * @param ao0 ambient occlusion du coin A
     * @param ao1 ambient occlusion du coin B
     * @param ao2 ambient occlusion du coin C
     * @param ao3 ambient occlusion du coin D
     * @param skyLight niveau de lumière du ciel
     * @param blockLight niveau de lumière de bloc
     * @param textureId identifiant de texture
     */
    public Quad(int x, int y, int z, int width, int height, int depth,
                int direction, int ao0, int ao1, int ao2, int ao3, 
                int skyLight, int blockLight, int textureId){
        this.x           = x;
        this.y           = y;
        this.z           = z;
        this.width       = width;
        this.height      = height;
        this.depth       = depth;
        this.direction   = direction;
        this.ao0         = ao0;
        this.ao1         = ao1;
        this.ao2         = ao2;
        this.ao3         = ao3;
        this.skyLight    = skyLight;
        this.blockLight  = blockLight;
        this.textureId   = textureId;
    }
}