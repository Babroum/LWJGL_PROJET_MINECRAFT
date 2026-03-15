package demo.Meshing;

import demo.BlockDefinition;
import demo.Blocks;

/**
 * Définit un carré depuis le greedyMeshing
 */
public class Quad {
    public int x; // position de départ
    public int y;
    public int z;       
    public int width; // largeur sur X
    public int height; // hauteur sur Y
    public String direction;
    public BlockDefinition type = Blocks.GRASS;
    public int ao0, ao1, ao2, ao3; // coins A, B, C, D

    public Quad(int x, int y, int z, int width, int height, String direction,
                int ao0, int ao1, int ao2, int ao3){
        this.x = x;
        this.y = y;
        this.z = z;
        this.width = width;
        this.height = height;
        this.direction = direction;
        this.ao0 = ao0;
        this.ao1 = ao1;
        this.ao2 = ao2;
        this.ao3 = ao3;
    }
}
