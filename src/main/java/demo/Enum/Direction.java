package demo.Enum;

/**
 * Contient les directions des faces et les valeurs de directions
 */
public enum Direction {
    NORTH(-1, 0, 0),
    SOUTH(1, 0, 0),
    UP(0, 1, 0),
    DOWN(0, -1, 0),
    EAST(0, 0, 1),
    WEST(0, 0, -1);

    public final int dx, dy, dz;

    Direction(int dx, int dy, int dz) {
        this.dx = dx;
        this.dy = dy;
        this.dz = dz;
    }
    // Utilisation : Direction.NORTH.dx
}
