package demo.Managers;

import java.util.LinkedHashMap;

import demo.Annotations.ChunkThread;
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
}
