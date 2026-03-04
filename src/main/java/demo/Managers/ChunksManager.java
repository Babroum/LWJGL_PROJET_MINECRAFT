package demo.Managers;

import java.util.Collection;
import java.util.LinkedHashMap;

import org.joml.Math;

import demo.Annotations.ChunkThread;
import demo.Enum.BlocksType;
import demo.Exceptions.IncorrectCoordinates;
import demo.Geometry.Chunk;
import demo.Geometry.SubChunk;
import demo.Rendering.Render;

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
        return chunk;
    }

    /**
     * Finalise la construction du chunk
     * @param chunk Chunk
     */
    @ChunkThread
    public static void finalizeChunk(Chunk chunk){
        for(SubChunk sub : chunk.subChunks){
            sub.mesh.setupSubChunkBuffers(sub.bitSet.cardinality()); // recrée vertexArrayPack avant de générer
            sub.generateSubChunkMesh();
            sub.linkNeighbors();
            Render.dirtySubChunks.add(sub);
        }
        Render.chunkQueue.add(chunk);
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
    public static void finalizeAllChunk(){
        for(Chunk chunk : ChunksManager.chunks.values()){
            ChunksManager.finalizeChunk(chunk);
        }
    }

    /**
     * Retourne un block dans l'un des chunks
     * Coordonnée global
     * @param x int
     * @param y int
     * @param z int
     * @return Object3D
     * @throws IncorrectCoordinates
     */
    public static BlocksType getBlock(int x, int y, int z) throws IncorrectCoordinates{
        if (y < 0 || y >= SubChunk.CHUNKSIZEY * Chunk.SUBCHUNKCOUNT){throw new IncorrectCoordinates("Valeur en Y incorrect");}

        int chunkX = (int) Math.floor((float) x / SubChunk.CHUNKSIZEX);
        int chunkZ = (int) Math.floor((float) z / SubChunk.CHUNKSIZEZ);

        Chunk chunk = ChunksManager.getChunk(chunkX, chunkZ);

        SubChunk sub = chunk.getSubChunk(y / SubChunk.CHUNKSIZEY);

        int localX = ChunksManager.floorMod(x, SubChunk.CHUNKSIZEX);
        int localY = ChunksManager.floorMod(y, SubChunk.CHUNKSIZEY);
        int localZ = ChunksManager.floorMod(z, SubChunk.CHUNKSIZEZ);
  
        return sub.getBlock(localX, localY, localZ);
    }

    /**
     * Retourne un subChunk dans l'un des chunks
     * Coordonnée global
     * @param x int
     * @param y int
     * @param z int
     * @return SubChunk
     * @throws IncorrectCoordinates
     */
    public static SubChunk getSubChunk(int x, int y, int z) throws IncorrectCoordinates{
        if (y < 0 || y >= SubChunk.CHUNKSIZEY * Chunk.SUBCHUNKCOUNT){throw new IncorrectCoordinates("Valeur en Y incorrect");}

        int chunkX = (int) Math.floor((float) x / SubChunk.CHUNKSIZEX);
        int chunkZ = (int) Math.floor((float) z / SubChunk.CHUNKSIZEZ);

        Chunk chunk = ChunksManager.getChunk(chunkX, chunkZ);

        SubChunk sub = chunk.getSubChunk(y / SubChunk.CHUNKSIZEY);

        return sub;
    }

    /**
     * Calcule le modulo avec toujours un résultat positif 
     * @param x
     * @param mod
     * @return int
     */
    public static int floorMod(int x, int mod) {
        return ((x % mod) + mod) % mod;
    }
}
