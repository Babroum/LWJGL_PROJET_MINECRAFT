package demo.Geometry;

import org.joml.Matrix4f;

import demo.Annotations.ChunkThread;
import demo.Annotations.OpenGLThread;
import demo.Enum.BlocksType;
import demo.Exceptions.IncorrectCoordinates;
import demo.Rendering.Render;

/**
 * Un chunk, contient des SubChunks
 */
public class Chunk extends Node3D{
    
    public static final int SUBCHUNKCOUNT = 16;
    public SubChunk[] subChunks;
    public String name;
    
    /**
     * Initialise les subChunks du chunks
     */
    @ChunkThread
    public Chunk(String name){
        this.subChunks = new SubChunk[Chunk.SUBCHUNKCOUNT];
        for (int i = 0; i < this.subChunks.length; i++){
            this.subChunks[i] = new SubChunk();
            this.subChunks[i].setSubChunkPosition(0, i * SubChunk.CHUNKSIZEY, 0);
            this.subChunks[i].name = name + "," + i;
            Render.monitor.addSubChunkCount();
        }
        Render.monitor.addChunkCount();
    }

    /**
     * Définis la position du chunk
     * @param chunkX int
     * @param chunkZ int
     */
    @ChunkThread
    public void setChunkPosition(int chunkX, int chunkZ){
        this.x = chunkX * SubChunk.CHUNKSIZEX;
        this.z = chunkZ * SubChunk.CHUNKSIZEZ;
        for (int i = 0; i < this.subChunks.length; i++){
            this.subChunks[i].setSubChunkPosition(this.x, i * SubChunk.CHUNKSIZEY, this.z);
        }
    }

    /**
     * Ajoute un sol au chunk
     * @param y int, hauteur du sol
     * @param nbFloor int, épaisseur du sol
     * @param type BlocksType
     */
    @ChunkThread
    public void addFloorToChunk(int y, int nbFloor, BlocksType type){
        this.subChunks[y / SubChunk.CHUNKSIZEY].addFloorToSubChunk(y % SubChunk.CHUNKSIZEY, nbFloor % (SubChunk.CHUNKSIZEY + 1), type);
    }
    
    /**
     * Ajoute un block dans le chunk <br> 
     * (coordonnées locales du chunk)
     * @param type BlockType
     * @param x int
     * @param y int
     * @param z int
     * @throws IncorrectCoordinates
     */
    @ChunkThread
    public void addBlockToChunk(BlocksType type, int x, int y, int z) throws IncorrectCoordinates{
        if (x < 0 || x >= SubChunk.CHUNKSIZEX ||
            y < 0 || y >= SubChunk.CHUNKSIZEY * Chunk.SUBCHUNKCOUNT ||
            z < 0 || z >= SubChunk.CHUNKSIZEZ
        ){throw new IncorrectCoordinates("Coordonées dans le chunk invalide");}
        this.subChunks[y / SubChunk.CHUNKSIZEY].addBlockToSubChunk(type, x, y % SubChunk.CHUNKSIZEY, z);
    }

    /**
     * Retire un block dans le chunk <br>
     * (coordonnées locales du subChunk)
     * @param x int
     * @param y int
     * @param z int
     * @throws IncorrectCoordinates
     */
    @ChunkThread
    public void removeBlockFromChunk(int x, int y, int z) throws IncorrectCoordinates{
        if (x < 0 || x >= SubChunk.CHUNKSIZEX ||
            y < 0 || y >= SubChunk.CHUNKSIZEY * Chunk.SUBCHUNKCOUNT ||
            z < 0 || z >= SubChunk.CHUNKSIZEZ
        ){throw new IncorrectCoordinates("Coordonées dans le chunk invalide");}
        this.subChunks[y / SubChunk.CHUNKSIZEY].removeBlockFromSubChunk(x, y % SubChunk.CHUNKSIZEY, z);
    }

    /**
     * Génère la maille des subChunks
     */
    @ChunkThread
    public void generateChunkMesh(){
        for(SubChunk sub : this.subChunks){
            sub.fillSubChunkBitSet();
        } 
        for(SubChunk sub : this.subChunks){
            sub.generateSubChunkMesh();
        }
    }

    /**
     * Permet d'obtenir le subChunk d'un chunk
     * @param y int 
     * @throws IncorrectCoordinates
     * @return SubChunk
     */
    @ChunkThread
    public SubChunk getSubChunk(int y) throws IncorrectCoordinates{ 
        if (y < 0 || y >= Chunk.SUBCHUNKCOUNT) throw new IncorrectCoordinates("Les coordonées sont incorrects (0 pour le subChunk n°0, 1 ... n°1, etc...");
        return this.subChunks[y];
    }

    /**
     * Retourne le tableau des sous chunks
     * @return SubChunk[]
     */
    public SubChunk[] getAllSubChunks(){
        return this.subChunks;
    }

    /**
     * Remplis les vbo des subChunks avec les données des objets avant le rendu
     */
    @OpenGLThread
    public void setupChunkVBOVAO(){
        for(SubChunk sub : this.subChunks) sub.setupSubChunkVBOVAO();
    }

    @Override
    public void drawNode(Matrix4f parentModel){
        for (SubChunk sub : subChunks){
            sub.drawNode(parentModel);
        }
    }
}
