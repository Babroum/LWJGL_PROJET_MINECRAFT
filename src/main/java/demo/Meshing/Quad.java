package demo.Meshing;

import demo.Enum.BlocksType;
import demo.Enum.Directions;

/**
 * Définit un carré depuis le greedyMeshing
 */
public class Quad {
    public int x; // position de départ
    public int y;
    public int z;       
    public int width;          // largeur sur X
    public int height;         // hauteur sur Y
    public Directions direction;
    public BlocksType type = BlocksType.GRASS;    // pour la texture

    public Quad(int x, int y, int z, int width, int height, Directions direction){
        this.x = x;
        this.y = y;
        this.z = z;
        this.width = width;
        this.height = height;
        this.direction = direction;
    }
}
