package demo.Enum;

/**
 * Contient les directions des faces
 */
public enum Directions {
    NORTH,
    SOUTH,    
    WEST,    
    EAST,
    UP,
    DOWN;

    public static Directions fromInt(int value){
        return Directions.values()[value];
    }
}
