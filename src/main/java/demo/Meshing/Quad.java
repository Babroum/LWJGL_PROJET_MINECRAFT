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
    public int width; // largeur sur X
    public int height; // hauteur sur Y
    public Directions direction;
    public BlocksType type = BlocksType.GRASS;
    public int ao0, ao1, ao2, ao3; // coins A, B, C, D
    public int light0, light1, light2, light3;

    public Quad(int x, int y, int z, int width, int height, Directions direction,
                int ao0, int ao1, int ao2, int ao3,
                int light0, int light1, int light2, int light3){
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
        this.light0 = light0;
        this.light1 = light1;
        this.light2 = light2;
        this.light3 = light3;
    }
}
