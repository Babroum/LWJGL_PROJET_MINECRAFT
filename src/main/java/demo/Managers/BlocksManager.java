package demo.Managers;

import com.fasterxml.jackson.databind.ObjectMapper;

import demo.Others.BlockDefinition;
import demo.Others.Blocks;
import demo.Rendering.Render;
import java.io.InputStream;

/**
 * Charge et référence toutes les définitions de blocs (BlockDefinition) du jeu.
 */
public class BlocksManager {
    /**
     * Registre des définitions de blocs, indexé par leur id (0-255).
     */
    private static final BlockDefinition[] REGISTRY = new BlockDefinition[256];
    /**
     * Mapper JSON utilisé pour désérialiser les fichiers de définition de blocs.
     */
    private static final ObjectMapper mapper = new ObjectMapper();

    /** Constructeur implicite vide */
    public BlocksManager(){}

    /**
     * Charge tous les fichiers JSON de définition de blocs donnés, les enregistre dans le registre,
     * initialise les raccourcis statiques de {@link Blocks} et "bake" chaque bloc.
     * @param files noms des fichiers JSON (sans extension) à charger depuis "/Blocks/"
     * @throws Exception si une erreur survient pendant le chargement
     */
    public static void init(String[] files) throws Exception {
        for (String blockName : files) {
            try {
                // On récupère l'InputStream comme dans ton FileLoader
                InputStream is = Render.class.getResourceAsStream("/Blocks/" + blockName + ".json");
                
                if (is != null) {
                    BlockDefinition block = mapper.readValue(is, BlockDefinition.class);
                    block.checkInfo();
                    REGISTRY[block.id] = block;
                    is.close();
                }
            } catch (Exception e) {
                System.err.println("Erreur chargement bloc: " + blockName);
                e.printStackTrace();
            }
        }
        
        // Sécurité Air
        if (REGISTRY[0] == null) {
            BlockDefinition air = new BlockDefinition();            
            air.id = 0; air.name = "Air"; air.opacity = 1;
            REGISTRY[0] = air;
        }

        // Une fois chargé, on crée les liens
        Blocks.NULL        = get(255);
        Blocks.AIR         = get(0);
        Blocks.GRASS       = get(1); // L'ID défini dans le JSON
        Blocks.STONE       = get(2);
        Blocks.DIRT        = get(3);
        Blocks.SAND        = get(4);
        Blocks.LIGHT_BLOCK   = get(5);
        Blocks.BEDROCK     = get(6);
        Blocks.COBBLESTONE = get(7);
        Blocks.GLASS       = get(8);          
        Blocks.GRAVEL        = get(9);        
        Blocks.STONE_BRICK_WALL = get(10); 
        Blocks.OAK_LOG = get(11);
        Blocks.OAK_PLANKS = get(12);
        Blocks.OAK_LEAVES = get(13);

        Blocks.WATER = get(14);
        Blocks.TORCH = get(15);
        
        for (BlockDefinition block : REGISTRY) {
            if (block != null) {
                block.bake();
            }
        }
    }

    /**
     * Retourne le BlockDefinition avec son id.
     * @param id Identifiant du bloc
     * @return la définition du bloc, ou celle du bloc Air (id 0) si l'id est introuvable
     */
    public static BlockDefinition get(int id) {
        return REGISTRY[id & 0xFF] != null ? REGISTRY[id & 0xFF] : REGISTRY[0];
    }
}