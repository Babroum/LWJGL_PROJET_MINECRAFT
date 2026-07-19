package demo.Managers;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;

import demo.Annotations.OpenGLThread;
import demo.Geometry.Chunk;
import demo.Geometry.SubChunk;
import demo.Others.Blocks;
import demo.Others.ChunkGenerationPool;

public class LiquidManager {

    private static final Set<Long> liquidQueuedSet        = ConcurrentHashMap.newKeySet();
    private static final Set<Long> removalLiquidQueuedSet = ConcurrentHashMap.newKeySet();

    private static final Queue<SubChunk> dirtySubChunks   = new ConcurrentLinkedQueue<>();

    private static final ConcurrentLinkedDeque<Long> pendingSchedule        = new ConcurrentLinkedDeque<>();
    private static final ConcurrentLinkedDeque<Long> pendingRemovalSchedule = new ConcurrentLinkedDeque<>();

    private static final long OFFSET = 1L << 25;
    private static final long MASK26 = 0x3FFFFFFL;

    private static final int FLOW_DELAY_TICKS   = 10;
    private static final int FLOW_STEP_DELAY    = 5; // 5 ticks/bloc comme Minecraft Java
    private static final int REMOVAL_STEP_DELAY = 5;

    private static long currentTick = 0;

    private static final int[][] HORIZONTAL_NEIGHBORS = {
        {1,0,0},{-1,0,0},{0,0,1},{0,0,-1}
    };

    @SuppressWarnings("unchecked")
    private static final Deque<Long>[] scheduledQueue        = new ArrayDeque[FLOW_DELAY_TICKS];
    @SuppressWarnings("unchecked")
    private static final Deque<Long>[] scheduledRemovalQueue = new ArrayDeque[FLOW_DELAY_TICKS];
    static {
        for (int i = 0; i < FLOW_DELAY_TICKS; i++) {
            scheduledQueue[i]        = new ArrayDeque<>();
            scheduledRemovalQueue[i] = new ArrayDeque<>();
        }
    }

    private static long encode(int gx, int gy, int gz, int liquidLevel) {
        long px = (gx + OFFSET) & MASK26;
        long pz = (gz + OFFSET) & MASK26;
        long py = gy & 0xFFL;
        long pl = liquidLevel & 0xFL;
        return (px << 38) | (pz << 12) | (py << 4) | pl;
    }

    private static void schedule(long encoded) {
        pendingSchedule.addLast(encoded);
    }

    private static void scheduleImmediate(long encoded) {
        int bucket = (int)((currentTick + 1) % FLOW_DELAY_TICKS);
        scheduledQueue[bucket].addLast(encoded);
    }

    private static void scheduleRemoval(long encoded) {
        pendingRemovalSchedule.addLast(encoded);
    }

    public static void addLiquid(Chunk chunk, int x, int y, int z, byte liquidLevel) {
        if (liquidLevel <= 1) return;
        int gx = chunk.x + x;
        int gz = chunk.z + z;
        long encoded = encode(gx, y, gz, liquidLevel);
        if (!liquidQueuedSet.add(encoded)) return;
        schedule(encoded);
    }

    public static void removeLiquid(Chunk chunk, int x, int y, int z, byte liquidLevel) {
        if (liquidLevel <= 1) return;
        int gx = chunk.x + x;
        int gz = chunk.z + z;
        long encoded = encode(gx, y, gz, liquidLevel);
        if (!removalLiquidQueuedSet.add(encoded)) return;
        scheduleRemoval(encoded);
    }

    private static boolean isDrain(int gx, int gy, int gz) {
        if (gy - 1 < 0) return false;
        Chunk c = ChunksManager.getChunkAt(gx, gz);
        if (c == null) return false;
        SubChunk sub = c.getSubChunkAt(gy - 1);
        if (sub == null) return false;
        int lx = ChunksManager.floorMod(gx, SubChunk.CHUNKSIZEXZ);
        int ly = ChunksManager.floorMod(gy - 1, SubChunk.CHUNKSIZEXZ);
        int lz = ChunksManager.floorMod(gz, SubChunk.CHUNKSIZEXZ);
        if (sub.isBlockSolid(lx, ly, lz)) return false;
        return sub.getLiquidLevel(lx, ly, lz) == 0; // doit être de l'air, pas de l'eau
    }

    private static boolean[] findDrainDirections(int gx, int gy, int gz, int maxRange) {
        int[] weights = new int[4];
        Arrays.fill(weights, 1000);
        int minWeight = 1000;

        for (int i = 0; i < 4; i++) {
            int[] d = HORIZONTAL_NEIGHBORS[i];
            int startX = gx + d[0], startZ = gz + d[2];

            // Le voisin direct dans cette direction doit être franchissable
            Chunk startChunk = ChunksManager.getChunkAt(startX, startZ);
            if (startChunk == null) continue;
            SubChunk startSub = startChunk.getSubChunkAt(gy);
            if (startSub == null) continue;
            int slx = ChunksManager.floorMod(startX, SubChunk.CHUNKSIZEXZ);
            int sly = gy % SubChunk.CHUNKSIZEXZ;
            int slz = ChunksManager.floorMod(startZ, SubChunk.CHUNKSIZEXZ);
            if (startSub.isBlockSolid(slx, sly, slz)) continue;

            // Le voisin direct est-il lui-même un drain ? Distance = 1
            if (isDrain(startX, gy, startZ)) {
                weights[i] = 1;
                minWeight = Math.min(minWeight, 1);
                continue; // drain immédiat, inutile de continuer le BFS pour cette direction
            }

            // BFS depuis ce voisin pour trouver un drain
            Queue<int[]> bfsQueue = new ArrayDeque<>();
            Set<Long>    visited  = new HashSet<>();
            long startKey = ((long) startX << 32) | (startZ & 0xFFFFFFFFL);
            visited.add(startKey);
            bfsQueue.add(new int[]{startX, startZ, 1});

            outer:
            while (!bfsQueue.isEmpty()) {
                int[] entry = bfsQueue.poll();
                int cx = entry[0], cz = entry[1], dist = entry[2];
                if (dist >= maxRange) continue;

                for (int[] nd : HORIZONTAL_NEIGHBORS) {
                    int nx = cx + nd[0], nz = cz + nd[2];
                    long key = ((long) nx << 32) | (nz & 0xFFFFFFFFL);
                    if (!visited.add(key)) continue;

                    Chunk chunk = ChunksManager.getChunkAt(nx, nz);
                    if (chunk == null) continue;
                    SubChunk sub = chunk.getSubChunkAt(gy);
                    if (sub == null) continue;
                    int lx = ChunksManager.floorMod(nx, SubChunk.CHUNKSIZEXZ);
                    int ly = gy % SubChunk.CHUNKSIZEXZ;
                    int lz = ChunksManager.floorMod(nz, SubChunk.CHUNKSIZEXZ);
                    if (sub.isBlockSolid(lx, ly, lz)) continue;

                    if (isDrain(nx, gy, nz)) {
                        weights[i] = dist + 1;
                        minWeight = Math.min(minWeight, dist + 1);
                        break outer; // premier drain trouvé = chemin le plus court (BFS)
                    }

                    bfsQueue.add(new int[]{nx, nz, dist + 1});
                }
            }
        }

        boolean[] result = new boolean[4];
        if (minWeight == 1000) {
            // Aucun drain trouvé dans le rayon, toutes les directions autorisées (comportement plat)
            result[0] = result[1] = result[2] = result[3] = true;
        } else {
            // Seulement les directions avec le poids minimum (chemin le plus court vers un drain)
            for (int i = 0; i < 4; i++) {
                result[i] = (weights[i] == minWeight);
            }
        }
        return result;
    }

    private static void bfsPropagate(long packed) {
        int gx = (int) (((packed >>> 38) & MASK26) - OFFSET);
        int gz = (int) (((packed >>> 12) & MASK26) - OFFSET);
        int gy = (int)  ((packed >>>  4) & 0xFFL);
        byte liquidLevel = (byte)(packed & 0xFL);

        Chunk parentChunk = ChunksManager.getChunkAt(gx, gz);
        if (parentChunk == null) return;
        SubChunk parentSub = parentChunk.getSubChunkAt(gy);
        if (parentSub == null) return;

        int cpx = ChunksManager.floorMod(gx, SubChunk.CHUNKSIZEXZ);
        int cpy = ChunksManager.floorMod(gy, SubChunk.CHUNKSIZEXZ);
        int cpz = ChunksManager.floorMod(gz, SubChunk.CHUNKSIZEXZ);

        if (parentSub.isBlockSolid(cpx, cpy, cpz)) return;
        int currentLevel = parentSub.getLiquidLevel(cpx, cpy, cpz);
        if (currentLevel <= 0 || currentLevel < liquidLevel) return;

        int px = ChunksManager.floorMod(gx, SubChunk.CHUNKSIZEXZ);
        int pz = ChunksManager.floorMod(gz, SubChunk.CHUNKSIZEXZ);

        boolean isBelowSolid;
        SubChunk downSub = null;
        int py = -1;

        if (gy - 1 < 0) {
            isBelowSolid = true;
        } else {
            int belowGy = gy - 1;
            if (Math.floorDiv(belowGy, SubChunk.CHUNKSIZEXZ) != Math.floorDiv(gy, SubChunk.CHUNKSIZEXZ)) {
                downSub = parentSub.getNeighborDown();
            } else {
                downSub = parentSub;
            }
            py = ChunksManager.floorMod(belowGy, SubChunk.CHUNKSIZEXZ);
            isBelowSolid = (downSub == null) || downSub.isBlockSolid(px, py, pz);
        }

        if (isBelowSolid) {
            // Sol sous nous : on étale horizontalement selon les directions prioritaires
            byte effectiveLevel = (liquidLevel == 15) ? (byte) Blocks.WATER.liquidLevel : liquidLevel;

            boolean[] drainDirs = findDrainDirections(gx, gy, gz, 4);

            for (int i = 0; i < HORIZONTAL_NEIGHBORS.length; i++) {
                if (!drainDirs[i]) continue;

                int[] delta = HORIZONTAL_NEIGHBORS[i];
                int nx = gx + delta[0];
                int ny = gy;
                int nz = gz + delta[2];

                if (ny < 0 || ny > SubChunk.CHUNKSIZEHEIGHT - 1) continue;

                int lx = ChunksManager.floorMod(nx, SubChunk.CHUNKSIZEXZ);
                int ly = ny % SubChunk.CHUNKSIZEXZ;
                int lz = ChunksManager.floorMod(nz, SubChunk.CHUNKSIZEXZ);

                Chunk chunk = ChunksManager.getChunkAt(nx, nz);
                if (chunk == null) continue;
                SubChunk sub = chunk.getSubChunkAt(ny);
                if (sub == null) continue;
                if (sub.isBlockSolid(lx, ly, lz)) continue;

                int newLiquidLevel = effectiveLevel - 1;
                if (newLiquidLevel < 1) continue;

                int neighborLiquidLevel = sub.getLiquidLevel(lx, ly, lz);
                if (neighborLiquidLevel >= newLiquidLevel) continue;

                long neighborEncoded = encode(nx, ny, nz, newLiquidLevel);
                if (!liquidQueuedSet.add(neighborEncoded)) continue;

                sub.setLiquidLevel(lx, ly, lz, newLiquidLevel);
                sub.blocks[lx + ly * SubChunk.CHUNKSIZEXZ + lz * SubChunk.CHUNKSIZEXZ * SubChunk.CHUNKSIZEXZ] = (byte) Blocks.WATER.id;
                schedule(neighborEncoded);

                if (!sub.dirty.compareAndSet(false, true)) continue;
                dirtySubChunks.add(sub);
            }
        } else {
            // Pas de sol sous nous : on tombe vers le bas en priorité (niveau 15)
            if (downSub == null) return;

            int belowCurrentLevel = downSub.getLiquidLevel(px, py, pz);
            if (belowCurrentLevel >= 15) return;

            long neighborEncoded = encode(gx, gy - 1, gz, 15);
            if (!liquidQueuedSet.add(neighborEncoded)) return;

            downSub.setLiquidLevel(px, py, pz, 15);
            downSub.blocks[px + py * SubChunk.CHUNKSIZEXZ + pz * SubChunk.CHUNKSIZEXZ * SubChunk.CHUNKSIZEXZ] = (byte) Blocks.WATER.id;
            schedule(neighborEncoded);

            if (downSub.dirty.compareAndSet(false, true)) {
                dirtySubChunks.add(downSub);
            }
        }
    }

    private static void bfsRemove(long packed) {
        int gx = (int) (((packed >>> 38) & MASK26) - OFFSET);
        int gz = (int) (((packed >>> 12) & MASK26) - OFFSET);
        int gy = (int)  ((packed >>>  4) & 0xFFL);
        byte liquidLevel = (byte)(packed & 0xFL);

        Chunk parentChunk = ChunksManager.getChunkAt(gx, gz);
        if (parentChunk == null) return;
        SubChunk parentSub = parentChunk.getSubChunkAt(gy);
        if (parentSub == null) return;

        int cpx = ChunksManager.floorMod(gx, SubChunk.CHUNKSIZEXZ);
        int cpy = ChunksManager.floorMod(gy, SubChunk.CHUNKSIZEXZ);
        int cpz = ChunksManager.floorMod(gz, SubChunk.CHUNKSIZEXZ);

        int currentLevel = parentSub.getLiquidLevel(cpx, cpy, cpz);
        // Si le bloc a été re-rempli par une source indépendante plus forte, on s'arrête
        if (currentLevel > liquidLevel) return;

        int px = ChunksManager.floorMod(gx, SubChunk.CHUNKSIZEXZ);
        int pz = ChunksManager.floorMod(gz, SubChunk.CHUNKSIZEXZ);

        boolean isBelowSolid;
        SubChunk downSub = null;
        int py = -1;

        if (gy - 1 < 0) {
            isBelowSolid = true;
        } else {
            int belowGy = gy - 1;
            if (Math.floorDiv(belowGy, SubChunk.CHUNKSIZEXZ) != Math.floorDiv(gy, SubChunk.CHUNKSIZEXZ)) {
                downSub = parentSub.getNeighborDown();
            } else {
                downSub = parentSub;
            }
            py = ChunksManager.floorMod(belowGy, SubChunk.CHUNKSIZEXZ);
            isBelowSolid = (downSub == null) || downSub.isBlockSolid(px, py, pz);
        }

        if (isBelowSolid) {
            byte effectiveLevel = (liquidLevel == 15) ? (byte) Blocks.WATER.liquidLevel : liquidLevel;

            for (int[] delta : HORIZONTAL_NEIGHBORS) {
                int nx = gx + delta[0];
                int ny = gy;
                int nz = gz + delta[2];

                if (ny < 0 || ny > SubChunk.CHUNKSIZEHEIGHT - 1) continue;

                int lx = ChunksManager.floorMod(nx, SubChunk.CHUNKSIZEXZ);
                int ly = ChunksManager.floorMod(ny, SubChunk.CHUNKSIZEXZ);
                int lz = ChunksManager.floorMod(nz, SubChunk.CHUNKSIZEXZ);

                Chunk chunk = ChunksManager.getChunkAt(nx, nz);
                if (chunk == null) continue;
                SubChunk sub = chunk.getSubChunkAt(ny);
                if (sub == null) continue;
                if (sub.isBlockSolid(lx, ly, lz)) continue;

                int neighborLiquidLevel = sub.getLiquidLevel(lx, ly, lz);
                if (neighborLiquidLevel <= 0) continue;

                if (neighborLiquidLevel < effectiveLevel) {
                    // Voisin DÉPENDANT : il était alimenté par nous, on le supprime
                    long relightEncoded = encode(nx, ny, nz, neighborLiquidLevel);
                    if (removalLiquidQueuedSet.add(relightEncoded)) {
                        sub.setLiquidLevel(lx, ly, lz, 0);
                        sub.blocks[lx + ly * SubChunk.CHUNKSIZEXZ + lz * SubChunk.CHUNKSIZEXZ * SubChunk.CHUNKSIZEXZ] = (byte) Blocks.AIR.id;
                        scheduleRemoval(relightEncoded);
                        if (sub.dirty.compareAndSet(false, true)) dirtySubChunks.add(sub);
                    }
                } else {
                    // Voisin INDÉPENDANT : a sa propre source → re-propager IMMÉDIATEMENT
                    // sans délai pour qu'il re-remplisse les trous avant la prochaine cascade
                    long relightEncoded = encode(nx, ny, nz, neighborLiquidLevel);
                    if (liquidQueuedSet.add(relightEncoded)) {
                        scheduleImmediate(relightEncoded);
                    }
                }
            }
        } else {
            // Suppression verticale : le bloc du dessous était alimenté par nous
            if (downSub == null) return;

            int belowActualLevel = downSub.getLiquidLevel(px, py, pz);
            if (belowActualLevel > 0) {
                long relightEncoded = encode(gx, gy - 1, gz, belowActualLevel);
                if (removalLiquidQueuedSet.add(relightEncoded)) {
                    downSub.setLiquidLevel(px, py, pz, 0);
                    downSub.blocks[px + py * SubChunk.CHUNKSIZEXZ + pz * SubChunk.CHUNKSIZEXZ * SubChunk.CHUNKSIZEXZ] = (byte) Blocks.AIR.id;
                    scheduleRemoval(relightEncoded);
                    if (downSub.dirty.compareAndSet(false, true)) dirtySubChunks.add(downSub);
                }
            }
        }
    }

    public static void processLiquidBFS() {
        // Drainer les pending (thread-safe) dans les ring buffers (single-thread)
        Long pending;
        while ((pending = pendingSchedule.pollFirst()) != null) {
            int bucket = (int)((currentTick + FLOW_STEP_DELAY) % FLOW_DELAY_TICKS);
            scheduledQueue[bucket].addLast(pending);
        }
        while ((pending = pendingRemovalSchedule.pollFirst()) != null) {
            int bucket = (int)((currentTick + REMOVAL_STEP_DELAY) % FLOW_DELAY_TICKS);
            scheduledRemovalQueue[bucket].addLast(pending);
        }

        int bucket = (int)(currentTick % FLOW_DELAY_TICKS);
        Long packed;

        while ((packed = scheduledQueue[bucket].pollFirst()) != null) {
            liquidQueuedSet.remove(packed);
            bfsPropagate(packed);
        }

        while ((packed = scheduledRemovalQueue[bucket].pollFirst()) != null) {
            removalLiquidQueuedSet.remove(packed);
            bfsRemove(packed);
        }

        currentTick++;
    }

    @OpenGLThread
    public static void flushDirtySubChunks() {
        SubChunk s;
        while ((s = dirtySubChunks.poll()) != null) {
            s.dirty.set(false);
            final SubChunk sub = s;
            ChunkGenerationPool.submit(() -> sub.rebuildSubChunk());
        }
    }

    public static void addDirtySubChunk(SubChunk sub) {
        dirtySubChunks.add(sub);
    }

    public static void clearAll() {
        liquidQueuedSet.clear();
        removalLiquidQueuedSet.clear();
        pendingSchedule.clear();
        pendingRemovalSchedule.clear();
        for (Deque<Long> q : scheduledQueue)        q.clear();
        for (Deque<Long> q : scheduledRemovalQueue) q.clear();
        dirtySubChunks.clear();
    }
}