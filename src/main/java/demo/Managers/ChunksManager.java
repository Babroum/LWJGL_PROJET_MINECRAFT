package demo.Managers;

import java.util.Collection;
import java.util.LinkedHashMap;

import org.joml.Math;

import demo.Annotations.ChunkThread;
import demo.Exceptions.IncorrectCoordinates;
import demo.Geometry.Chunk;
import demo.Geometry.SubChunk;
import demo.Rendering.Render;
import demo.Geometry.Object3D;

public class ChunksManager {

    // HashMap fait pareil mais linked permet de garder l'ordre dans lequel c'est insérer
    // C'est comme un dictionnaire, clé valeur
    private static LinkedHashMap<String, Chunk> chunks = new LinkedHashMap<>();
    
    /**
     * Créer un nouveau chunk à la position donné
     * @param x int
     * @param z int
     * @throws IncorrectCoordinates
     */
    @ChunkThread
    public static Chunk createChunk(int x, int z) throws IncorrectCoordinates{
        String coord = String.valueOf(x)+","+String.valueOf(z);
        if (ChunksManager.chunks.containsKey(coord)) 
            {throw new IncorrectCoordinates("Le chunk x: " + x + " z: "+ z + " existe déjà");}
        Chunk chunk = new Chunk(x + "," + z);
        chunk.setChunkPosition(x, z);
        // il faut tout créer avant de généré la mesh ça évite de devoir la regénérer
        // sauf pour les test on s'en fout
        ChunksManager.chunks.put(coord, chunk);
        chunk.generateChunkMesh();

        return chunk;
    }

    /**
     * Retourne le chunk à la position donné
     * @param x int
     * @param z int
     * @throws IncorrectCoordinates
     */
    @ChunkThread
    public static Chunk getChunk(int x, int z) throws IncorrectCoordinates{
        String coord = String.valueOf(x)+","+String.valueOf(z);
        if (!ChunksManager.chunks.containsKey(coord)){
            throw new IncorrectCoordinates("Le chunk x: " + x + " z: "+ z + " n'existe pas");
        }
        return ChunksManager.chunks.get(coord);
    }

    /**
     * Retourne la collection de tous les chunks
     * @return Collection<Chunk>
     */
    public static Collection<Chunk> getAllChunks(){
        return ChunksManager.chunks.values();
    }

    /**
     * Supprime le chunk à la position donné
     * @param x int
     * @param z int
     * @throws IncorrectCoordinates
     */
    @ChunkThread
    public static void removeChunk(int x, int z) throws IncorrectCoordinates{
        String coord = String.valueOf(x)+","+String.valueOf(z);
        if (!ChunksManager.chunks.containsKey(coord)){
            throw new IncorrectCoordinates("Le chunk x: " + x + " z: "+ z + " n'existe pas");
        }    
        // Récupère le chunk
        Chunk chunk = ChunksManager.chunks.get(coord);
        // Supprime les VBO VAO de chaque chunk, le garbage collector fait le reste tout seul
        for(SubChunk sub : chunk.subChunks) sub.removeSubChunk();
        // Et supprime le chunk du dico
        ChunksManager.chunks.remove(coord);
    }

    /**
     * Reconstruit tout les chunks <br>
     * Pas optimal
     */
    public static void rebuildAllCHunks(){
        for(Chunk chunk : ChunksManager.chunks.values()){
            for(SubChunk sub : chunk.subChunks){
                sub.fillSubChunkBitSet();
            }
        }
        for(Chunk chunk : ChunksManager.chunks.values()){
            for(SubChunk sub : chunk.subChunks){
                sub.mesh.setupSubChunkBuffers();
                sub.generateSubChunkMesh();
                Render.dirtySubChunks.add(sub);
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
     * @throws 
     */
    public static Object3D getBlock(int x, int y, int z) throws IncorrectCoordinates{
        //System.out.println("-------------------------------------------------------------");
        //System.out.println("[getBlock] global: x=" + x + " y=" + y + " z=" + z);
        if (y < 0 || y >= SubChunk.CHUNKSIZEY * Chunk.SUBCHUNKCOUNT){throw new IncorrectCoordinates("Valeur en Y incorrect");}

        int chunkX = (int) Math.floor((float) x / SubChunk.CHUNKSIZEX);
        int chunkZ = (int) Math.floor((float) z / SubChunk.CHUNKSIZEZ);
        //System.out.println("[getBlock] chunk: chunkX=" + chunkX + " chunkZ=" + chunkZ);

        Chunk chunk = ChunksManager.getChunk(chunkX, chunkZ);

        //System.out.println("[getBlock] subChunk index: " + y / SubChunk.CHUNKSIZEY);
        SubChunk sub = chunk.getSubChunk(y / SubChunk.CHUNKSIZEY);

        // Math.floorMod pour avoir un modulo toujours positif
        int localX = ChunksManager.floorMod(x, SubChunk.CHUNKSIZEX);
        int localY = ChunksManager.floorMod(y, SubChunk.CHUNKSIZEY);
        int localZ = ChunksManager.floorMod(z, SubChunk.CHUNKSIZEZ);
        //System.out.println("[getBlock] local: x=" + localX + " y=" + localY + " z=" + localZ);
  
        return sub.getBlock(localX, localY, localZ);
    }

    private static int floorMod(int x, int mod) {
        return ((x % mod) + mod) % mod;
    }
}
