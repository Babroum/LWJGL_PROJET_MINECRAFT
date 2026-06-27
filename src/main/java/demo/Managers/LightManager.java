package demo.Managers;

import java.util.Deque;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;

import demo.Annotations.OpenGLThread;
import demo.Geometry.Chunk;
import demo.Geometry.SubChunk;

public class LightManager {

    // utilise des sets pour vérifié si une lumière est dans la queue, car O(1) sur un set meilleur que O(n) sur une queue
    private static final Set<Long> skyLightQueuedSet          = ConcurrentHashMap.newKeySet();
    private static final Set<Long> blockLightQueuedSet        = ConcurrentHashMap.newKeySet();
    private static final Set<Long> removalSkyLightQueuedSet   = ConcurrentHashMap.newKeySet();
    private static final Set<Long> removalBlockLightQueuedSet = ConcurrentHashMap.newKeySet();

    private static final Deque<Long> skyLightQueue            = new ConcurrentLinkedDeque<>();
    private static final Deque<Long> blockLightQueue          = new ConcurrentLinkedDeque<>();
    private static final Deque<Long> removalSkyLightQueue     = new ConcurrentLinkedDeque<>();
    private static final Deque<Long> removalBlockLightQueue   = new ConcurrentLinkedDeque<>(); 

    private static final Queue<SubChunk> dirtySubChunks       = new ConcurrentLinkedQueue<>();

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

    /**
     * Ajoute une lumière du ciel à la queue de propagation
     * @param chunk Chunk dans le quel l'ajout de lumière est
     * @param x Coordonnées en x (locale) 0-15
     * @param y Coordonnées en y (locale) 0-255
     * @param z Coordonnées en z (locale) 0-15
     * @param light niveau de lumière à propager 
     */
    public static void addSkyLight(Chunk chunk, int x, int y, int z, byte light){
        if (light <= 1) return;
        int gx = chunk.x + x;
        int gz = chunk.z + z;
        if (!skyLightQueuedSet.add(encode(gx, y, gz, light))) return; // déjà présent -> stop
        skyLightQueue.push(encode(gx, y, gz, light));
    }

    /**
     * Ajoute une lumière d'un bloc à la queue de propagation
     * @param chunk Chunk dans le quel l'ajout de lumière est
     * @param x Coordonnées en x (locale) 0-15
     * @param y Coordonnées en y (locale) 0-255
     * @param z Coordonnées en z (locale) 0-15
     * @param light niveau de lumière à propager 
     */
    public static void addBlockLight(Chunk chunk, int x, int y, int z, byte light) {
        if (light <= 1) return;
        int gx = chunk.x + x;
        int gz = chunk.z + z;
        if (!blockLightQueuedSet.add(encode(gx, y, gz, light))) return;
        blockLightQueue.push(encode(gx, y, gz, light));
    }

    /**
     * Ajoute une lumière du ciel à la queue de suppresion
     * @param chunk Chunk dans le quel la suppression de lumière est
     * @param x Coordonnées en x (locale) 0-15
     * @param y Coordonnées en y (locale) 0-255
     * @param z Coordonnées en z (locale) 0-15
     * @param light niveau de lumière à propager 
     */
    public static void removeSkyLight(Chunk chunk, int x, int y, int z, byte light){
        if (light <= 1) return;
        int gx = chunk.x + x;
        int gz = chunk.z + z;
        if (!removalSkyLightQueuedSet.add(encode(gx, y, gz, light))) return;
        removalSkyLightQueue.push(encode(gx, y, gz, light));
    }

    /**
     * Ajoute une lumière d'un bloc à la queue de suppresion
     * @param chunk Chunk dans le quel la suppression de lumière est
     * @param x Coordonnées en x (locale) 0-15
     * @param y Coordonnées en y (locale) 0-255
     * @param z Coordonnées en z (locale) 0-15
     * @param light niveau de lumière à propager 
     */
    public static void removeBlockLight(Chunk chunk, int x, int y, int z, byte light) {
        if (light <= 1) return;
        int gx = chunk.x + x;
        int gz = chunk.z + z;
        if (!removalBlockLightQueuedSet.add(encode(gx, y, gz, light))) return;
        removalBlockLightQueue.push(encode(gx, y, gz, light));
    }

    /**
     * Propage une lumière
     * @param packed lumière encodée (x, y, z, light)
     * @param isSky vrai si la lumière vient du ciel
     */
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

            // si bloc solide
            if (sub.isBlockSolid(lx, ly, lz)) {
                //System.out.println("  Skip (" + nx + ", " + ny + ", " + nz + ") solide, id : " + sub.getbloc(lx, ly, lz).name);
                continue;
            }

            // lumière du voisin
            int neighborLight = isSky ? sub.getBlockSkyLight(lx, ly, lz) : sub.getBlockLight(lx, ly, lz);
            
            // si lumière du voisin supèrieur ou égal au bloc actuel alors on skip (une source de lumière plus importante existe)
            if (neighborLight >= light) {
                //System.out.println("  Skip (" + nx + ", " + ny + ", " + nz + ") neighborLight=" + neighborLight + " >= level=" + light);
                continue;
            }

            int neighborOpacity = sub.getBlock(lx, ly, lz).opacity;           
            int newLight = light - neighborOpacity;
            
            if (newLight < 1) {
                continue;
            }
              
            if (isSky) {                
                if (!skyLightQueuedSet.add(encode(nx, ny, nz, newLight))) {                       
                    continue;            
                } else {
                    sub.setBlockSkyLight(lx, ly, lz, newLight);
                    skyLightQueue.addLast(encode(nx, ny, nz, newLight));
                }                
            } else {
                if (!blockLightQueuedSet.add(encode(nx, ny, nz, newLight))) {
                    continue;
                } else {               
                    sub.setBlockLight(lx, ly, lz, newLight);
                    blockLightQueue.addLast(encode(nx, ny, nz, newLight));
                } 
            }            

            if (!sub.dirty.compareAndSet(false, true)) continue;
            dirtySubChunks.add(sub);            
        }
    }

    /**
     * Supprime une lumière en propageant la suppression
     * @param packed lumière encodée (x, y, z, light)
     * @param isSky vrai si la lumière vient du ciel
     */
    private static void bfsRemove(long packed, boolean isSky){
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

            // si bloc solide
            if (sub.isBlockSolid(lx, ly, lz)) {
                continue;
            }

            // lumière du voisin
            int neighborSkyLight = sub.getBlockSkyLight(lx, ly, lz);
            int neighborBlockLight = sub.getBlockLight(lx, ly, lz);
            
            // si lumière du voisin est inférieur à la lumière actuel, alors le voisin et dépendant du bloc actuel
            // sinon il est indépendant et il faut donc repropagé
            if (isSky){
                if (neighborSkyLight < light){
                    if (!removalSkyLightQueuedSet.add(encode(nx, ny, nz, neighborSkyLight))){ 
                        continue;          
                    } else {
                        sub.setBlockSkyLight(lx, ly, lz, 0);
                        removalSkyLightQueue.addLast(encode(nx, ny, nz, neighborSkyLight));
                    }
                }else{
                    if (!skyLightQueuedSet.add(encode(nx, ny, nz, neighborSkyLight))) {                       
                        continue;            
                    } else {
                        sub.setBlockSkyLight(lx, ly, lz, neighborSkyLight);
                        skyLightQueue.addLast(encode(nx, ny, nz, neighborSkyLight));
                    } 
                }
            }else{
                if (neighborBlockLight < light){
                    if (!removalBlockLightQueuedSet.add(encode(nx, ny, nz, neighborBlockLight))) {
                        continue;
                    } else {             
                        sub.setBlockLight(lx, ly, lz, 0);
                        removalBlockLightQueue.addLast(encode(nx, ny, nz, neighborBlockLight));
                    } 
                }else{
                    if (!blockLightQueuedSet.add(encode(nx, ny, nz, neighborBlockLight))) {
                        continue;
                    } else {               
                        sub.setBlockLight(lx, ly, lz, neighborBlockLight);
                        blockLightQueue.addLast(encode(nx, ny, nz, neighborBlockLight));
                    } 
                }
            }

            if (!sub.dirty.compareAndSet(false, true)) continue;
            dirtySubChunks.add(sub);  
        }
    }
    
    // BFS pur : peut tourner sur n'importe quel thread (pas OpenGL)
    public static void processLightBFS() {
        Long packed;

        while ((packed = removalBlockLightQueue.pollFirst()) != null) {
            removalBlockLightQueuedSet.remove(packed);
            bfsRemove(packed, false);
        }
        while ((packed = removalSkyLightQueue.pollFirst()) != null) {
            removalSkyLightQueuedSet.remove(packed);
            bfsRemove(packed, true);
        }                
        while ((packed = blockLightQueue.pollFirst()) != null) {
            blockLightQueuedSet.remove(packed);
            bfsPropagate(packed, false);
        }
        while ((packed = skyLightQueue.pollFirst()) != null) {
            skyLightQueuedSet.remove(packed);
            bfsPropagate(packed, true);
        }
    }

    // Rebuild mesh : OpenGL uniquement, appelé depuis la render loop
    @OpenGLThread
    public static void flushDirtySubChunks() {
        SubChunk s;
        while ((s = dirtySubChunks.poll()) != null) {
            s.dirty.set(false);
            s.rebuildSubChunk();
        }
    }

    public static void addDirtySubChunk(SubChunk sub) {
        dirtySubChunks.add(sub);
    }
    
    public static void clearAll() {
        skyLightQueuedSet.clear();
        blockLightQueuedSet.clear();
        removalBlockLightQueuedSet.clear();
        removalSkyLightQueuedSet.clear();

        skyLightQueue.clear();
        blockLightQueue.clear();
        removalSkyLightQueue.clear();
        removalBlockLightQueue.clear();

        dirtySubChunks.clear();
    }
}
