package demo;

import demo.Geometry.Chunk;
import demo.Geometry.SubChunk;
import demo.Others.FastNoiseLite;
import demo.Enum.BlocksType;

// je lui demande : fait une javadoc et explique bien 
// il abuse un peu la

/**
 * Générateur procédural de terrain pour le monde voxel.
 *
 * <p>Le terrain est généré en trois passes successives :</p>
 * <ol>
 *   <li><b>Grille basse résolution</b> : calcul de la densité sur une grille 5×33×5
 *       espacée de 4 blocs en XZ et 8 blocs en Y.</li>
 *   <li><b>Interpolation trilinéaire</b> : remplissage de chaque bloc à partir des
 *       8 coins de la cellule de grille correspondante. Réduit massivement les appels
 *       au bruit (×512 environ) sans perte visuelle.</li>
 *   <li><b>Couches de surface</b> : remplacement du premier bloc solide rencontré
 *       (depuis le haut) par de la Grass ou du Sand selon le niveau de mer, puis
 *       les 3 blocs suivants par de la Dirt.</li>
 * </ol>
 *
 * <p><b>Architecture des bruits :</b></p>
 * <ul>
 *   <li>{@code minLimitNoise} — variation 3D basse fréquence, plancher de densité.</li>
 *   <li>{@code maxLimitNoise} — variation 3D basse fréquence, plafond de densité.</li>
 *   <li>{@code selectorNoise} — très basse fréquence, sélectionne entre min et max
 *       ET déclenche les zones montagneuses.</li>
 *   <li>{@code depthNoise}    — bruit 2D, décale verticalement la surface (vallons / mers).</li>
 * </ul>
 *
 * <h2>Paramètres clés à régler (voir tableau en bas du fichier)</h2>
 * <pre>
 * ┌─────────────────────────────────────────┬─────────────────┬─────────────────────────────────────────────────────┐
 * │ Paramètre                               │ Valeur actuelle │ Effet si on augmente la valeur                      │
 * ├─────────────────────────────────────────┼─────────────────┼─────────────────────────────────────────────────────┤
 * │ BASE_HEIGHT                             │ 68              │ Terrain plus haut dans le monde                     │
 * │ SEA_LEVEL                               │ 64              │ Mers plus hautes → plus de surface inondée          │
 * │ depth * 3.5  (multiplicateur depth)     │ 3.5             │ Collines plus hautes / creux plus profonds          │
 * │ falloff > 0  (* 0.75)                   │ 0.75            │ Sommet des collines plus pointu / aiguisé           │
 * │ falloff < 0  (* 0.35)                   │ 0.35            │ Base des collines plus large / en pente douce       │
 * │ selectorNoise.frequency  (0.002f)       │ 0.002f          │ Zones biomes plus petites, transitions plus rapides │
 * │ mountainSel seuil  (0.55)               │ 0.55            │ Moins de montagnes (zone 0.55→1.0 réduite)          │
 * │ mountain t2*t2 * 22.0                   │ 22.0            │ Montagnes plus hautes                               │
 * │ depthNoise.frequency  (0.028f)          │ 0.028f          │ Rivières/mers plus étroites et sinueuses            │
 * │ minLimitNoise * 0.4  (amplitude)        │ 0.4             │ Plus de détail 3D dans la densité (grottes etc.)    │
 * └─────────────────────────────────────────┴─────────────────┴─────────────────────────────────────────────────────┘
 * </pre>
 */
public class WorldGenerator {

    // ─────────────────────────────────────────────────────────────────
    //  Générateurs de bruit
    // ─────────────────────────────────────────────────────────────────

    /** Bruit 3D Perlin FBm — plancher de la densité interpolée. */
    private final FastNoiseLite minLimitNoise;

    /** Bruit 3D Perlin FBm — plafond de la densité interpolée. */
    private final FastNoiseLite maxLimitNoise;

    /**
     * Bruit Perlin très basse fréquence — remplit deux rôles :
     * <ul>
     *   <li>Sélecteur 3D : choisit le ratio entre minLimit et maxLimit.</li>
     *   <li>Sélecteur 2D : déclenche les zones montagneuses quand sa valeur > 0.55.</li>
     * </ul>
     */
    private final FastNoiseLite selectorNoise;

    /**
     * Bruit Perlin 2D (seuls X et Z sont utilisés) — décale verticalement la surface
     * pour créer des vallons, des crêtes et des creux aquatiques (rivières / mers peu profondes).
     */
    private final FastNoiseLite depthNoise;

    // ─────────────────────────────────────────────────────────────────
    //  Constantes de mise en forme du terrain
    // ─────────────────────────────────────────────────────────────────

    /**
     * Hauteur de référence (en blocs) autour de laquelle oscille la surface du terrain.
     * Augmenter pousse tout le terrain vers le haut.
     * <ul>
     *   <li>Valeur basse (ex. 50) → terrain dans le bas du monde.</li>
     *   <li>Valeur haute (ex. 100) → terrain dans le haut du monde.</li>
     * </ul>
     */
    private static final int BASE_HEIGHT = 68;

    /**
     * Niveau à partir duquel un bloc de surface est considéré « sous l'eau ».
     * En dessous → bloc SAND (sable/gravier) ; au dessus → bloc GRASS.
     * <ul>
     *   <li>Augmenter → plus de surface couverte par l'eau.</li>
     *   <li>Diminuer → mers moins présentes, terrain plus aride.</li>
     * </ul>
     */
    private static final int SEA_LEVEL = 64;

    // ─────────────────────────────────────────────────────────────────
    //  Dimensions du chunk
    // ─────────────────────────────────────────────────────────────────

    /** Taille horizontale d'un chunk (ex. 16 blocs). Tiré de SubChunk.CHUNKSIZEX. */
    private static final int CHUNK_SIZE   = SubChunk.CHUNKSIZEX;

    /** Hauteur totale du chunk = taille d'un sous-chunk × nombre de sous-chunks (ex. 256). */
    private static final int CHUNK_HEIGHT = SubChunk.CHUNKSIZEY * Chunk.SUBCHUNKCOUNT;

    // ─────────────────────────────────────────────────────────────────
    //  Grille basse résolution pour l'interpolation trilinéaire
    // ─────────────────────────────────────────────────────────────────

    /**
     * Nombre de points de grille en X et Z.
     * {@code CHUNK_SIZE / STEP_XZ + 1} = 16/4 + 1 = 5 points → 4 intervalles couvrent les 16 blocs.
     */
    private static final int GRID_X = 5;

    /**
     * Nombre de points de grille en Y.
     * {@code CHUNK_HEIGHT / STEP_Y + 1} = 256/8 + 1 = 33 points → 32 intervalles couvrent les 256 blocs.
     */
    private static final int GRID_Y = 33;

    /** Nombre de points de grille en Z (identique à GRID_X). */
    private static final int GRID_Z = 5;

    /**
     * Pas de la grille en XZ (en blocs).
     * Plus petit → grille plus dense → génération plus précise mais plus lente.
     */
    private static final int STEP_XZ = 4;

    /**
     * Pas de la grille en Y (en blocs).
     * Plus petit → interpolation verticale plus précise (moins d'artefacts de lissage).
     */
    private static final int STEP_Y  = 8;

    // ─────────────────────────────────────────────────────────────────
    //  Constructeur
    // ─────────────────────────────────────────────────────────────────

    /**
     * Initialise les quatre générateurs de bruit avec des graines décalées pour
     * éviter toute corrélation entre eux.
     *
     * @param seed Graine de base du monde. Chaque bruit reçoit {@code seed + N}
     *             (N = 0, 1, 2, 3) pour garantir l'indépendance des patterns.
     */
    public WorldGenerator(long seed) {

        // ── minLimitNoise ─────────────────────────────────────────────
        this.minLimitNoise = new FastNoiseLite((int)(seed));        // graine de base
        this.minLimitNoise.SetNoiseType(FastNoiseLite.NoiseType.Perlin);
        this.minLimitNoise.SetFrequency(0.01f);                     // fréquence spatiale : détails ~100 blocs
        this.minLimitNoise.SetFractalType(FastNoiseLite.FractalType.FBm); // fractales pour plus de détails
        this.minLimitNoise.SetFractalOctaves(3);                    // 3 octaves = bon compromis qualité/perf

        // ── maxLimitNoise ─────────────────────────────────────────────
        this.maxLimitNoise = new FastNoiseLite((int)(seed + 1));    // +1 → pattern différent de minLimit
        this.maxLimitNoise.SetNoiseType(FastNoiseLite.NoiseType.Perlin);
        this.maxLimitNoise.SetFrequency(0.01f);                     // même fréquence que minLimit
        this.maxLimitNoise.SetFractalType(FastNoiseLite.FractalType.FBm);
        this.maxLimitNoise.SetFractalOctaves(3);

        // ── selectorNoise ─────────────────────────────────────────────
        this.selectorNoise = new FastNoiseLite((int)(seed + 2));    // +2 → encore différent
        this.selectorNoise.SetNoiseType(FastNoiseLite.NoiseType.Perlin);
        this.selectorNoise.SetFrequency(0.002f);  // très basse fréquence → grandes zones homogènes (~500 blocs)
        this.selectorNoise.SetFractalOctaves(2);  // peu d'octaves → transitions douces entre biomes

        // ── depthNoise ────────────────────────────────────────────────
        this.depthNoise = new FastNoiseLite((int)(seed + 3));       // +3 → indépendant des autres
        this.depthNoise.SetNoiseType(FastNoiseLite.NoiseType.Perlin);
        this.depthNoise.SetFrequency(0.028f);     // fréquence modérée → rivières ~35 blocs de large
        this.depthNoise.SetFractalOctaves(3);     // quelques octaves pour des bords naturels
    }

    // ─────────────────────────────────────────────────────────────────
    //  Calcul de densité
    // ─────────────────────────────────────────────────────────────────

    /**
     * Calcule la densité d'un point 3D du monde.
     *
     * <p>La logique est la suivante :</p>
     * <ol>
     *   <li>On mélange {@code minLimitNoise} et {@code maxLimitNoise} via
     *       {@code selectorNoise} pour obtenir une densité de base.</li>
     *   <li>On calcule un décalage vertical ({@code depth}) via {@code depthNoise}
     *       2D, potentiellement amplifié par une zone montagneuse.</li>
     *   <li>On soustrait un terme de chute verticale ({@code verticalFalloff})
     *       qui force le terrain à avoir une surface (densité positive → solide,
     *       densité négative → air).</li>
     * </ol>
     *
     * @param worldX Coordonnée X absolue dans le monde.
     * @param worldY Coordonnée Y absolue dans le monde (altitude).
     * @param worldZ Coordonnée Z absolue dans le monde.
     * @return Valeur de densité : {@code > 0} → bloc solide, {@code ≤ 0} → air.
     */
    private double computeDensity(int worldX, int worldY, int worldZ) {

        // ── Densité de base (blend min/max) ───────────────────────────

        // Valeur du bruit plancher, réduite à ±0.4 pour limiter l'impact 3D
        double minVal = this.minLimitNoise.GetNoise(worldX, worldY, worldZ) * 0.4;

        // Valeur du bruit plafond, même amplitude
        double maxVal = this.maxLimitNoise.GetNoise(worldX, worldY, worldZ) * 0.4;

        // Valeur du sélecteur entre -1 et +1
        double sel = this.selectorNoise.GetNoise(worldX, worldY, worldZ);

        // Normalise le sélecteur entre 0.0 et 1.0 pour l'utiliser comme facteur de mix
        double t = (sel + 1.0) / 2.0;

        // Interpolation linéaire entre minVal et maxVal selon t
        // t=0 → densité = minVal  |  t=1 → densité = maxVal
        double density = minVal + t * (maxVal - minVal);

        // ── Décalage vertical par depthNoise (surface / creux) ────────

        // Bruit 2D pur (Y ignoré) → seule la position horizontale influence la hauteur
        double raw = depthNoise.GetNoise(worldX, worldZ); // retourne [-1 ; +1]

        // Conserve le signe mais peut appliquer une courbe (ici exposant = 1 = linéaire)
        double sign   = raw >= 0 ? 1.0 : -1.0;
        double curved = sign * Math.pow(Math.abs(raw), 1); // sans effet actuellement (pow 1)

        // Amplitude du décalage : ±3.5 blocs → contrôle la variation vallon/creux
        // Augmenter ce chiffre → collines et creux plus prononcés
        double depth = curved * 3.5;

        // ── Zone montagneuse (bonus de hauteur conditionnel) ───────────

        // Réutilise le selectorNoise en 2D (sans Y) pour identifier les zones de montagnes
        // Basse fréquence → les zones montagneuses s'étendent sur ~500 blocs
        double mountainSel = this.selectorNoise.GetNoise(worldX, worldZ);

        if (mountainSel > 0.55) {
            // Seuil : au-delà de 0.55, on entre dans une zone montagneuse
            // La plage 0.55→1.0 représente ~22% du terrain → "de temps en temps"
            // Diminuer le seuil (ex. 0.4) → plus de montagnes
            // Augmenter le seuil (ex. 0.7) → montagnes très rares

            // t2 : progression de 0 à 1 dans la zone montagneuse (0 au bord, 1 au centre)
            double t2 = (mountainSel - 0.55) / 0.45;

            // Courbe en carré (t2²) → transition douce aux bords de la montagne
            // Multiplier 22.0 → montagnes plus hautes (ex. 40.0 = pics imposants)
            depth += t2 * t2 * 22.0;
        }

        // ── Chute verticale (force la présence d'une surface) ─────────

        // Décalage par rapport à la hauteur de référence (positif = au-dessus, négatif = dessous)
        double heightOffset = (worldY - BASE_HEIGHT);

        double verticalFalloff;
        if (heightOffset > 0) {
            // Au-dessus de BASE_HEIGHT : chute LINÉAIRE (× 0.75)
            // Plus le coefficient est grand → sommet des collines plus pointu
            // density = depth - heightOffset * 0.75 → devient 0 quand worldY = BASE_HEIGHT + depth/0.75
            verticalFalloff = heightOffset * 0.75;
        } else {
            // En dessous de BASE_HEIGHT : chute plus douce (× 0.35)
            // Permet à la densité de rester positive plus longtemps → roche bien remplie en profondeur
            verticalFalloff = heightOffset * 0.35;
        }

        // Accumule : densité 3D de base + décalage de surface - chute verticale
        // Quand density > 0 → bloc solide | Quand density ≤ 0 → air
        density += depth - verticalFalloff;

        return density;
    }

    // ─────────────────────────────────────────────────────────────────
    //  Génération du chunk
    // ─────────────────────────────────────────────────────────────────

    /**
     * Génère l'intégralité d'un chunk (blocs + couches de surface).
     *
     * <p>Processus en 3 passes :</p>
     * <ol>
     *   <li>Calcul de la densité sur la grille basse résolution 5×33×5.</li>
     *   <li>Interpolation trilinéaire pour remplir chaque bloc.</li>
     *   <li>Application des couches de surface (herbe, terre, sable).</li>
     * </ol>
     *
     * @param chunk  Instance du chunk à remplir (ses sous-chunks sont modifiés en place).
     * @param chunkX Index X du chunk dans le monde (en unités de chunk, pas en blocs).
     * @param chunkZ Index Z du chunk dans le monde.
     * @return Tableau de blocs bruts (non utilisé directement, les sous-chunks sont mis à jour).
     */
    public byte[] generateChunk(Chunk chunk, int chunkX, int chunkZ) {

        // Tableau de sortie (non utilisé par les sous-chunks mais retourné par la méthode)
        byte[] blocks = new byte[WorldGenerator.CHUNK_SIZE * WorldGenerator.CHUNK_HEIGHT * WorldGenerator.CHUNK_SIZE];

        // ── PASSE 1 : grille basse résolution ─────────────────────────

        // Tableau 3D de densités pré-calculées aux nœuds de la grille
        double[][][] grid = new double[WorldGenerator.GRID_X][WorldGenerator.GRID_Y][WorldGenerator.GRID_Z];

        for (int ix = 0; ix < WorldGenerator.GRID_X; ix++) {
            for (int iy = 0; iy < WorldGenerator.GRID_Y; iy++) {
                for (int iz = 0; iz < WorldGenerator.GRID_Z; iz++) {

                    // Convertit les indices de grille en coordonnées monde absolues
                    int worldX = chunkX * 16 + ix * WorldGenerator.STEP_XZ;
                    int worldY =              iy * WorldGenerator.STEP_Y;   // Y commence à 0
                    int worldZ = chunkZ * 16 + iz * WorldGenerator.STEP_XZ;

                    // Stocke la densité au nœud courant
                    grid[ix][iy][iz] = computeDensity(worldX, worldY, worldZ);
                }
            }
        }

        // ── PASSE 2 : interpolation trilinéaire bloc par bloc ─────────

        for (int bx = 0; bx < WorldGenerator.CHUNK_SIZE; bx++) {
            for (int bz = 0; bz < WorldGenerator.CHUNK_SIZE; bz++) {
                for (int by = 0; by < WorldGenerator.CHUNK_HEIGHT; by++) {

                    // Identifie la cellule de grille contenant ce bloc (coin inférieur-gauche)
                    int cx = bx / WorldGenerator.STEP_XZ; // indice cellule en X
                    int cy = by / WorldGenerator.STEP_Y;  // indice cellule en Y
                    int cz = bz / WorldGenerator.STEP_XZ; // indice cellule en Z

                    // Position normalisée [0.0 ; 1.0[ du bloc à l'intérieur de la cellule
                    double tx = (double)(bx % WorldGenerator.STEP_XZ) / WorldGenerator.STEP_XZ;
                    double ty = (double)(by % WorldGenerator.STEP_Y)  / WorldGenerator.STEP_Y;
                    double tz = (double)(bz % WorldGenerator.STEP_XZ) / WorldGenerator.STEP_XZ;

                    // Les 8 coins de la cellule (notation : c[x][y][z] avec 0=bas/gauche, 1=haut/droite)
                    double c000 = grid[cx  ][cy  ][cz  ]; // coin (0,0,0)
                    double c100 = grid[cx+1][cy  ][cz  ]; // coin (+x,0,0)
                    double c010 = grid[cx  ][cy  ][cz+1]; // coin (0,0,+z)
                    double c110 = grid[cx+1][cy  ][cz+1]; // coin (+x,0,+z)
                    double c001 = grid[cx  ][cy+1][cz  ]; // coin (0,+y,0)
                    double c101 = grid[cx+1][cy+1][cz  ]; // coin (+x,+y,0)
                    double c011 = grid[cx  ][cy+1][cz+1]; // coin (0,+y,+z)
                    double c111 = grid[cx+1][cy+1][cz+1]; // coin (+x,+y,+z)

                    // Interpolation sur l'axe X (produit 4 valeurs sur les arêtes de la face basse/haute)
                    double x00 = c000 + tx * (c100 - c000); // face basse,  z=0
                    double x10 = c010 + tx * (c110 - c010); // face basse,  z=1
                    double x01 = c001 + tx * (c101 - c001); // face haute,  z=0
                    double x11 = c011 + tx * (c111 - c011); // face haute,  z=1

                    // Interpolation sur l'axe Z (produit 2 valeurs sur les arêtes basse/haute)
                    double z0  = x00  + tz * (x10  - x00);  // face basse,  après interp Z
                    double z1  = x01  + tz * (x11  - x01);  // face haute,  après interp Z

                    // Interpolation finale sur Y → valeur de densité du bloc
                    double density = z0 + ty * (z1 - z0);

                    // ── Écriture dans le sous-chunk ───────────────────

                    // Détermine dans quel sous-chunk se trouve ce bloc
                    int subChunkIndex = by / WorldGenerator.CHUNK_SIZE;  // ex. by=20 → subChunk 1
                    int localY        = by % WorldGenerator.CHUNK_SIZE;  // coordonnée Y locale dans le sous-chunk

                    SubChunk subChunk = chunk.getSubChunk(subChunkIndex);

                    // Calcul de l'index linéaire dans le tableau du sous-chunk
                    // Layout : X + localY * SIZE + Z * CHUNK_HEIGHT
                    int idx = bx + localY * WorldGenerator.CHUNK_SIZE + bz * WorldGenerator.CHUNK_HEIGHT;

                    // Affectation : positif → pierre, négatif/nul → air
                    subChunk.blocks[idx] = (density > 0)
                        ? (byte) BlocksType.STONE.getId()
                        : (byte) BlocksType.AIR.getId();
                }
            }
        }

        // ── PASSE 3 : couches de surface ──────────────────────────────
        applySurfaceLayers(chunk);

        return blocks;
    }

    // ─────────────────────────────────────────────────────────────────
    //  Couches de surface
    // ─────────────────────────────────────────────────────────────────

    /**
     * Applique les couches de surface sur un chunk déjà rempli de pierre/air.
     *
     * <p>Pour chaque colonne (X, Z) du chunk, le parcours descend depuis le bloc
     * le plus haut (Y=255) vers le bas (Y=0) et remplace :</p>
     * <ul>
     *   <li>Le <b>premier</b> bloc solide rencontré → GRASS (au-dessus du niveau de mer)
     *       ou SAND (sous le niveau de mer).</li>
     *   <li>Les <b>3 blocs</b> solides suivants → DIRT.</li>
     *   <li>Le bloc Y=0 → BEDROCK (bedrock, indestructible).</li>
     * </ul>
     *
     * @param chunk Chunk dont les sous-chunks vont être modifiés.
     */
    private void applySurfaceLayers(Chunk chunk) {

        for (int bx = 0; bx < WorldGenerator.CHUNK_SIZE; bx++) {
            for (int bz = 0; bz < WorldGenerator.CHUNK_SIZE; bz++) {

                // -1 = on n'a pas encore trouvé la surface de cette colonne
                // >0 = nombre de blocs de terre restant à placer en dessous
                int dirtDepth = -1;

                // Parcours de haut en bas pour trouver la surface
                for (int by = 255; by >= 0; by--) {

                    // Calcul du sous-chunk et de la position locale
                    int subChunkIndex = by / WorldGenerator.CHUNK_SIZE;
                    int localY        = by % WorldGenerator.CHUNK_SIZE;
                    SubChunk subChunk = chunk.getSubChunk(subChunkIndex);
                    int idx = bx + localY * WorldGenerator.CHUNK_SIZE + bz * WorldGenerator.CHUNK_HEIGHT;

                    // Y=0 → bedrock : toujours présent, fin de colonne
                    if (by == 0) {
                        subChunk.blocks[idx] = (byte) BlocksType.BEDROCK.getId();
                        continue;
                    }

                    // Bloc air → pas de surface ici, on continue vers le bas
                    if (subChunk.blocks[idx] == (byte) BlocksType.AIR.getId()) {
                        dirtDepth = -1; // reset : si on rencontre de l'air, la couche de terre est interrompue
                        continue;
                    }

                    // Premier bloc solide rencontré → c'est la surface
                    if (dirtDepth == -1) {
                        dirtDepth = 3; // arme le compteur de terre (3 blocs de dirt sous la surface)

                        // Choix de la texture de surface selon le niveau de mer
                        if (by >= WorldGenerator.SEA_LEVEL)
                            subChunk.blocks[idx] = (byte) BlocksType.GRASS.getId(); // surface émergée → herbe
                        else
                            subChunk.blocks[idx] = (byte) BlocksType.SAND.getId(); // surface immergée → sable/gravier

                    } else if (dirtDepth > 0) {
                        // Blocs sous la surface (jusqu'à 3 blocs de profondeur) → terre
                        subChunk.blocks[idx] = (byte) BlocksType.DIRT.getId();
                        dirtDepth--; // décrémente le compteur de terre
                    }
                    // Si dirtDepth == 0 : on a épuisé la couche de terre, le reste reste en pierre
                }
            }
        }
    }
}