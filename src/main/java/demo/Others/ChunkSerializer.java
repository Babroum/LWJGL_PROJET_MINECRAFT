package demo.Others;

import java.io.IOException;
import java.nio.file.*;

import demo.Geometry.Chunk;
import demo.Managers.ChunksManager;

public class ChunkSerializer {
    private static final String SAVE_DIR = "saves/world/";

    public static void init(){
        try{                       
            Path path = Paths.get(ChunkSerializer.SAVE_DIR);
            Files.createDirectories(path);
        }catch (IOException e){
            System.out.println(e);
        }
    }

    public static void save(Chunk chunk){
        byte[] blocks = chunk.getAllSubChunkBlocks();
        try{
            
            Path path = ChunkSerializer.getPath(ChunksManager.floorDiv(chunk.x, 16),ChunksManager.floorDiv(chunk.z, 16));
            Files.write(path, blocks);
        }catch (IOException e){
            System.out.println(e);
        }
    }

    public static byte[] load(int cx, int cz){
        if (!exists(cx, cz)) return null;
        try{
            Path path = ChunkSerializer.getPath(cx, cz);
            byte[] blocks = Files.readAllBytes(path);
            return blocks;
        }catch(IOException e){
            System.out.println(e);
            return null;
        }
    }

    /**
     * Retourne la Path pour un chunk
     * @param cx Coordonnées en x (locale)
     * @param cz Coordonnées en x (locale)
     * @return Retourne le Path, exemple : ".../1_2.chunk"
     */
    private static Path getPath(int cx, int cz){
        return Paths.get(ChunkSerializer.SAVE_DIR + cx + "_" + cz + ".chunk");
    }

    private static boolean exists(int cx, int cz){
        Path path = ChunkSerializer.getPath(cx, cz);
        return Files.exists(path);
    }
}
