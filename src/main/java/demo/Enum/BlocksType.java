package demo.Enum;

import demo.Geometry.Cube;

/**
 * Liste de blocks qui existent
 */
public enum BlocksType {
    GRASS(1),
    COBBLESTONE(2),
    STONE(3),
    DIRT(4),
    MAX(5),
    HOMER(6),
    NULL(0);

    private Cube cube;
    private final int id;

    BlocksType(int id) { this.id = id; }

    public int getId() { return id; }

    public static BlocksType fromId(int id) {
        for (BlocksType t : values()) {
            if (t.id == id) return t;
        }
        return NULL;
    }

    public Cube getCube(){
        if (this.cube == null){
            this.cube = new Cube(this);
        }
        return this.cube;
    }

    public TexturesType getTexture(Directions direction){
        switch(this){ // this = le type de bloc ex: GRASS
            case GRASS:
                if(direction == Directions.UP)     return TexturesType.GRASS_TOP;
                if(direction == Directions.DOWN)   return TexturesType.DIRT;
                return TexturesType.GRASS_SIDE; // le type de texture à appliqué
            case STONE:
                return TexturesType.STONE;
            default:
                return TexturesType.valueOf(this.name());
        }
    }
}
