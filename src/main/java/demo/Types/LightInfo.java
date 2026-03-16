package demo.Types;

/**Information sur une lumière */
public class LightInfo {
    /**
     * Coordonnées en x (global)
     */
    public int x;
    /**
     * Coordonnées en y (global)
     */
    public int y;
    /**
     * Coordonnées en z (global)
     */
    public int z;
    /**
     * Intensité de lumière
     */
    public byte light;
    /**
     * Type de lumière, ciel (0) et block (1)
     */
    public byte type;

     /**
     * Encode (x,y,z,light,type) pour les files BFS. <br>
     * @param x Coordonnées en x
     * @param y Coordonnées en y
     * @param z Coordonnées en z
     * @param light Intensité de lumière
     * @param type Type de lumière, ciel(0) ou block(1)
     */
    public LightInfo(int x, int y, int z, byte light, byte type){
        this.x = x;
        this.y = y;
        this.z = z;
        this.light = light;
        this.type = type;
    }
}
