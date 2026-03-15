package demo.Managers;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.concurrent.ConcurrentLinkedDeque;

import demo.Annotations.OpenGLThread;
import demo.Geometry.Chunk;
import demo.Geometry.SubChunk;

public class LightManager {

    private static Deque<Long> lightQueue = new ConcurrentLinkedDeque<>();
    private static Deque<Long> removalQueue = new ConcurrentLinkedDeque<>();
    private static HashSet<SubChunk> dirtySubChunks = new HashSet<>();

    private static final int[][] LIGHT_NEIGHBORS = {
        {1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}
    };

    /**
     * Encode (x,y,z,light) en un Long pour les files BFS. <br>
     * Offset +32 sur chaque coord pour absorber les valeurs négatives de bordure.
     * @param x Coordonnées en x
     * @param y Coordonnées en y
     * @param z Coordonnées en z
     * @param light Intensité de lumière
     * @param type Type de lumière, ciel(0) ou block(1)
     * @return Retourne les 4 paramètres encodés dans un long
     */
    private static long encodeLightEntry(int x, int y, int z, int light, int type) {
        // 6bits, 6bits, 6bits, 4bits, 1bits
        return ((long)(x+32) << 58) | ((long)(y+32) << 52) | ((long)(z+32) << 46) | ((long)light << 42) | ((long)type << 41);
    }

    public static void addBlockToQueue(int x, int y, int z, int light, int type){
        LightManager.lightQueue.push(LightManager.encodeLightEntry(x, y, z, light, type));

        Chunk chunk = ChunksManager.getChunk(x / SubChunk.CHUNKSIZEX, z / SubChunk.CHUNKSIZEZ);
        LightManager.dirtySubChunks.add(chunk.getSubChunk(y / SubChunk.CHUNKSIZEY));
    }

    private static void bfsPropagate(long block){
        int posX   = (int)((block >>> 58) & 0x3F) - 32;
        int posY   = (int)((block >>> 52) & 0x3F) - 32; 
        int posZ   = (int)((block >>> 46) & 0x3F) - 32; 
        int light =  (int)((block >>> 42) & 0xF); 
        int type   = (int)((block >>> 41) & 0x1); 
        System.out.println("posX : " + posX + ", posY : " + posY + ", posZ : " + posZ);

        if (light <= 1) return;

        for (int[] n : LightManager.LIGHT_NEIGHBORS){
            int nx = posX + n[0];
            int ny = posY + n[1];
            int nz = posZ + n[2];
            int localX = ChunksManager.floorMod(nx, SubChunk.CHUNKSIZEX);
            int localZ = ChunksManager.floorMod(nz, SubChunk.CHUNKSIZEZ);
            int localY = ny % SubChunk.CHUNKSIZEY;

            if (ny < 0) continue;

            Chunk chunk = ChunksManager.getChunk(nx / SubChunk.CHUNKSIZEX, nz / SubChunk.CHUNKSIZEZ);
            SubChunk sub = chunk.getSubChunk(ny / SubChunk.CHUNKSIZEY);

            //if (sub.isBlockSolid(nx, ny, nz)) continue;

            int oldLight = type == 0 ? sub.getBlockSkyLight(localX, localY, localZ) : sub.getBlockLight(localX, localY, localZ);

            if (oldLight < light){
                byte newLight = (byte)(light - 1);
                if (type == 0) sub.setBlockSkyLight(localX, localY, localZ, newLight); else sub.setBlockLight(localX, localY, localZ, newLight);
                LightManager.lightQueue.push(LightManager.encodeLightEntry(nx, ny, nz, newLight, type));
            }
        }
    }

    @OpenGLThread
    public static void updateLight(){
        synchronized(LightManager.lightQueue) {
            while (!LightManager.lightQueue.isEmpty()) {
                long block = LightManager.lightQueue.poll();
                LightManager.bfsPropagate(block);
            }
        }
        while (!LightManager.removalQueue.isEmpty()) {
            long block = LightManager.removalQueue.poll();
            // bfs remove
        }
        for (SubChunk s : LightManager.dirtySubChunks) { s.rebuildSubChunk(); }
    }

}
