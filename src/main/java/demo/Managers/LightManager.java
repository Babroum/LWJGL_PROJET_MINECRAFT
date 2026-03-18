package demo.Managers;

import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

import demo.Annotations.OpenGLThread;
import demo.Geometry.Chunk;
import demo.Geometry.SubChunk;

public class LightManager {

    private static final Deque<Long> skyLightQueue   = new ConcurrentLinkedDeque<>();
    private static final Deque<Long> blockLightQueue = new ConcurrentLinkedDeque<>();
    private static final Deque<Long> removalSkyQueue   = new ConcurrentLinkedDeque<>();
    private static final Deque<Long> removalBlockQueue = new ConcurrentLinkedDeque<>(); 
    private static Set<SubChunk> dirtySubChunks = ConcurrentHashMap.newKeySet();

    private static final int[][] LIGHT_NEIGHBORS = {
        {1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}
    };

    /**
     * Encode (x,y,z,light) en un Long pour les files BFS. <br>
     * @param x Coordonnées en x (globale)
     * @param y Coordonnées en y (globale)
     * @param z Coordonnées en z (globale)
     * @param light Intensité de lumière
     * @return Retourne les 4 paramètres encodés dans un long
     */
    // 27 bits X/Z = +/-67 millions
    private static long encode(int gx, int gy, int gz, int light) {
        final long OFFSET = 1L << 26;           // 67 108 864

        long px = (gx + OFFSET) & 0x7FFFFFFFL;  // 27 bits
        long pz = (gz + OFFSET) & 0x7FFFFFFFL;
        long py = gy & 0xFFL;                   // 8 bits (256)
        long pl = light & 0xFL;                 // 4 bits

        return (px << 39) | (pz << 12) | (py <<  4) |  pl;
    }

    public static void addSkyLight(Chunk chunk, int x, int y, int z, byte light){
        if (light <= 1) return;
        int gx = chunk.x + x;
        int gz = chunk.z + z;
        skyLightQueue.push(encode(gx, y, gz, light));
        dirtySubChunks.add(chunk.getSubChunkAt(y));
    }

    public static void addBlockLight(Chunk chunk, int x, int y, int z, byte light) {
        if (light <= 1) return;
        int gx = chunk.x + x;
        int gz = chunk.z + z;
        blockLightQueue.push(encode(gx, y, gz, light));
        dirtySubChunks.add(chunk.getSubChunkAt(y));
    }

    private static void bfsPropagate(long packed, boolean isSky){
        int gx = (int) (((packed >>> 39) & 0x7FFFFFFFL) - (1L << 26));
        int gz = (int) (((packed >>> 12) & 0x7FFFFFFFL) - (1L << 26));
        int gy = (int)  ((packed >>>  4) & 0xFFL);
        byte level = (byte)(packed & 0xFL);

        for (int[] delta : LIGHT_NEIGHBORS) {
            int nx = gx + delta[0];
            int ny = gy + delta[1];
            int nz = gz + delta[2];
            int lx = ChunksManager.floorMod(nx, SubChunk.CHUNKSIZEX);
            int ly = ny % SubChunk.CHUNKSIZEY;
            int lz = ChunksManager.floorMod(nz, SubChunk.CHUNKSIZEZ);

            // si en dehors de la map
            if (ny < 0 || ny > SubChunk.CHUNKSIZEY * Chunk.SUBCHUNKCOUNT - 1) continue;

            Chunk chunk = ChunksManager.getChunkAt(nx, nz);
            // si n'existe pas
            if (chunk == null) continue;

            int subY = ny / SubChunk.CHUNKSIZEY; // quel subchunk vertical
            SubChunk sub = chunk.getSubChunkAt(subY);
            // si n'existe pas
            if (sub == null) continue;

            // lumière du voisin
            byte oldLevel = isSky ? sub.getBlockSkyLight(lx, ly, lz) : sub.getBlockLight(lx, ly, lz);

            // si block solide
            if (sub.isBlockSolid(lx, ly, lz)) continue;
            // si lumière du voisin supèrieur ou égal au block actuel alors on skip (une source de lumière plus importante existe)
            if (oldLevel >= level) continue;

            int opacity = sub.getBlock(lx, ly, lz).opacity;            
            byte decreased = (byte) (level - opacity);
            
            if (decreased <= 1) continue;

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

        // Rebuild uniquement les subchunks touchés (une seule fois)        
        if (!dirtySubChunks.isEmpty()) {
            for (SubChunk s : dirtySubChunks) {
                s.rebuildSubChunk();
            }
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
