package demo.Others;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.HashMap;
import java.util.Map;

import demo.Managers.TextureManager;

@JsonIgnoreProperties(ignoreUnknown = true) // Évite les crashs si le JSON a des champs en trop
public class BlockDefinition {
    public int id;
    public String name;
    
    public int opacity;
    public int light;
    public boolean transparent;
    
    // "up" -> "grass_block_top", "down" -> "dirt", etc.
    public Map<String, String> textures;

    // C'est ici qu'on stocke les ingrédients "prêts"
    private final Map<String, Integer> bakedTextures = new HashMap<>();

    // Appelé UNE SEULE FOIS dans le BlocksManager après le chargement
    public void bake() {
        if (textures == null) return;
        
        // On transforme les noms en index OpenGL
        for (String direction : textures.keySet()) {
            String fileName = textures.get(direction);
            int textureIndex = TextureManager.getIndex(fileName);
            bakedTextures.put(direction, textureIndex);
        }
        
        // Sécurité pour le "all" (le défaut)
        if (textures.containsKey("all")) {
            int allIndex = TextureManager.getIndex(textures.get("all"));
            bakedTextures.put("all", allIndex);
        }
    }

    // Constructeur vide requis par Jackson
    public BlockDefinition() {}

    // Cette méthode remplace l'ancien getTexture(Directions)
    public int getTextureLayer(String direction) {
        // Plus de recherche de nom, plus d'appel au TextureManager
        // On récupère juste le chiffre déjà prêt
        Integer index = bakedTextures.get(direction);
        if (index == null) {
            index = bakedTextures.get("all");
        }
        return (index != null) ? index : 0;
    }
}
