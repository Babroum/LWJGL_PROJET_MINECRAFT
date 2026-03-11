package demo.Enum;

import demo.Geometry.Cube;

/**
 * Liste des blocks qui existent
 */
public enum BlocksType {
    AIR(0),
    GRASS(1),
    COBBLESTONE(2),
    STONE(3),
    DIRT(4),
    MAX(5),
    HOMER(6),
    SAND(7),
    BEDROCK(8),
    NULL(-1);

    private Cube cube; // valeur d'instance, chaque valeur à le sien

    // Constructeur, appelé une seule fois par valeur au chargement JVM
    private final int id; // private car pas possible de créer de nouvelles instancs depuis l'exterieur
    BlocksType(int id) { this.id = id; }

    public int getId() { return id; } // retourne l'id de cette valeur, BlocksType.STONE -> 3

    public static BlocksType fromId(int id) {
        for (BlocksType t : values()) {
            if (t.id == id) return t; // retourne la valeur qui a le même id 3, -> BlocksType.STONE
        }
        return NULL;
    }

    public Cube getCube(){
        if (this.cube == null){
            this.cube = new Cube(this); // au départ créer un cube pour la valeur
        }
        return this.cube; // retourne le même object par valeur, plusieurs appel de GRASS retourne le même object Cube
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
