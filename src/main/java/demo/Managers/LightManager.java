package demo.Managers;

import java.util.Deque;
import java.util.HashSet;
import java.util.concurrent.ConcurrentLinkedDeque;

import demo.Annotations.OpenGLThread;
import demo.Geometry.Chunk;
import demo.Geometry.SubChunk;
import demo.Types.LightInfo;

public class LightManager {

    private static Deque<LightInfo> lightQueue = new ConcurrentLinkedDeque<>();
    private static Deque<LightInfo> removalQueue = new ConcurrentLinkedDeque<>();
    private static HashSet<SubChunk> dirtySubChunks = new HashSet<>();

    private static final int[][] LIGHT_NEIGHBORS = {
        {1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}
    };

    /**
     * Encode (x,y,z,light) pour les files BFS. <br>
     * @param x Coordonnées en x
     * @param y Coordonnées en y
     * @param z Coordonnées en z
     * @param light Intensité de lumière
     * @param type Type de lumière, ciel(0) ou block(1)
     * @return Retourne les 5 paramètres encodés
     */
    private static LightInfo encodeLightEntry(int x, int y, int z, byte light, byte type) {
        return new LightInfo(x, y, z, (byte)0, (byte)0);
    }

    public static void addBlockToQueue(int x, int y, int z, byte light, byte type){
        LightManager.lightQueue.push(LightManager.encodeLightEntry(x, y, z, light, type));

        Chunk chunk = ChunksManager.getChunkAt(x, z);
        LightManager.dirtySubChunks.add(chunk.getSubChunkAt(y));
    }

    private static void bfsPropagate(LightInfo block){
        int posX   = block.x;
        int posY   = block.y; 
        int posZ   = block.z; 
        byte light  = block.light; 
        byte type   = block.type; 
        System.out.println("Apres encodage posX : " + posX + ", posY : " + posY + ", posZ : " + posZ);

        if (light <= 1) return;

        for (int[] n : LightManager.LIGHT_NEIGHBORS){
            int nx = posX + n[0];
            int ny = posY + n[1];
            int nz = posZ + n[2];
            int localX = ChunksManager.floorMod(nx, SubChunk.CHUNKSIZEX);
            int localZ = ChunksManager.floorMod(nz, SubChunk.CHUNKSIZEZ);
            int localY = ny % SubChunk.CHUNKSIZEY;

            if (ny < 0) continue;

            Chunk chunk = ChunksManager.getChunkAt(nx, nz);
            SubChunk sub = chunk.getSubChunkAt(ny);

            if (sub.isBlockSolid(nx, ny, nz)) continue; // chunkmanager floor mod

            byte oldLight = type == 0 ? sub.getBlockSkyLight(localX, localY, localZ) : sub.getBlockLight(localX, localY, localZ);

            if (oldLight < light){
                byte newLight = (byte)(light - 1);
                if (type == 0) sub.setBlockSkyLight(localX, localY, localZ, newLight); else sub.setBlockLight(localX, localY, localZ, newLight);
                LightManager.lightQueue.push(LightManager.encodeLightEntry(nx, ny, nz, newLight, (byte)type));
            }
        }
    }

    @OpenGLThread
    public static void updateLight(){
        synchronized(LightManager.lightQueue) {
            while (!LightManager.lightQueue.isEmpty()) {
                LightInfo block = LightManager.lightQueue.poll();
                LightManager.bfsPropagate(block);
            }
        }
        while (!LightManager.removalQueue.isEmpty()) {
            LightInfo block = LightManager.removalQueue.poll();
            // bfs remove
        }
        for (SubChunk s : LightManager.dirtySubChunks) { s.rebuildSubChunk(); }
    }

}
