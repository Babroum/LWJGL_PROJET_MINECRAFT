package demo;

import demo.Geometry.Chunk;
import demo.Geometry.SubChunk;
import demo.Others.FastNoiseLite;

/**
 * Génère procéduralement le terrain du monde voxel.
 *
 * Fonctionne en 3 passes :
 * 1. Calcul de densité sur une grille basse résolution 5x33x5
 * 2. Interpolation trilinéaire pour remplir chaque bloc
 * 3. Application des couches de surface (herbe, terre, sable, bedrock)
 *
 * Paramètres clés :
 *
 * | Paramètre                          | Valeur  | Effet si augmenté                           |
 * |------------------------------------|---------|---------------------------------------------|
 * | BASE_HEIGHT                        | 68      | Terrain plus haut dans le monde             |
 * | SEA_LEVEL                          | 64      | Plus de surface inondée                     |
 * | depth * 3.5                        | 3.5     | Collines plus hautes / creux plus profonds  |
 * | falloff > 0 (* 0.75)               | 0.75    | Sommets plus pointus                        |
 * | falloff < 0 (* 0.35)               | 0.35    | Base des collines plus large                |
 * | selectorNoise.frequency (0.002f)   | 0.002f  | Zones de biomes plus petites                |
 * | mountainSel seuil (0.55)           | 0.55    | Montagnes plus rares                        |
 * | mountain t2*t2 * 22.0              | 22.0    | Montagnes plus hautes                       |
 * | depthNoise.frequency (0.028f)      | 0.028f  | Rivières plus étroites et sinueuses         |
 * | caveNoise seuil abs() < 0.08       | 0.08    | Grottes plus larges                         |
 */
public class WorldGenerator {

    //  Générateurs de bruit

    /** Bruit 3D Perlin FBm — plancher de la densité interpolée. */
    private final FastNoiseLite minLimitNoise;

    /** Bruit 3D Perlin FBm — plafond de la densité interpolée. */
    private final FastNoiseLite maxLimitNoise;

    /** Bruit 3D Perlin FBm — premier bruit de grotte. */
    private final FastNoiseLite caveNoise;

    /** Bruit 3D Perlin FBm — second bruit de grotte. L'intersection des deux crée des tunnels horizontaux. */
    private final FastNoiseLite caveNoise2;

    /**
     * Bruit Perlin très basse fréquence.
     * Sert de sélecteur entre minLimit et maxLimit (3D),
     * et de déclencheur pour les zones montagneuses (2D).
     */
    private final FastNoiseLite selectorNoise;

    /**
     * Bruit Perlin 2D (Y ignoré).
     * Décale verticalement la surface pour créer des vallons et des creux aquatiques.
     */
    private final FastNoiseLite depthNoise;

    /** Hauteur de référence en blocs autour de laquelle oscille la surface. */
    private static final int BASE_HEIGHT = 68;

    /** Niveau de la mer. En dessous -> sable, au dessus -> herbe. */
    private static final int SEA_LEVEL = 64;

    /** Taille horizontale d'un chunk en blocs. */
    private static final int CHUNK_SIZE   = SubChunk.CHUNKSIZEX;

    /** Hauteur totale d'un chunk en blocs (sous-chunks × hauteur par sous-chunk). */
    private static final int CHUNK_HEIGHT = SubChunk.CHUNKSIZEY * Chunk.SUBCHUNKCOUNT;

    //  Grille basse résolution

    /** Points de grille en X : 16/4 + 1 = 5. */
    private static final int GRID_X = 5;

    /** Points de grille en Y : 256/8 + 1 = 33. */
    private static final int GRID_Y = 33;

    /** Points de grille en Z : identique à GRID_X. */
    private static final int GRID_Z = 5;

    /** Pas de la grille en XZ en blocs. Plus petit = plus précis mais plus lent. */
    private static final int STEP_XZ = 4;

    /** Pas de la grille en Y en blocs. Plus petit = moins d'artefacts d'interpolation verticale. */
    private static final int STEP_Y  = 8;

    /**
     * Initialise tous les générateurs de bruit avec des graines décalées
     * pour éviter toute corrélation entre eux.
     *
     * @param seed graine de base du monde
     */
    public WorldGenerator(long seed) {

        // minLimitNoise
        this.minLimitNoise = new FastNoiseLite((int)(seed)); // graine de base
        this.minLimitNoise.SetNoiseType(FastNoiseLite.NoiseType.Perlin);
        this.minLimitNoise.SetFrequency(0.01f); // fréquence spatiale : détails ~100 blocs
        this.minLimitNoise.SetFractalType(FastNoiseLite.FractalType.FBm); // fractales pour plus de détails
        this.minLimitNoise.SetFractalOctaves(3); // 3 octaves = bon compromis qualité/perf

        // maxLimitNoise
        this.maxLimitNoise = new FastNoiseLite((int)(seed + 1)); // +1 -> pattern différent de minLimit
        this.maxLimitNoise.SetNoiseType(FastNoiseLite.NoiseType.Perlin);
        this.maxLimitNoise.SetFrequency(0.01f); // même fréquence que minLimit
        this.maxLimitNoise.SetFractalType(FastNoiseLite.FractalType.FBm);
        this.maxLimitNoise.SetFractalOctaves(3);

        // selectorNoise
        this.selectorNoise = new FastNoiseLite((int)(seed + 2)); // +2 -> encore différent
        this.selectorNoise.SetNoiseType(FastNoiseLite.NoiseType.Perlin);
        this.selectorNoise.SetFrequency(0.002f); // très basse fréquence -> grandes zones homogènes (~500 blocs)
        this.selectorNoise.SetFractalOctaves(2); // peu d'octaves -> transitions douces entre biomes

        // depthNoise
        this.depthNoise = new FastNoiseLite((int)(seed + 3)); // +3 -> indépendant des autres
        this.depthNoise.SetNoiseType(FastNoiseLite.NoiseType.Perlin);
        this.depthNoise.SetFrequency(0.028f); // fréquence modérée -> rivières ~35 blocs de large
        this.depthNoise.SetFractalOctaves(3); // quelques octaves pour des bords naturels

        // caveNoise
        this.caveNoise = new FastNoiseLite((int)(seed + 4));
        this.caveNoise.SetNoiseType(FastNoiseLite.NoiseType.Perlin);
        this.caveNoise.SetFrequency(0.02f);       // baisser -> tunnels plus longs et espacés
        this.caveNoise.SetFractalType(FastNoiseLite.FractalType.FBm);
        this.caveNoise.SetFractalOctaves(3);

        // caveNoise2
        this.caveNoise2 = new FastNoiseLite((int)(seed + 5)); // graine différente = pattern différent
        this.caveNoise2.SetNoiseType(FastNoiseLite.NoiseType.Perlin);
        this.caveNoise2.SetFrequency(0.02f);
        this.caveNoise2.SetFractalType(FastNoiseLite.FractalType.FBm);
        this.caveNoise2.SetFractalOctaves(3);
    }

    //  Calcul de densité

    /**
     * Calcule la densité d'un point 3D du monde.
     * Densité > 0 = bloc solide, <= 0 = air.
     *
     * @param worldX coordonnée X absolue
     * @param worldY coordonnée Y absolue (altitude)
     * @param worldZ coordonnée Z absolue
     * @return valeur de densité
     */
    private float computeDensity(int worldX, int worldY, int worldZ) {

        // Densité de base (blend min/max)

        // Valeur du bruit plancher, réduite à ±0.4 pour limiter l'impact 3D
        float minVal = this.minLimitNoise.GetNoise(worldX, worldY, worldZ) * 0.4f;

        // Valeur du bruit plafond, même amplitude
        float maxVal = this.maxLimitNoise.GetNoise(worldX, worldY, worldZ) * 0.4f;

        // Valeur du sélecteur entre -1 et +1
        float sel = this.selectorNoise.GetNoise(worldX, worldY, worldZ);

        // Normalise le sélecteur entre 0.0 et 1.0 pour l'utiliser comme facteur de mix
        float t = (float)(sel + 1.0) / 2.0f;

        // Interpolation linéaire entre minVal et maxVal selon t
        // t=0 -> densité = minVal  |  t=1 -> densité = maxVal
        float density = minVal + t * (maxVal - minVal);

        // Décalage vertical par depthNoise (surface / creux)

        // Bruit 2D pur (Y ignoré) -> seule la position horizontale influence la hauteur
        float raw = depthNoise.GetNoise(worldX, worldZ); // retourne [-1 ; +1]

        // Conserve le signe mais peut appliquer une courbe (ici exposant = 1 = linéaire)
        float sign   = raw >= 0 ? 1.0f : -1.0f;
        float curved = sign * (float)Math.pow(Math.abs(raw), 1); // sans effet actuellement (pow 1)

        // Amplitude du décalage : +/-5 blocs -> contrôle la variation vallon/creux
        // Augmenter ce chiffre -> collines et creux plus prononcés
        float depth = curved * 5f;

        // Zone montagneuse

        // Réutilise le selectorNoise en 2D (sans Y) pour identifier les zones de montagnes
        // Basse fréquence -> les zones montagneuses s'étendent sur ~500 blocs
        float mountainSel = this.selectorNoise.GetNoise(worldX, worldZ);

        if (mountainSel > 0.3) {
            // Seuil : au-delà de 0.55, on entre dans une zone montagneuse (~22% du terrain)
            // Diminuer le seuil (ex. 0.4) -> plus de montagnes
            // Augmenter le seuil (ex. 0.7) -> montagnes très rares

            // t2 : progression de 0 à 1 dans la zone montagneuse (0 au bord, 1 au centre)
            float t2 = (float)(mountainSel - 0.3) / 0.45f;

            // Courbe en carré -> transition douce aux bords de la montagne
            // Multiplier 22.0 -> montagnes plus hautes
            depth += t2 * t2 * 30.0;
        }

        // Chute verticale (force la présence d'une surface)

        // Décalage par rapport à la hauteur de référence (positif = au-dessus, négatif = dessous)
        float heightOffset = (worldY - BASE_HEIGHT);

        float verticalFalloff;
        if (heightOffset > 0) {
            // Au-dessus : chute rapide -> sommet des collines bien défini
            verticalFalloff = heightOffset * 0.75f;
        } else {
            // En dessous : chute douce -> roche bien remplie en profondeur
            verticalFalloff = heightOffset * 0.35f;
        }

        density += depth - verticalFalloff;

        return density;
    }

    //  Génération du chunk

    /**
     * Génère tous les blocs d'un chunk en 3 passes :
     * densité sur grille basse résolution, interpolation trilinéaire, couches de surface.
     *
     * @param chunk  le chunk à remplir (ses sous-chunks sont modifiés en place)
     * @param chunkX index X du chunk en unités de chunk
     * @param chunkZ index Z du chunk en unités de chunk
     */
    public void generateChunk(Chunk chunk, int chunkX, int chunkZ) {
        // PASSE 1 : grille basse résolution       

        // Tableau 3D de densités pré-calculées aux nœuds de la grille
        float[] grid = new float[WorldGenerator.GRID_X * WorldGenerator.GRID_Y * WorldGenerator.GRID_Z];

        float[] caveGrid1 = new float[GRID_X * GRID_Y * GRID_Z];
        float[] caveGrid2 = new float[GRID_X * GRID_Y * GRID_Z];

        for (int ix = 0; ix < WorldGenerator.GRID_X; ix++) {
            for (int iy = 0; iy < WorldGenerator.GRID_Y; iy++) {
                for (int iz = 0; iz < WorldGenerator.GRID_Z; iz++) {

                    // Convertit les indices de grille en coordonnées monde absolues
                    int worldX = chunkX * WorldGenerator.CHUNK_SIZE + ix * WorldGenerator.STEP_XZ;
                    int worldY =                                      iy * WorldGenerator.STEP_Y;   // Y commence à 0
                    int worldZ = chunkZ * WorldGenerator.CHUNK_SIZE + iz * WorldGenerator.STEP_XZ;

                    // Stocke la densité au nœud courant
                    grid[ix * WorldGenerator.GRID_Y * WorldGenerator.GRID_Z + iy * WorldGenerator.GRID_Z + iz] = computeDensity(worldX, worldY, worldZ);
                    caveGrid1[ix * WorldGenerator.GRID_Y * WorldGenerator.GRID_Z + iy * WorldGenerator.GRID_Z + iz] = caveNoise.GetNoise(worldX, worldY, worldZ);
                    caveGrid2[ix * WorldGenerator.GRID_Y * WorldGenerator.GRID_Z + iy * WorldGenerator.GRID_Z + iz] = caveNoise2.GetNoise(worldX, worldY, worldZ);
                }
            }
        }
        // PASSE 2 : interpolation trilinéaire bloc par bloc

        for (int bx = 0; bx < WorldGenerator.CHUNK_SIZE; bx++) {
            for (int bz = 0; bz < WorldGenerator.CHUNK_SIZE; bz++) {
                SubChunk currentSub = chunk.getSubChunkAt(0);
                int currentSubIndex = 0;
                for (int by = 0; by < WorldGenerator.CHUNK_HEIGHT; by++) {

                    // Identifie la cellule de grille contenant ce bloc (coin inférieur-gauche)
                    int cx = bx / WorldGenerator.STEP_XZ; // indice cellule en X
                    int cy = by / WorldGenerator.STEP_Y;  // indice cellule en Y
                    int cz = bz / WorldGenerator.STEP_XZ; // indice cellule en Z

                    // Position normalisée [0.0 ; 1.0[ du bloc à l'intérieur de la cellule
                    float tx = (float)(bx % WorldGenerator.STEP_XZ) / WorldGenerator.STEP_XZ;
                    float ty = (float)(by % WorldGenerator.STEP_Y)  / WorldGenerator.STEP_Y;
                    float tz = (float)(bz % WorldGenerator.STEP_XZ) / WorldGenerator.STEP_XZ;

                    float density     = trilinear(grid,      cx, cy, cz, tx, ty, tz);
                    float cave1Interp = trilinear(caveGrid1, cx, cy, cz, tx, ty, tz);
                    float cave2Interp = trilinear(caveGrid2, cx, cy, cz, tx, ty, tz);

                    if (by < SEA_LEVEL - 5 && Math.abs(cave1Interp) < 0.06f && Math.abs(cave2Interp) < 0.06f){
                        density = -1.0f;
                    }
                    // Écriture dans le sous-chunk                       
                    if (by != currentSubIndex) {
                        currentSubIndex = by;
                        currentSub = chunk.getSubChunkAt(by);
                    }

                    int localY = by % WorldGenerator.CHUNK_SIZE;
                    // Calcul de l'index linéaire dans le tableau du sous-chunk
                    // Layout : X + localY * SIZE + Z * CHUNK_HEIGHT
                    int idx = bx + localY * WorldGenerator.CHUNK_SIZE + bz * WorldGenerator.CHUNK_HEIGHT;
                    // Affectation : positif -> pierre, négatif/nul -> air
                    currentSub.blocks[idx] = (density > 0)
                        ? (byte) Blocks.STONE.id
                        : (byte) Blocks.AIR.id;
                }
            }
        }

        // PASSE 3 : couches de surface
        applySurfaceLayers(chunk);
    }

    private float trilinear(float[] grid, int cx, int cy, int cz, float tx, float ty, float tz){
        // Les 8 coins de la cellule 
        float c000 = grid[ cx    * GRID_Y * GRID_Z +  cy    * GRID_Z +  cz   ]; // coin (0,0,0)
        float c100 = grid[(cx+1) * GRID_Y * GRID_Z +  cy    * GRID_Z +  cz   ]; // coin (+x,0,0)
        float c010 = grid[ cx    * GRID_Y * GRID_Z +  cy    * GRID_Z + (cz+1)]; // coin (0,0,+z)
        float c110 = grid[(cx+1) * GRID_Y * GRID_Z +  cy    * GRID_Z + (cz+1)]; // coin (+x,0,+z)
        float c001 = grid[ cx    * GRID_Y * GRID_Z + (cy+1) * GRID_Z +  cz   ]; // coin (0,+y,0)
        float c101 = grid[(cx+1) * GRID_Y * GRID_Z + (cy+1) * GRID_Z +  cz   ]; // coin (+x,+y,0)
        float c011 = grid[ cx    * GRID_Y * GRID_Z + (cy+1) * GRID_Z + (cz+1)]; // coin (0,+y,+z)
        float c111 = grid[(cx+1) * GRID_Y * GRID_Z + (cy+1) * GRID_Z + (cz+1)]; // coin (+x,+y,+z)

        // Interpolation sur l'axe X (produit 4 valeurs sur les arêtes de la face basse/haute)
        float x00 = c000 + tx * (c100 - c000); // face basse,  z=0
        float x10 = c010 + tx * (c110 - c010); // face basse,  z=1
        float x01 = c001 + tx * (c101 - c001); // face haute,  z=0
        float x11 = c011 + tx * (c111 - c011); // face haute,  z=1

        // Interpolation sur l'axe Z (produit 2 valeurs sur les arêtes basse/haute)
        float z0  = x00  + tz * (x10  - x00); // face basse,  après interp Z
        float z1  = x01  + tz * (x11  - x01); // face haute,  après interp Z
         // Interpolation finale sur Y -> valeur de densité du bloc
        return z0 + ty * (z1 - z0);
    }

    // Couches de surface

    /**
     * Applique les couches de surface sur un chunk déjà rempli.
     * Descend colonne par colonne depuis Y=255 :
     * premier bloc solide -> herbe ou sable selon SEA_LEVEL,
     * 3 blocs suivants -> terre, Y=0 -> bedrock.
     * Les grottes ne sont pas affectées (surfacePlaced bloque le reset).
     *
     * @param chunk le chunk à modifier
     */
    private void applySurfaceLayers(Chunk chunk) {
        

        for (int bx = 0; bx < WorldGenerator.CHUNK_SIZE; bx++) {
            for (int bz = 0; bz < WorldGenerator.CHUNK_SIZE; bz++) {
                boolean surfacePlaced = false; // pour éviter d'avoir du sable dans les grottes

                // -1 = on n'a pas encore trouvé la surface de cette colonne
                // >0 = nombre de blocs de terre restant à placer en dessous
                int dirtDepth = -1;

                int currentSubIndex = Chunk.SUBCHUNKCOUNT * SubChunk.CHUNKSIZEY - SubChunk.CHUNKSIZEY;
                SubChunk currentSub = chunk.getSubChunkAt(currentSubIndex);                
                
                // Parcours de haut en bas pour trouver la surface
                for (int by = WorldGenerator.CHUNK_HEIGHT - 1; by >= 0; by--) {

                    // Calcul du sous-chunk
                    if (by != currentSubIndex) {
                        currentSubIndex = by;
                        currentSub = chunk.getSubChunkAt(by);
                    }
                    int localY        = by % WorldGenerator.CHUNK_SIZE;
                    int idx = bx + localY * WorldGenerator.CHUNK_SIZE + bz * WorldGenerator.CHUNK_HEIGHT;

                    // Y=0 -> bedrock : toujours présent, fin de colonne
                    if (by == 0) {
                        currentSub.blocks[idx] = (byte) Blocks.BEDROCK.id;
                        continue;
                    }

                    // Bloc air avant la surface -> on continue vers le bas
                    if (currentSub.blocks[idx] == (byte) Blocks.AIR.id && !surfacePlaced) {
                        dirtDepth = -1;
                        continue;
                    }

                    // Premier bloc solide rencontré -> c'est la surface
                    if (dirtDepth == -1 && !surfacePlaced) {
                        surfacePlaced = true;
                        dirtDepth = 3; // 3 blocs de dirt sous la surface

                        if (by >= WorldGenerator.SEA_LEVEL)
                            currentSub.blocks[idx] = (byte) Blocks.GRASS.id;
                        else
                            currentSub.blocks[idx] = (byte) Blocks.SAND.id;

                    } else if (dirtDepth > 0) {
                        currentSub.blocks[idx] = (byte) Blocks.DIRT.id;
                        dirtDepth--;
                    }
                    // dirtDepth == 0 -> le reste reste en pierre
                }
            }
        }
    }
}