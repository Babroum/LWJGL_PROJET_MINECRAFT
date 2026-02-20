package demo.Geometry;

import demo.Enum.BlocksType;
import demo.Exceptions.IncorrectCoordinates;
import demo.Meshing.ChunkMesh;

public class Chunk extends Node3D{

    public static final int CHUNKSIZEX = 16;
    public static final int CHUNKSIZEY = 255;
    public static final int CHUNKSIZEZ = 16;
    public static final int MAXBLOCK = 65280;

    public ChunkMesh mesh = new ChunkMesh();
    private Object3D[] objects;
    private int objectCount;

    public Chunk(){
        this.objects = new Object3D[Chunk.MAXBLOCK];
        this.objectCount = 0;
        this.mesh.setupChunkBuffers();
    }

    /**
     * Ajoute un sol 
     * @param y int, hauteur du sol
     */
    public void addFloor(int y){
        for(int x = 0; x < Chunk.CHUNKSIZEX; x++){
            for (int z = 0; z < Chunk.CHUNKSIZEZ; z++){
                this.addBlock(BlocksType.GRASS, x, y, z);
            }
        }
    }

    /**
     * Envoie la maille de chaque bloc à la maille du chunk
     */
    public void uploadMesh(){
        for(Object3D obj : this.objects){
            this.mesh.addObjectVertexToArray(obj.mesh.getVertexArray());
        }
    }

    /**
     * Ajoute un block dans le chunk <br> 
     * (coordonnées locales du chunk)
     * @param type BlockType
     * @param x int
     * @param y int
     * @param z int
     */
    public void addBlock(BlocksType type, int x, int y, int z) throws IncorrectCoordinates{
        if (x < 0 || x >= Chunk.CHUNKSIZEX ||
            y < 0 || y >= Chunk.CHUNKSIZEY ||
            z < 0 || z >= Chunk.CHUNKSIZEZ
        ){throw new IncorrectCoordinates("Coordonées dans le chunk invalide");}
        // créer un cube et l'ajoute à la liste
        this.objects[objectCount] = new Cube(BlocksType.GRASS);
        // ajoute le cube en temps que enfant
        this.addChild(this.objects[objectCount]);
        // définit la position du cube
        this.objects[objectCount].x = x;
        this.objects[objectCount].y = y;
        this.objects[objectCount].z = z;
        this.objectCount++;
    }
}
