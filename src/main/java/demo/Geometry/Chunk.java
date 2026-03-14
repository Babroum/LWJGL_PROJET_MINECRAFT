package demo.Geometry;

import java.util.Arrays;

import org.joml.Matrix4f;

import demo.Annotations.ChunkThread;
import demo.Annotations.OpenGLThread;
import demo.Enum.BlocksType;
import demo.Exceptions.IncorrectCoordinates;
import demo.Rendering.FrustumCulling;
import demo.Rendering.Render;

/**
 * Un chunk, contient des SubChunks
 */
public class Chunk extends Node3D{
    
    public static final int SUBCHUNKCOUNT = 16;
    public SubChunk[] subChunks;

    private int minX;
    private int minY;
    private int minZ;

    private int maxX;
    private int maxY;
    private int maxZ;

    /** Y global du premier bloc solide de la colonne, -1 si colonne entièrement vide */
    public int[] heightMap = new int[SubChunk.CHUNKSIZEX * SubChunk.CHUNKSIZEZ];
    
    /**
     * Initialise les subChunks du chunks
     * @param chunkX position du chunk en X coordonnées locales
     * @param chunkZ position du chunk en Z coordonnées locales
     */
    @ChunkThread
    public Chunk(int chunkX, int chunkZ){
        this.subChunks = new SubChunk[Chunk.SUBCHUNKCOUNT];
        for (int i = 0; i < this.subChunks.length; i++){
            this.subChunks[i] = new SubChunk();
            this.subChunks[i].setSubChunkPosition(0, i * SubChunk.CHUNKSIZEY, 0);
        }
        this.setChunkPosition(chunkX, chunkZ);
        
        this.minX = this.x;
        this.minY = this.y;
        this.minZ = this.z;

        this.maxX = this.minX + SubChunk.CHUNKSIZEX;
        this.maxY = this.minY + SubChunk.CHUNKSIZEY * Chunk.SUBCHUNKCOUNT;
        this.maxZ = this.minZ + SubChunk.CHUNKSIZEZ;
        
        Render.monitor.addChunkCount();
    }

    /**
     * Calcule le heightMap pour toutes les colonnes du chunk.
     * Doit être appelé après génération des blocs, avant propagation lumière.
     */
    @ChunkThread
    public void buildHeightMap() {
        Arrays.fill(heightMap, -1);
        for (int x = 0; x < SubChunk.CHUNKSIZEX; x++) {
            for (int z = 0; z < SubChunk.CHUNKSIZEZ; z++) {
                updateHeightMapAt(x, z);
            }
        }
    }

    /**
     * Recalcule le heightMap pour une seule colonne.
     * Appelé après chaque ajout/suppression de bloc.
     * @param x coordonnée locale x (0-15)
     * @param z coordonnée locale z (0-15)
     */
    @ChunkThread
    public void updateHeightMapAt(int x, int z) {
        heightMap[x + z * SubChunk.CHUNKSIZEX] = -1;
        // descend de haut en bas à travers tous les subChunks
        outer: // label d'une boucle 
        for (int i = SUBCHUNKCOUNT - 1; i >= 0; i--) {
            for (int y = SubChunk.CHUNKSIZEY - 1; y >= 0; y--) {
                int idx = x + y * SubChunk.CHUNKSIZEX + z * SubChunk.CHUNKSIZEX * SubChunk.CHUNKSIZEY;
                if (subChunks[i].blocks[idx] != (byte) BlocksType.AIR.getId()) {
                    heightMap[x + z * SubChunk.CHUNKSIZEX] = i * SubChunk.CHUNKSIZEY + y;
                    break outer; // permet de casser la boucle for avec le label outer pour évité de faire un double break
                }
            }
        }
    }

    /**
     * Définis la position du chunk
     * @param chunkX position du chunk en X coordonnées locales
     * @param chunkZ position du chunk en Z coordonnées locales
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
        this.subChunks[y / SubChunk.CHUNKSIZEY].addBlockToSubChunk(type, x, y % SubChunk.CHUNKSIZEY, z, true);
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

    @OpenGLThread
    @Override
    public void drawNode(Matrix4f parentModel){
        // si le chunk est visible on l'affiche
        if (FrustumCulling.testAABB(this.minX, this.minY, this.minZ, this.maxX, this.maxY, this.maxZ)){
            for (SubChunk sub : subChunks){
                if (sub.mesh.getVAO() == 0) continue; // subchunk vide, rien à dessiner
                sub.drawNode(parentModel);
            } 
        }               
    }
}
