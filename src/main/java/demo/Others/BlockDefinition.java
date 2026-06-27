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
    public int luminance;
    public boolean transparent;
    
    // "up" -> "grass_bloc_top", "down" -> "dirt", etc.
    public Map<String, String> textures;

    // C'est ici qu'on stocke les ingrédients "prêts"
    private final Map<Integer, Integer> bakedTextures = new HashMap<>();

    // Appelé UNE SEULE FOIS dans le blocsManager après le chargement
    public void bake() {
        if (textures == null) return;
        
        // On transforme les noms en index OpenGL
        for (String direction : textures.keySet()) {
            String fileName = textures.get(direction);
            int textureIndex = TextureManager.getIndex(fileName);
            int dirId;
            switch (direction) {
                case "north":
                    dirId = 0; break;
                case "south":
                    dirId = 1; break;
                case "west":
                    dirId = 2; break;
                case "east":
                    dirId = 3; break;
                case "up":
                    dirId = 4; break;
                case "down":
                    dirId = 5; break;       
                case "all":
                    dirId = 6; break;     
                default:
                    dirId = 6; break;
            }
            bakedTextures.put(dirId, textureIndex);
        }
        
        // Sécurité pour le "all" (le défaut)
        if (textures.containsKey("all")) {
            int allIndex = TextureManager.getIndex(textures.get("all"));
            bakedTextures.put(6, allIndex);
        }
    }

    // Constructeur vide requis par Jackson
    public BlockDefinition() {}

    // Cette méthode remplace l'ancien getTexture(Directions)
    public int getTextureLayer(int direction) {
        // Plus de recherche de nom, plus d'appel au TextureManager
        // On récupère juste le chiffre déjà prêt
        Integer index = bakedTextures.get(direction);
        if (index == null) {
            index = bakedTextures.get(6);
        }
        return (index != null) ? index : 0;
    }
}
