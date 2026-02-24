package demo.Enum;

import demo.Geometry.Cube;

/**
 * Liste de blocks qui existent
 */
public enum BlocksType {
    AIR,
    GRASS,
    COBBLESTONE,
    STONE,
    DIRT,
    MAX,
    HOMER,
    NULL;

    private Cube cube;

    public Cube getCube(){
        if (this.cube == null && this != AIR){
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
