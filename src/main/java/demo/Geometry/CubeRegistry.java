package demo.Geometry;

import java.util.EnumMap;
import demo.Enum.BlocksType;

/**
 * Stocke UN SEUL Cube par BlocksType.
 * Au lieu de créer des milliers de Cube identiques,
 * tous les blocs du même type partagent le même objet.
 */
public class CubeRegistry {

    private static final EnumMap<BlocksType, Cube> registry = new EnumMap<>(BlocksType.class);

    // Appelé UNE SEULE FOIS au démarrage
    public static void init() {
        for (BlocksType type : BlocksType.values()) {
            if (type != BlocksType.NULL) {
                registry.put(type, new Cube(type));
            }
        }
    }

    public static Cube get(BlocksType type) {
        return registry.get(type);
    }
}