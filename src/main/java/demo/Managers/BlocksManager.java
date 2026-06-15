package demo.Managers;

import com.fasterxml.jackson.databind.ObjectMapper;

import demo.Others.BlockDefinition;
import demo.Others.Blocks;
import demo.Rendering.Render;
import java.io.InputStream;

public class BlocksManager {
    private static final BlockDefinition[] REGISTRY = new BlockDefinition[256];
    private static final ObjectMapper mapper = new ObjectMapper();

    public static void init(String[] files) {
        for (String blockName : files) {
            try {
                // On récupère l'InputStream comme dans ton FileLoader
                InputStream is = Render.class.getResourceAsStream("/Blocks/" + blockName + ".json");
                
                if (is != null) {
                    BlockDefinition block = mapper.readValue(is, BlockDefinition.class);
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
        Blocks.GLOWSTONE   = get(5); 
        Blocks.BEDROCK     = get(6);
        Blocks.COBBLESTONE = get(7);  
        Blocks.GLASS       = get(8);     
        
        for (BlockDefinition block : REGISTRY) {
            if (block != null) {
                block.bake();
            }
        }
    }

    /**
     * Retourne le BlockDefinition avec son id.
     * @param id Identifiant du block
     * @return
     */
    public static BlockDefinition get(int id) {
        return REGISTRY[id & 0xFF] != null ? REGISTRY[id & 0xFF] : REGISTRY[0];
    }
}