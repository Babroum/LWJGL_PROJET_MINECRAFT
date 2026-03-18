package demo.Managers;

import java.util.Deque;
import java.util.HashSet;
import java.util.concurrent.ConcurrentLinkedDeque;

import demo.Annotations.OpenGLThread;
import demo.Geometry.Chunk;
import demo.Geometry.SubChunk;
import demo.Rendering.Render;

public class LightManager {

    private static final Deque<Long> skyLightQueue   = new ConcurrentLinkedDeque<>();
    private static final Deque<Long> blockLightQueue = new ConcurrentLinkedDeque<>();
    private static final Deque<Long> removalSkyQueue   = new ConcurrentLinkedDeque<>();
    private static final Deque<Long> removalBlockQueue = new ConcurrentLinkedDeque<>(); 
    private static HashSet<SubChunk> dirtySubChunks = new HashSet<>();

    private static final int[][] LIGHT_NEIGHBORS = {
        {1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}
    };

    /**
     * Encode (x,y,z,light) en un Long pour les files BFS. <br>
     * Offset +32 sur chaque coord pour absorber les valeurs négatives de bordure.
     * @param x Coordonnées en x (locale)
     * @param y Coordonnées en y (locale)
     * @param z Coordonnées en z (locale)
     * @param light Intensité de lumière
     * @param type Type de lumière, ciel(0) ou block(1)
     * @return Retourne les 4 paramètres encodés dans un long
     */
    // 27 bits X/Z = +/-67 millions
    private static long encode(int gx, int gy, int gz, int lightLevel) {
        final long OFFSET = 1L << 26;           // 67 108 864

        long px = (gx + OFFSET) & 0x7FFFFFFFL;  // 27 bits
        long pz = (gz + OFFSET) & 0x7FFFFFFFL;
        long py = gy & 0xFFL;                   // 8 bits (256)
        long pl = lightLevel & 0xFL;            // 4 bits

        return (px << 39) | (pz << 12) | (py <<  4) |  pl;
    }

    /**
     * Décode le long.
     * @param val Valeur à décodé dans le long
     * @param out Résultat de ce qu'il y avait dans le long
     */
    private static void unpack(long val, int[] out) {   // out[0]=gx, [1]=gy, [2]=gz, [3]=light
        final long OFFSET = 1L << 26;

        out[0] = (int) (((val >>> 39) & 0x7FFFFFFFL) - OFFSET);
        out[2] = (int) (((val >>> 12) & 0x7FFFFFFFL) - OFFSET);
        out[1] = (int)  ((val >>>  4) & 0xFFL);
        out[3] = (int)   (val         & 0xFL);
    }

    public static void addSkyLight(Chunk chunk, int x, int y, int z, byte light){
        if (light <= 1) return;
        int gx = chunk.x * SubChunk.CHUNKSIZEX + x;
        int gz = chunk.z * SubChunk.CHUNKSIZEZ + z;
        skyLightQueue.push(encode(gx, y, gz, light));
        dirtySubChunks.add(chunk.getSubChunkAt(y));
    }

    public static void addBlockLight(Chunk chunk, int x, int y, int z, byte light) {
        if (light <= 1) return;
        int gx = chunk.x * SubChunk.CHUNKSIZEX + x;
        int gz = chunk.z * SubChunk.CHUNKSIZEZ + z;
        blockLightQueue.push(encode(gx, y, gz, light));
        dirtySubChunks.add(chunk.getSubChunkAt(y));
    }

    private static void bfsPropagate(long packed, boolean isSky){
        int[] data = new int[4];
        LightManager.unpack(packed, data);

        int gx = data[0];
        int gy = data[1];
        int gz = data[2];
        byte level = (byte) data[3];

        for (int[] delta : LIGHT_NEIGHBORS) {
            int nx = gx + delta[0];
            int ny = gy + delta[1];
            int nz = gz + delta[2];

            if (ny < 0 || ny > SubChunk.CHUNKSIZEY * Chunk.SUBCHUNKCOUNT - 1) continue;

            Chunk chunk = ChunksManager.getChunkAt(nx, nz);
            if (chunk == null) continue;

            int subY = ny / SubChunk.CHUNKSIZEY; // quel subchunk vertical
            SubChunk sub = chunk.getSubChunkAt(subY);
            if (sub == null) continue;

            int lx = ChunksManager.floorMod(nx, SubChunk.CHUNKSIZEX);
            int ly = ny % SubChunk.CHUNKSIZEY;
            int lz = ChunksManager.floorMod(nz, SubChunk.CHUNKSIZEZ);


            byte oldLevel = isSky ? sub.getBlockSkyLight(lx, ly, lz) : sub.getBlockLight(lx, ly, lz);

            if (sub.isBlockSolid(lx, ly, lz)) continue;
            if (oldLevel >= level) continue;

            byte decreased = level;

            if (delta != LIGHT_NEIGHBORS[2]){
                decreased = (byte) (level - 1);
            }            

            if (isSky) {
                sub.setBlockSkyLight(lx, ly, lz, decreased);
                skyLightQueue.push(encode(nx, ny, nz, decreased));
            } else {
                sub.setBlockLight(lx, ly, lz, decreased);
                blockLightQueue.push(encode(nx, ny, nz, decreased));
            }

            dirtySubChunks.add(sub);
        }
    }

    @OpenGLThread
    public static void updateLight() {
        // Propagation lumière ciel
        while (!skyLightQueue.isEmpty()) {
            long packed = skyLightQueue.poll();
            bfsPropagate(packed, true);
        }

        // Propagation lumière blocs
        while (!blockLightQueue.isEmpty()) {
            long packed = blockLightQueue.poll();
            bfsPropagate(packed, false);
        }

        // ── À implémenter plus tard ──
        // while (!removalSkyQueue.isEmpty())   { ... }
        // while (!removalBlockQueue.isEmpty()) { ... }

        // Rebuild uniquement les subchunks touchés (une seule fois)
        
        if (!dirtySubChunks.isEmpty()) {
            for (SubChunk s : LightManager.dirtySubChunks) { s.rebuildSubChunk(); }
        }
    }
    
    public static void clearAll() {
        skyLightQueue.clear();
        blockLightQueue.clear();
        removalSkyQueue.clear();
        removalBlockQueue.clear();
        dirtySubChunks.clear();
    }
}
