package demo.Managers;

import java.util.Collection;
import java.util.concurrent.*;

import demo.Annotations.ChunkThread;
import demo.Annotations.OpenGLThread;
import demo.Geometry.Chunk;
import demo.Geometry.SubChunk;
import demo.Others.BlockDefinition;

/**
 * Gère la collection de tous les chunks chargés (création, accès, suppression).
 */
public class ChunksManager {

    /**
     * Niveau de lumière du ciel maximal (non utilisé pour la propagation actuelle, valeur de référence).
     */
    public static final byte SKY_LIGHT = (byte) 10;


    // C'est comme un dictionnaire, clé valeur
    // String remplacé par Long, optimisation
    /**
     * Table de tous les chunks chargés, indexée par une clé encodant leurs coordonnées (x, z).
     */
    private static ConcurrentHashMap <Long, Chunk> chunks = new ConcurrentHashMap<>();

    /** Constructeur implicite vide */
    public ChunksManager(){}

    /**
     * Construit la clé unique du HashMap à partir des coordonnées d'un chunk.
     * @param x coordonnée x du chunk (en unités de chunk)
     * @param z coordonnée z du chunk (en unités de chunk)
     * @return la clé encodée
     */
    // construit la clé pour le HashMap
    private static long chunkKey(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    /**
     * Créer les chunks entre les coordonées
     * @param fromX départ en X
     * @param toX arrivé en X
     * @param fromZ départ en Z
     * @param toZ arrivé en Z
     */
    public static void createChunks(int fromX, int toX, int fromZ, int toZ) {
        for (int x = fromX; x < toX; x++){
            for (int z = fromZ; z < toZ; z++){
                createChunk(x, z);
            }
        }
    }

    /**
     * Créer un nouveau chunk à la position donné
     * @param x int
     * @param z int
     * @return Chunk
     * @throws RuntimeException si un chunk existe déjà à cette position
     */
    @ChunkThread
    public static Chunk createChunk(int x, int z){
        long coord = chunkKey(x, z);
        if (ChunksManager.chunks.containsKey(coord)) 
            {throw new RuntimeException("Le chunk x: " + x + " z: "+ z + " existe déjà");}
        Chunk chunk = new Chunk(x, z);
        ChunksManager.chunks.put(coord, chunk);
        return chunk;
    }

    /**
     * Retourne le chunk à la position donné (x, z)
     * @param x Coordonées en x (global)
     * @param z Coordonées en z (global)
     * @return Le chunk retourné
     */
    @ChunkThread
    public static Chunk getChunkAt(int x, int z){
        int localX = ChunksManager.floorDiv(x, SubChunk.CHUNKSIZEXZ);
        int localZ = ChunksManager.floorDiv(z, SubChunk.CHUNKSIZEXZ);

        long coord = chunkKey(localX, localZ);
        if (!ChunksManager.chunks.containsKey(coord)) return null;
        return ChunksManager.chunks.get(coord);
    }

    /**
     * Retourne la collection de tous les chunks
     * @return Collection
     */
    public static Collection<Chunk> getAllChunks(){
        return ChunksManager.chunks.values();
    }

    /**
     * Supprime le chunk à la position donné (x, z)
     * @param x Coordonées en x (global)
     * @param z Coordonées en x (global)
     */
    @OpenGLThread
    public static void removeChunk(int x, int z){
        int localX = ChunksManager.floorDiv(x, SubChunk.CHUNKSIZEXZ);
        int localZ = ChunksManager.floorDiv(z, SubChunk.CHUNKSIZEXZ);

        long coord = chunkKey(localX, localZ);
        if (!ChunksManager.chunks.containsKey(coord)) return; // silencieux au lieu de throw

        // Récupère le chunk
        Chunk chunk = ChunksManager.chunks.get(coord);
        // Supprime les VBO VAO de chaque chunk, le garbage collector fait le reste tout seul
        for(SubChunk sub : chunk.subChunks) sub.removeSubChunk();
        // Et supprime le chunk du dico
        ChunksManager.chunks.remove(coord);
    }

    /**
     * Supprime tout les subchunks
     */
    @OpenGLThread
    public static void removeAllSubChunk(){
        for (Chunk chunk : ChunksManager.chunks.values()){
            for (SubChunk sub : chunk.subChunks){
                sub.removeSubChunk();
            }
        }
    }

    /**
     * Supprime tous les subchunks (VBO/VAO) ET vide la table des chunks chargés.
     * A appeler à l'arrêt d'une partie pour repartir sur un état propre.
     */
    @OpenGLThread
    public static void clearAllChunks(){
        ChunksManager.removeAllSubChunk(); // libère les ressources OpenGL
        ChunksManager.chunks.clear();      // vide la table
    }

    /**
     * Retourne un bloc dans l'un des chunks
     * Coordonnée global
     * @param x int
     * @param y int
     * @param z int
     * @return Object3D
     */
    public static BlockDefinition getBlock(int x, int y, int z){
        if (y < 0 || y >= SubChunk.CHUNKSIZEHEIGHT){throw new RuntimeException("Valeur en Y incorrect");}

        Chunk chunk = ChunksManager.getChunkAt(x, z);

        int localX = ChunksManager.floorMod(x, SubChunk.CHUNKSIZEXZ);
        int localZ = ChunksManager.floorMod(z, SubChunk.CHUNKSIZEXZ);

        return chunk.getBlock(localX, y, localZ);
    }

    /**
     * Détermine si une boîte de collision (AABB) donnée en paramètre chevauche un bloc.
     * @param block bloc à tester
     * @param minLx borne minimale en x de la boîte à tester
     * @param maxLx borne maximale en x de la boîte à tester
     * @param minLy borne minimale en y de la boîte à tester
     * @param maxLy borne maximale en y de la boîte à tester
     * @param minLz borne minimale en z de la boîte à tester
     * @param maxLz borne maximale en z de la boîte à tester
     * @return true si la boîte chevauche le bloc
     */
    // prend un RANGE, pas un point
    public static boolean isCollidingBlockAt(BlockDefinition block, float minLx, float maxLx, float minLy, float maxLy, float minLz, float maxLz){
        return (maxLx >= block.x && minLx <= block.x + block.width) &&
               (maxLy >= block.y && minLy <= block.y + block.height) &&
               (maxLz >= block.z && minLz <= block.z + block.depth);
    }

    /**
     * Calcule le modulo avec toujours un résultat positif 
     * @param x valeur à moduler
     * @param mod modulo
     * @return int, toujours compris entre 0 (inclus) et mod (exclus)
     */
    public static int floorMod(int x, int mod) {
        return ((x % mod) + mod) % mod;
    }

    /**
     * Division entière qui fait toujours FLOOR (même pour les négatifs)
     * Exemple : floorDiv(-5, 16) = -1
     * @param a dividende
     * @param b diviseur
     * @return le quotient arrondi vers le bas (floor)
     */
    public static int floorDiv(int a, int b) {
        int q = a / b;
        int r = a % b;
        // si on a un reste et que a et b ont des signes différents
        // on doit descendre d'un cran pour faire un vrai floor
        // ^ est un XOR
        if (r != 0 && ((a < 0) ^ (b < 0))) {
            q--;
        }
        return q;
    }
}