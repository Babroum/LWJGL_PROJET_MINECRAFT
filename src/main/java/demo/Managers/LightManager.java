package demo.Managers;

import java.util.Deque;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;

import demo.Annotations.OpenGLThread;
import demo.Geometry.Chunk;
import demo.Geometry.SubChunk;

public class LightManager {

    private static final Deque<Long> skyLightQueue      = new ConcurrentLinkedDeque<>();
    private static final Deque<Long> blockLightQueue    = new ConcurrentLinkedDeque<>();
    private static final Deque<Long> removalSkyQueue    = new ConcurrentLinkedDeque<>();
    private static final Deque<Long> removalBlockQueue  = new ConcurrentLinkedDeque<>(); 

    private static final Queue<SubChunk> dirtySubChunks = new ConcurrentLinkedQueue<>();

    private static final long OFFSET = 1L << 25; // 33 554 432
    private static final long MASK26 = 0x3FFFFFFL;

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
    // 26 bits X/Z = +/-33 millions
    private static long encode(int gx, int gy, int gz, int light) {
        long px = (gx + OFFSET) & MASK26;   // 26 bits
        long pz = (gz + OFFSET) & MASK26;
        long py = gy & 0xFFL;                   // 8 bits (256)
        long pl = light & 0xFL;                 // 4 bits

        return (px << 38) | (pz << 12) | (py <<  4) |  pl;
    }

    public static void addSkyLight(Chunk chunk, int x, int y, int z, byte light){
        if (light <= 1) return;
        int gx = chunk.x + x;
        int gz = chunk.z + z;
        if (skyLightQueue.contains(encode(gx, y, gz, light))) return;
        skyLightQueue.push(encode(gx, y, gz, light));  
    }

    public static void addBlockLight(Chunk chunk, int x, int y, int z, byte light) {
        if (light <= 1) return;
        int gx = chunk.x + x;
        int gz = chunk.z + z;
        if (blockLightQueue.contains(encode(gx, y, gz, light))) return;
        blockLightQueue.push(encode(gx, y, gz, light));      
    }

    private static void bfsPropagate(long packed, boolean isSky){
        int gx = (int) (((packed >>> 38) & MASK26) - OFFSET);
        int gz = (int) (((packed >>> 12) & MASK26) - OFFSET);
        int gy = (int)  ((packed >>>  4) & 0xFFL);
        byte light = (byte)(packed & 0xFL);  
        
        for (int[] delta : LIGHT_NEIGHBORS) {
            // coor voisin globales
            int nx = gx + delta[0];
            int ny = gy + delta[1];
            int nz = gz + delta[2];
            // coor voisin locales (propre au subchunk)
            int lx = ChunksManager.floorMod(nx, SubChunk.CHUNKSIZEXZ);
            int ly = ny % SubChunk.CHUNKSIZEXZ;
            int lz = ChunksManager.floorMod(nz, SubChunk.CHUNKSIZEXZ);

            // si en dehors de la map
            if (ny < 0 || ny > SubChunk.CHUNKSIZEHEIGHT - 1) continue;

            Chunk chunk = ChunksManager.getChunkAt(nx, nz);
            // si n'existe pas
            if (chunk == null) continue;

            SubChunk sub = chunk.getSubChunkAt(ny); // fils de pute qui me fait galérer pdt 1j
            // si n'existe pas
            if (sub == null) continue;

            // si block solide
            if (sub.isBlockSolid(lx, ly, lz)) {
                //System.out.println("  Skip (" + nx + ", " + ny + ", " + nz + ") solide, id : " + sub.getBlock(lx, ly, lz).name);
                continue;
            }

            // lumière du voisin
            int neighborLight = isSky ? sub.getBlockSkyLight(lx, ly, lz) : sub.getBlockLight(lx, ly, lz);
            
            // si lumière du voisin supèrieur ou égal au block actuel alors on skip (une source de lumière plus importante existe)
            if (neighborLight >= light) {
                //System.out.println("  Skip (" + nx + ", " + ny + ", " + nz + ") neighborLight=" + neighborLight + " >= level=" + light);
                continue;
            }

            int neighborOpacity = sub.getBlock(lx, ly, lz).opacity;           
            int newLight = light - neighborOpacity;
            
            if (newLight < 1) {
                //System.out.println("  Skip (" + nx + ", " + ny + ", " + nz + ") newLight=" + newLight + " <= 1");
                continue;
            }

            if (isSky) {                
                if (skyLightQueue.contains(encode(nx, ny, nz, newLight))) {
                    //System.out.println("  SKY Already IN (" + nx + ", " + ny + ", " + nz + ")");                            
                    continue;            
                } else {
                    //System.out.println("  SKY GOOD (" + nx + ", " + ny + ", " + nz + ") newLight= " + newLight);
                    sub.setBlockSkyLight(lx, ly, lz, newLight);
                    skyLightQueue.addLast(encode(nx, ny, nz, newLight));
                }                
            } else {
                if (blockLightQueue.contains(encode(nx, ny, nz, newLight))) {
                    //System.out.println("  Already IN (" + nx + ", " + ny + ", " + nz + ")");
                    continue;
                } else {
                    //System.out.println("  GOOD (" + nx + ", " + ny + ", " + nz + ") newLight= " + newLight);                    
                    sub.setBlockLight(lx, ly, lz, newLight);
                    blockLightQueue.addLast(encode(nx, ny, nz, newLight));
                } 
            }

            if (!sub.dirty) {
                //System.out.println("  Rebuild (" + nx + ", " + ny + ", " + nz + ")");
                sub.dirty = true;
                dirtySubChunks.add(sub);
            }
        }
    }

    @OpenGLThread
    public static void updateLight() {
        // Propagation lumière ciel
        while (!skyLightQueue.isEmpty()) {
            long packed = skyLightQueue.pollFirst();
            bfsPropagate(packed, true);
        }

        // Propagation lumière blocs
        while (!blockLightQueue.isEmpty()) {
            long packed = blockLightQueue.pollFirst();
            bfsPropagate(packed, false);
        }
        
        SubChunk s;
        while ((s = dirtySubChunks.poll()) != null) {         
            s.rebuildSubChunk();
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
