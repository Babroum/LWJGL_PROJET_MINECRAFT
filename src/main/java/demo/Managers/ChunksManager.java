package demo.Managers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.*;

import org.joml.Math;

import demo.BlockDefinition;
import demo.WorldGenerator;
import demo.Annotations.ChunkThread;
import demo.Annotations.OpenGLThread;
import demo.Geometry.Chunk;
import demo.Geometry.SubChunk;
import demo.Rendering.Render;

public class ChunksManager {

    public static final byte SKY_LIGHT = (byte) 10;

    // HashMap fait pareil mais linked permet de garder l'ordre dans lequel c'est insérer
    // C'est comme un dictionnaire, clé valeur
    // String remplacé par Long, optimisation
    private static LinkedHashMap<Long, Chunk> chunks = new LinkedHashMap<>();

    // -2 pour laisser les threads OpenGL et game logic respirer
    // Un ExecutorService c'est un pool de N threads qui restent en vie et se partagent le travail.
    private static final ExecutorService pool = Executors.newFixedThreadPool(
        Math.max(1, Runtime.getRuntime().availableProcessors() - 2),
        r -> { // explique au pool ce qu'il doit faire quand il créer un thread (ThreadFactory)
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        }
    );

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
     * Génère les chunks (maillages)
     * @param generator un seed
     */
    public static void generateChunks(ThreadLocal<WorldGenerator> generator) {
        // les threads travaille chacun de leur côté mais pour chaque chunk on attend avant de passer à l'étape suivante

        // chaque chunk est généré indépendamment, aucune dépendance inter-chunks
        // Future — c'est un ticket de résultat en attente.
        // le ? permet de dire que la tache ne retourne rien (void), on attend pas un résultat mais on s'en sert juste pour attendre la fin
        
        List<Future<?>> futures = new ArrayList<>();
        for (Chunk chunk : chunks.values()) {
            // submit permet au pool de mettre en file d'attente la tâche puis thread worker la prend dès qu'il est libre
            // passe la commande au resto d'un plat
            futures.add(pool.submit(() -> { // pool.submit() retourne un Future
                int cx = ChunksManager.floorDiv(chunk.x, SubChunk.CHUNKSIZEX);
                int cz = ChunksManager.floorDiv(chunk.z, SubChunk.CHUNKSIZEZ);
                generator.get().generateChunk(chunk, cx, cz); // ThreadLocal → thread-safe
                BenchmarkManager.getInstance().recordChunkGenerated();
            }));
        }
        // Attend que TOUS les chunks soient générés avant de passer à finalizeChunks
        for (Future<?> f : futures) {
            // get() présente le ticket de la commande au comptoire du resto
            // Le plat est prêt -> on le récupères immédiatement
            // Le plat cuit encore -> on attends qu'il soit prêt, bloqué là
            try { f.get(); } catch (Exception e) { Thread.currentThread().interrupt(); }

            // après le get on est sur que c'est finit
        }
    }

    /**
     * Finis la génération en connectant les voisins et calculant les faces à caché entre voisins
     */
    public static void finalizeChunks() {        
        // les 3 passes restent séquentielles entre elles,
        // mais les chunks sont traités en parallèle à l'intérieur de chaque passe
        List<Future<?>> futures = new ArrayList<>();

        // Passe 1 : linkNeighbors — lit/écrit uniquement les refs du chunk courant → parallel safe
        for (Chunk chunk : chunks.values()){
            futures.add(pool.submit(() -> { for (SubChunk sub : chunk.subChunks) sub.linkNeighbors(); }));
        }
            
        for (Future<?> f : futures) try { f.get(); } catch (Exception e) {}
        futures.clear(); 

        for (Chunk chunk : chunks.values()){
            futures.add(pool.submit(() -> chunk.initHeightMap()));
        }

        for (Future<?> f : futures) try { f.get(); } catch (Exception e) {}
        futures.clear(); 

        for (Chunk chunk : chunks.values()){
            futures.add(pool.submit(() -> finalizeChunk(chunk) ));
        }
            
        for (Future<?> f : futures) {
            try { f.get(); } catch (Exception e) {}
        }
    }

    /**
     * Finalise la construction du chunk
     * @param chunk Chunk
     */
    @ChunkThread
    public static void finalizeChunk(Chunk chunk){
        for (SubChunk sub : chunk.subChunks){
            sub.fillSubChunkBitSet();
        }
        // générer le mesh avec un BitSet correct
        for(SubChunk sub : chunk.subChunks){
            sub.mesh.setupSubChunkBuffers(sub.bitSet.cardinality()); // recrée vertexArrayPack avant de générer
            sub.generateSubChunkMesh();
            // met le thread en pause si l'array est plein, le thread se relance automatiquement au moment ou OpenGLThread poll un chunk
            if (sub.mesh.getVertexCount() > 0) {   // n'envoie pas les subchunks vides au GPU
                try { Render.dirtySubChunks.put(sub); } catch(InterruptedException e) { Thread.currentThread().interrupt(); }
            }
        }
        try { Render.chunkQueue.put(chunk); } catch(InterruptedException e) { Thread.currentThread().interrupt(); }
                 
    }

    /**
     * Retourne le chunk à la position donné (x, z)
     * @param x Coordonées en x (global)
     * @param z Coordonées en z (global)
     * @return Le chunk retourné
     */
    @ChunkThread
    public static Chunk getChunkAt(int x, int z){
        int localX = ChunksManager.floorDiv(x, SubChunk.CHUNKSIZEX);
        int localZ = ChunksManager.floorDiv(z, SubChunk.CHUNKSIZEZ);

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
        int localX = ChunksManager.floorDiv(x, SubChunk.CHUNKSIZEX);
        int localZ = ChunksManager.floorDiv(z, SubChunk.CHUNKSIZEZ);

        long coord = chunkKey(localX, localZ);
        if (!ChunksManager.chunks.containsKey(coord)){
            throw new RuntimeException("Le chunk x: " + localX + " z: "+ localZ + " n'existe pas");
        }    
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
     * Retourne un block dans l'un des chunks
     * Coordonnée global
     * @param x int
     * @param y int
     * @param z int
     * @return Object3D
     */
    public static BlockDefinition getBlock(int x, int y, int z){
        if (y < 0 || y >= SubChunk.CHUNKSIZEY * Chunk.SUBCHUNKCOUNT){throw new RuntimeException("Valeur en Y incorrect");}

        Chunk chunk = ChunksManager.getChunkAt(x, z);

        int localX = ChunksManager.floorMod(x, SubChunk.CHUNKSIZEX);
        int localZ = ChunksManager.floorMod(z, SubChunk.CHUNKSIZEZ);

        return chunk.getBlock(localX, y, localZ);
    }

    /**
     * Calcule le modulo avec toujours un résultat positif 
     * @param x
     * @param mod
     * @return int
     */
    public static int floorMod(int x, int mod) {
        return ((x % mod) + mod) % mod;
    }

    /**
     * Division entière qui fait toujours FLOOR (même pour les négatifs)
     * Exemple : floorDiv(-5, 16) = -1
     * @param a
     * @param b
     * @return
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
