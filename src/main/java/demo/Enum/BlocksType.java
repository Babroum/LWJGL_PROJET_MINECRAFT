package demo.Enum;

/**
 * Liste de blocks qui existent
 */
public enum BlocksType {
    GRASS,
    COBBLESTONE,
    STONE,
    DIRT,
    MAX,
    HOMER,
    NULL;

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
