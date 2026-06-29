package demo.Managers;

import java.util.Set;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import demo.Geometry.WorldGenerator;
import demo.Geometry.SubChunk;
import demo.Geometry.Chunk;
import demo.Rendering.Render;
import demo.Others.ChunkGenerationPool;
import demo.Others.ChunkSerializer;

public class WorldManager {

    private static final int MAX_CHUNKS_PER_TICK = 3;

    public static final int RENDER_DISTANCE = 10;
    public static final int UNLOAD_DISTANCE = RENDER_DISTANCE + 2;

    public static final int ZONE_NEAR = RENDER_DISTANCE / 4; // 25% de la render distance
    public static final int ZONE_MID  = RENDER_DISTANCE / 2; // 50% de la render distance
    public static final int ZONE_FAR  = RENDER_DISTANCE;     // 100%

    // position du joueur en coordonnées chunk (pas monde) pas (0,0) car sinon la ya rien car le joueur spawn en 0,0
    private static int lastPlayerChunkX = Integer.MIN_VALUE;
    private static int lastPlayerChunkZ = Integer.MIN_VALUE;

    // chunks en cours de génération (évite les doublons)
    private static final Set<Long> generatingChunks = ConcurrentHashMap.newKeySet();
    private static final Set<Long> unloadingChunks = ConcurrentHashMap.newKeySet();

    private static ThreadLocal<WorldGenerator> generator = ThreadLocal.withInitial(
        () -> new WorldGenerator(4949849L)
    );

    /**
     * Appelé chaque tick depuis GameLoop.run()
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
            ChunkGenerationPool.submit(() -> {
                subToMesh.fillSubChunkBitSet();
                synchronized (subToMesh) {
                    subToMesh.mesh.setupSubChunkBuffers(subToMesh.solidBitSet.cardinality());
                    subToMesh.transparentMesh.setupSubChunkBuffers(subToMesh.transparentBitSet.cardinality());
                    subToMesh.generateSubChunkMesh();
                }
                // si il y a au moins une vertice dans le subchunk
                if (subToMesh.mesh.getVertexCount() > 0 || subToMesh.transparentMesh.getVertexCount() > 0) {
                    try { Render.dirtySubChunks.put(subToMesh); } catch (InterruptedException e) {}
                }
                // si tout les subchunks sont chargés alors on ajoute le chunk à la scène
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

        int submitted = 0;
        for (int[] coord : toLoad) {
            if (submitted >= MAX_CHUNKS_PER_TICK) break; // limite

            int cx = coord[0], cz = coord[1];
            long key = chunkKey(cx, cz);
            generatingChunks.add(key);

            int zone = WorldManager.getZone(cx, cz);

            // on créer les chunks mais pas le mesh, seulement la light map, connexion aux voisins
            ChunkGenerationPool.submit(() -> {
                try {
                    Chunk chunk = ChunksManager.createChunk(cx, cz);
                    byte[] saved = ChunkSerializer.load(cx, cz);
                    if (saved != null) {
                        // chunk déjà connun, on restaure les blocs depuis le fichier
                        chunk.setAllSubChunkBlocks(saved);
                    } else {
                        // chunk nouveau, génération
                        generator.get().generateChunk(chunk, cx, cz);
                    }
                    finalizeNewChunk(chunk);
                } finally {
                    generatingChunks.remove(key);
                }
            });
            submitted++;
        }
    }

    /**
     * Marque les chunks trop loin pour suppression (sur OpenGL thread via queue)
     */
    private static void scheduleChunkUnloads(int centerX, int centerZ) {
        for (Chunk chunk : ChunksManager.getAllChunks()) {
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
     */
    public static int getZone(int chunkX, int chunkZ) {
        int dx = chunkX - lastPlayerChunkX;
        int dz = chunkZ - lastPlayerChunkZ;
        int dist2 = dx * dx + dz * dz;
        if (dist2 <= ZONE_NEAR * ZONE_NEAR) return 1;
        if (dist2 <= ZONE_MID  * ZONE_MID)  return 2;
        return 4;
    }

    private static long chunkKey(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    private static void finalizeNewChunk(Chunk chunk) {
        for (SubChunk sub : chunk.subChunks) sub.linkNeighbors();
        relinkNeighborChunks(chunk);
        chunk.initHeightMap();
        LightManager.processLightBFS();

        for (SubChunk sub : chunk.subChunks) sub.fillSubChunkBitSet();      

        // stocker pending dans le chunk pour y accéder depuis le budget loop
        chunk.pendingMeshCount = new AtomicInteger(chunk.subChunks.length);

        // au lieu de mesher maintenant, on met en queue        
        for (SubChunk sub : chunk.subChunks) {
            Render.subChunkMeshQueue.add(sub);
        }
    }

    private static void relinkNeighborChunks(Chunk newChunk) {
        int[][] directions = {{16, 0}, {-16, 0}, {0, 16}, {0, -16}};
        for (int[] d : directions) {
            Chunk neighbor = ChunksManager.getChunkAt(newChunk.x + d[0], newChunk.z + d[1]);
            if (neighbor == null) continue;
            for (SubChunk sub : neighbor.subChunks) {
                sub.linkNeighbors();
                sub.dirty.set(true);
                LightManager.addDirtySubChunk(sub);
            }
        }
    }
}
