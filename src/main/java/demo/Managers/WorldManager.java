package demo.Managers;

import java.util.Set;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.Random;

import demo.Geometry.WorldGenerator;
import demo.Geometry.SubChunk;
import demo.Game.GameSession;
import demo.Geometry.Chunk;
import demo.Rendering.Render;
import demo.Others.Blocks;
import demo.Others.ChunkGenerationPool;
import demo.Others.ChunkSerializer;

/**
 * Gère le chargement, le déchargement et le cycle de vie des chunks autour du joueur
 */
public class WorldManager {

    /** Nombre maximum de chunks soumis à la génération par tick. */
    private static final int MAX_CHUNKS_PER_TICK = 4;

    /** Distance de rendu, en chunks. */
    public static final int RENDER_DISTANCE = 5;
    /** Distance de déchargement des chunks, en chunks. */
    public static final int UNLOAD_DISTANCE = RENDER_DISTANCE + 2;

    /** Zone proche, 25% de la render distance. */
    public static final int ZONE_NEAR = RENDER_DISTANCE / 4; // 25% de la render distance
    /** Zone intermédiaire, 50% de la render distance. */
    public static final int ZONE_MID  = RENDER_DISTANCE / 2; // 50% de la render distance
    /** Zone lointaine, 100% de la render distance. */
    public static final int ZONE_FAR  = RENDER_DISTANCE;     // 100%

    // position du joueur en coordonnées chunk (pas monde) pas (0,0) car sinon la ya rien car le joueur spawn en 0,0
    /** Dernière position du joueur en x, en coordonnées chunk. */
    private static int lastPlayerChunkX = Integer.MIN_VALUE;
    /** Dernière position du joueur en z, en coordonnées chunk. */
    private static int lastPlayerChunkZ = Integer.MIN_VALUE;

    // chunks en cours de génération (évite les doublons)
    /** Clés des chunks actuellement en cours de génération (évite les doublons). */
    private static final Set<Long> generatingChunks = ConcurrentHashMap.newKeySet();
    /** Clés des chunks actuellement en cours de déchargement (évite les doublons). */
    private static final Set<Long> unloadingChunks = ConcurrentHashMap.newKeySet();

    /** Générateur de terrain, une instance par thread. */
    private static ThreadLocal<WorldGenerator> generator = ThreadLocal.withInitial(
        () -> new WorldGenerator(4649686131987684165L)
    );

    /** Constructeur implicite vide */
    public WorldManager(){}

    /**
     * Remet le WorldManager à son état initial (à appeler à l'arrêt d'une partie).
     */
    public static void reset(){
        lastPlayerChunkX = Integer.MIN_VALUE;
        lastPlayerChunkZ = Integer.MIN_VALUE;
        generatingChunks.clear();
        unloadingChunks.clear();
    }

    /**
     * Appelé chaque tick depuis GameLoop.run()
     * @param playerX float, position du joueur en x
     * @param playerZ float, position du joueur en z
     */
    public static void tick(float playerX, float playerZ) {
        int playerChunkX = ChunksManager.floorDiv((int) playerX, SubChunk.CHUNKSIZEXZ);
        int playerChunkZ = ChunksManager.floorDiv((int) playerZ, SubChunk.CHUNKSIZEXZ);

        boolean playerMoved = playerChunkX != lastPlayerChunkX || playerChunkZ != lastPlayerChunkZ;
        if (playerMoved) {
            // si le joueur à bougé alors on décharge
            lastPlayerChunkX = playerChunkX;
            lastPlayerChunkZ = playerChunkZ;
            scheduleChunkUnloads(playerChunkX, playerChunkZ);
        }

        // ici les mesh des chunks sont créer chaque tick avec un budget
        int budget = 16;
        while (budget-- > 0){
            SubChunk subToMesh = Render.subChunkMeshQueue.poll();
            if (subToMesh == null) break;
            final int epoch = GameSession.currentEpoch();
            ChunkGenerationPool.submit(() -> {
                if (epoch != GameSession.currentEpoch()) return; // partie arrêtée entre-temps
                subToMesh.fillSubChunkBitSet();
                synchronized (subToMesh) {
                    subToMesh.mesh.setupSubChunkBuffers(subToMesh.solidBitSet.cardinality());
                    subToMesh.transparentMesh.setupSubChunkBuffers(subToMesh.transparentBitSet.cardinality());
                    subToMesh.generateSubChunkMesh();
                }
                if (epoch != GameSession.currentEpoch()) return; // re-check après le travail, avant d'écrire

                if (subToMesh.mesh.getVertexCount() > 0 || subToMesh.transparentMesh.getVertexCount() > 0) {
                    try { Render.dirtySubChunks.put(subToMesh); } catch (InterruptedException e) {}
                }
                if (subToMesh.parent.pendingMeshCount.decrementAndGet() == 0) {
                    try { Render.chunkQueue.put(subToMesh.parent); } catch (InterruptedException e) {}
                }
            });
        }

        // toujours appelé, même si le joueur n'a pas bougé
        // car il reste peut-être des chunks à soumettre du tick précédent
        scheduleChunkLoads(playerChunkX, playerChunkZ);
    }

    /**
     * Charge les chunks manquants en spirale (les plus proches en premier)
     * @param centerX int, position centrale en x (coordonnées chunk)
     * @param centerZ int, position centrale en z (coordonnées chunk)
     */
    private static void scheduleChunkLoads(int centerX, int centerZ) {
        List<int[]> toLoad = new ArrayList<>();

        // détermine la zone autour du joueur, et charge en spirale
        for (int dx = -RENDER_DISTANCE; dx <= RENDER_DISTANCE; dx++) {
            for (int dz = -RENDER_DISTANCE; dz <= RENDER_DISTANCE; dz++) {
                if (dx * dx + dz * dz > RENDER_DISTANCE * RENDER_DISTANCE) continue;

                int cx = centerX + dx;
                int cz = centerZ + dz;

                // est ce que le chunk est déjà chargé ou en cours
                if (ChunksManager.getChunkAt(cx * 16, cz * 16) != null) continue;
                long key = chunkKey(cx, cz);
                if (generatingChunks.contains(key)) continue;

                toLoad.add(new int[]{cx, cz, dx * dx + dz * dz}); // distance² pour trier
            }
        }

        // trie par distance croissante : on génère les plus proches en premier
        toLoad.sort(Comparator.comparingInt(a -> a[2]));

        // Phase 1 : générer le terrain en parallèle
        List<Future<?>> futures = new ArrayList<>();
        List<Chunk> generatedChunks = new java.util.concurrent.CopyOnWriteArrayList<>();

        final int epoch = GameSession.currentEpoch();

        int submitted = 0;
        for (int[] coord : toLoad) {
            if (submitted >= MAX_CHUNKS_PER_TICK) break;

            int cx = coord[0], cz = coord[1];
            long key = chunkKey(cx, cz);
            generatingChunks.add(key);

            futures.add(ChunkGenerationPool.submitFuture(() -> {
                try {
                    if (epoch != GameSession.currentEpoch()) return; // partie arrêtée, on n'écrit rien

                    Chunk chunk = ChunksManager.createChunk(cx, cz);
                    byte[] saved = ChunkSerializer.load(cx, cz);
                    if (saved != null) {
                        chunk.setAllSubChunkBlocks(saved);
                    } else {
                        generator.get().generateChunk(chunk, cx, cz);
                    }
                    for (SubChunk sub : chunk.subChunks) sub.linkNeighbors();
                    relinkNeighborChunks(chunk);
                    chunk.initHeightMap();
                    for (SubChunk sub : chunk.subChunks) sub.fillSubChunkBitSet();
                    generatedChunks.add(chunk);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (epoch == GameSession.currentEpoch()) {
                        generatingChunks.remove(key);
                    }
                }
            }));
            submitted++;
        }

        //
        // Un seul thread attend les futures et lance la phase 2
        ChunkGenerationPool.submit(() -> {
            for (Future<?> f : futures) {
                try { f.get(); } catch (Exception e) {}
            }
            
            if (epoch != GameSession.currentEpoch()) return; // même epoch capturé plus haut, réutilisé ici

            // Tous les chunks de la phase 1 sont prêts, on peut placer les arbres
            for (Chunk chunk : generatedChunks) {
                boolean isFromSave = ChunkSerializer.load(
                    ChunksManager.floorDiv(chunk.x, 16),
                    ChunksManager.floorDiv(chunk.z, 16)
                ) != null;

                if (!isFromSave) {
                    Random random = new Random();
                    for (int x = 0; x < SubChunk.CHUNKSIZEXZ; x++) {
                        for (int z = 0; z < SubChunk.CHUNKSIZEXZ; z++) {
                            int y = chunk.heightMap[z + x * SubChunk.CHUNKSIZEXZ];
                            if (chunk.getBlock(x, y, z) == Blocks.GRASS) {
                                if (random.nextInt(50) == 0) {
                                    generator.get().generateTree(x, y + 1, z, chunk);
                                }
                            }
                        }
                    }
                }

                chunk.pendingMeshCount.set(chunk.subChunks.length);
                for (SubChunk sub : chunk.subChunks) {
                    Render.subChunkMeshQueue.add(sub);
                }
                chunk.isReady = true;
            }
        });
    }

    /**
     * Marque les chunks trop loin pour suppression (sur OpenGL thread via queue)
     * @param centerX int, position centrale en x (coordonnées chunk)
     * @param centerZ int, position centrale en z (coordonnées chunk)
     */
    private static void scheduleChunkUnloads(int centerX, int centerZ) {
        for (Chunk chunk : ChunksManager.getAllChunks()) {
            if (!chunk.isReady) continue; 

            int cx = ChunksManager.floorDiv(chunk.x, SubChunk.CHUNKSIZEXZ);
            int cz = ChunksManager.floorDiv(chunk.z, SubChunk.CHUNKSIZEXZ);
            int dx = cx - centerX;
            int dz = cz - centerZ;
            if (dx * dx + dz * dz > UNLOAD_DISTANCE * UNLOAD_DISTANCE) {
                long key = chunkKey(cx, cz);
                if (unloadingChunks.add(key)) { // add retourne false si déjà présent
                    Render.chunksToUnload.offer(chunk);
                }
            }
        }
    }

    /**
     * A appeler lorsqu'un chunk vient d'être déchargé, retire sa clé de {@link #unloadingChunks}
     * @param chunk Chunk déchargé
     */
    // dans WorldManager
    public static void onChunkUnloaded(Chunk chunk) {
        int cx = ChunksManager.floorDiv(chunk.x, SubChunk.CHUNKSIZEXZ);
        int cz = ChunksManager.floorDiv(chunk.z, SubChunk.CHUNKSIZEXZ);
        unloadingChunks.remove(chunkKey(cx, cz));
    }

    /**
     * A MODIFIER VALEUR
     * Retourne la zone LOD d'un chunk (pour usage futur)
     * 1 = NEAR, 2 = MID, 4 = FAR
     * @param chunkX int, position du chunk en x
     * @param chunkZ int, position du chunk en z
     * @return int, 1 = NEAR, 2 = MID, 4 = FAR
     */
    public static int getZone(int chunkX, int chunkZ) {
        int dx = chunkX - lastPlayerChunkX;
        int dz = chunkZ - lastPlayerChunkZ;
        int dist2 = dx * dx + dz * dz;
        if (dist2 <= ZONE_NEAR * ZONE_NEAR) return 1;
        if (dist2 <= ZONE_MID  * ZONE_MID)  return 2;
        return 4;
    }

    /**
     * Encode des coordonnées de chunk en une clé unique
     * @param x int, coordonnée chunk en x
     * @param z int, coordonnée chunk en z
     * @return long, clé unique
     */
    private static long chunkKey(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    /**
     * Relie les subchunks voisins d'un nouveau chunk et les marque comme dirty pour la lumière
     * @param newChunk Chunk nouvellement généré
     */
    private static void relinkNeighborChunks(Chunk newChunk) {
        int[][] directions = {{16, 0}, {-16, 0}, {0, 16}, {0, -16}};
        for (int[] d : directions) {
            Chunk neighbor = ChunksManager.getChunkAt(newChunk.x + d[0], newChunk.z + d[1]);
            if (neighbor == null) continue;
            for (SubChunk sub : neighbor.subChunks) {
                sub.linkNeighbors();
                if (sub.dirty.compareAndSet(false, true)) {
                    LightManager.addDirtySubChunk(sub);
                }
            }
        }
    }
}