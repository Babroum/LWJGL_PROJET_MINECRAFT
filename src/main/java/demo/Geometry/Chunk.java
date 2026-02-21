package demo.Geometry;

import org.joml.Matrix4f;

import demo.Annotations.OpenGLThread;
import demo.Enum.BlocksType;
import demo.Exceptions.IncorrectCoordinates;
import demo.Managers.ShaderManager;
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
        // définit la taille du buffer et du tableau de vertex
        this.mesh.setupChunkBuffers();
    }

    /**
     * Place le chunk avec des coordonnées par rapport au autres chunk
     * @param chunkX
     * @param chunkY
     * @param chunkZ
     */
    public void setChunkPosition(int chunkX, int chunkY, int chunkZ){
        this.x = chunkX * Chunk.CHUNKSIZEX;
        this.y = chunkY * Chunk.CHUNKSIZEY;
        this.z = chunkZ * Chunk.CHUNKSIZEZ;
    }

    /**
     * Remplis le vbo avec les données des objets avant le rendu
     */
    public void setupChunkBuffers(){
        // envoie la maille des objets à la maille
        this.uploadMesh();
        // rempli le buffer de la maille avec les données pack (vertex packing)
        this.mesh.fillChunkVertexBufferWithArray();
        // créer les vbo vao avec le buffer rempli
        this.mesh.createChunkVBOVAO();
        this.mesh.createChunkBorderVBOVAO();
    }

    /**
     * Ajoute un sol 
     * @param y int, hauteur du sol
     * @param nbFloor int, épaisseur du sol
     * @param type BlocksType
     */
    public void addFloor(int Y, int nbFloor, BlocksType type){
        if (Y < 0 || Y + nbFloor >= Chunk.CHUNKSIZEY
        ){throw new IncorrectCoordinates("Coordonées dans le chunk invalide");}
        for(int y = Y; y < Y + nbFloor; y++){
            for(int x = 0; x < Chunk.CHUNKSIZEX; x++){
                for (int z = 0; z < Chunk.CHUNKSIZEZ; z++){
                    this.addBlock(type, x, y, z);          
                }    
            }
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
        this.objects[objectCount] = new Cube(type);
        // ajoute le cube en temps que enfant du chunk
        this.addChild(this.objects[objectCount]);
        // définit la position du cube
        this.objects[objectCount].x = x;
        this.objects[objectCount].y = y;
        this.objects[objectCount].z = z;
        this.objectCount++;
    }

    /**
     * Envoie la maille de chaque bloc à la maille du chunk
     */
    public void uploadMesh(){
        for (int i = 0; i < this.objectCount; i++){
            int cubeX =this.objects[i].x;
            int cubeY =this.objects[i].y;
            int cubeZ =this.objects[i].z;
            this.mesh.addObjectVertexToArray(this.objects[i].mesh.getVertexArray(), cubeX, cubeY, cubeZ);
        }
    }

    public Object3D[] getChunkObject(){
        return this.objects;
    }

    /**
     * Dessine la maille du chunk
     */
    @OpenGLThread
    @Override
    public void drawSpecific(){
        this.mesh.drawChunkMesh(this.buffer);
    }

    @Override
    public void drawNode(Matrix4f parentModel){
        Matrix4f model = new Matrix4f(parentModel)
            .translate(this.x, this.y, this.z);
        model.get(this.buffer);
        ShaderManager.runModel(this.buffer);
        this.drawSpecific();
        // pas d'itération sur les cubes enfants
    }
}
