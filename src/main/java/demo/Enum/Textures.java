package demo.Enum;

public enum Textures {
    GRASS, // nom du bloc
    GRASS_TOP, // type de texture que peut avoir grass
    GRASS_SIDE,
    COBBLESTONE,
    STONE,
    DIRT,
    MAX,
    NULL;

    public Textures getTexture(Directions direction){
        switch(this){ // this = le type de bloc ex: GRASS
            case GRASS:
                if(direction == Directions.UP)     return Textures.GRASS_TOP;
                if(direction == Directions.DOWN)   return Textures.DIRT;
                return Textures.GRASS_SIDE;
            case STONE:
                return Textures.STONE;
            default:
                return this;
    }
}
}
