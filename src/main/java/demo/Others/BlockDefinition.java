package demo.Others;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.HashMap;
import java.util.Map;

import demo.Managers.TextureManager;

/**
 * Représente la définition d'un bloc chargée depuis un fichier JSON
 */
@JsonIgnoreProperties(ignoreUnknown = true) // Évite les crashs si le JSON a des champs en trop
public class BlockDefinition {
    /**
     * Id du bloc
     */
    public int id;
    /**
     * Nom du bloc
     */
    public String name;    
    /**
     * Opacité du bloc, 0 = bloc plein, 15 = bloc transparent
     */
    public int opacity;
    /**
     * Luminosité du bloc, 0 = éteint, 15 = allumé
     */
    public int luminance;
    /**
     * Niveau de liquide du bloc, 0 = pas de liquide, 14 = niveau max de liquide
     */
    public int liquidLevel;
    /**
     * Indique si le bloc est transparent
     */
    public boolean transparent;
    /**
     * Largeur du bloc, 1-16
     */
    public int width;
    /**
     * Profondeur du bloc, 1-16
     */
    public int depth;
    /**
     * Hauteur du bloc, 1-16
     */
    public int height;
    /**
     * Position en x du bloc, 0-15
     */
    public int x;
    /**
     * Position en y du bloc, 0-15
     */
    public int y;
    /**
     * Position en z du bloc, 0-15
     */
    public int z;
    
    /** Textures du bloc par direction, exemple : "up" -> "grass_bloc_top", "down" -> "dirt", etc. */
    public Map<String, String> textures;

    /** Textures déjà converties en index OpenGL, prêtes à l'emploi (clé = id de direction). */
    private final Map<Integer, Integer> bakedTextures = new HashMap<>();

    /**
     * Convertit les noms de texture en index OpenGL. <br>
     * Appelé une seule fois dans le BlocksManager après le chargement
     */
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

    /**
     * Constructeur vide requis par Jackson
     * @throws Exception jamais levée ici, requis par Jackson
     */
    public BlockDefinition() throws Exception {}

    /**
     * Vérifie la cohérence des valeurs chargées depuis le JSON
     * @throws Exception si une valeur est hors des bornes autorisées
     */
    public void checkInfo() throws Exception {
        if (opacity > 15){
            throw new Exception("Erreur JSON (id=" + this.id + ") : Opacity > 15");
        }
        if (opacity < 0){
            throw new Exception("Erreur JSON (id=" + this.id + ") : Opacity < 0");
        }
        if (luminance > 15){
            throw new Exception("Erreur JSON (id=" + this.id + ") : Luminance > 15");
        }
        if (luminance < 0){
            throw new Exception("Erreur JSON (id=" + this.id + ") : Luminance < 0");
        }
        if (liquidLevel == 15){
            throw new Exception("Erreur JSON (id=" + this.id + ") : Liquid level == 15, valeur réservée");
        }
        if (liquidLevel > 15){
            throw new Exception("Erreur JSON (id=" + this.id + ") : Liquid level > 15");
        }
        if (liquidLevel < 0){
            throw new Exception("Erreur JSON (id=" + this.id + ") : Liquid level < 0");
        }
        if (width > 16){
            throw new Exception("Erreur JSON (id=" + this.id + ") : Width > 16");
        }
        if (width < 0){
            throw new Exception("Erreur JSON (id=" + this.id + ") : Width < 0");
        }
        if (depth > 16){
            throw new Exception("Erreur JSON (id=" + this.id + ") : Depth > 16");
        }
        if (depth < 0){
            throw new Exception("Erreur JSON (id=" + this.id + ") : Depth < 0");
        }
        if (height > 16){
            throw new Exception("Erreur JSON (id=" + this.id + ") : Height > 16");
        }
        if (height < 0){
            throw new Exception("Erreur JSON (id=" + this.id + ") : Height < 0");
        }
        if (x > 15){
            throw new Exception("Erreur JSON (id=" + this.id + ") : X > 15");
        }
        if (x < 0){
            throw new Exception("Erreur JSON (id=" + this.id + ") : X < 0");
        }
        if (y > 15){
            throw new Exception("Erreur JSON (id=" + this.id + ") : Y > 15");
        }
        if (y < 0){
            throw new Exception("Erreur JSON (id=" + this.id + ") : Y < 0");
        }
        if (z > 15){
            throw new Exception("Erreur JSON (id=" + this.id + ") : Z > 15");
        }
        if (z < 0){
            throw new Exception("Erreur JSON (id=" + this.id + ") : Z < 0");
        }
        if (x + width > 16){
            throw new Exception("Erreur JSON (id=" + this.id + ") : X + Width > 16");
        }
        if (y + height > 16){
            throw new Exception("Erreur JSON (id=" + this.id + ") : Y + Height > 16");
        }
        if (z + depth > 16){
            throw new Exception("Erreur JSON (id=" + this.id + ") : Z + Depth > 16");
        }
    }

    /**
     * Retourne l'index de texture déjà préparé pour une direction donnée. <br>
     * Remplace l'ancien getTexture(Directions)
     * @param direction id de la direction (0=north, 1=south, 2=west, 3=east, 4=up, 5=down, 6=all)
     * @return int, index de texture, ou celui de "all" si absent, ou 0 par défaut
     */
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