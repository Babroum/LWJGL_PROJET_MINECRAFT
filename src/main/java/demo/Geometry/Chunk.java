package demo.Geometry;

import java.util.BitSet;

import org.joml.Matrix4f;

import demo.Annotations.ChunkThread;
import demo.Annotations.OpenGLThread;
import demo.Enum.BlocksType;
import demo.Enum.Directions;
import demo.Exceptions.IncorrectCoordinates;
import demo.Managers.ShaderManager;
import demo.Meshing.ChunkMesh;

public class Chunk extends Node3D{

    public static final int CHUNKSIZEX = 16;
    public static final int CHUNKSIZEY = 255;
    public static final int CHUNKSIZEZ = 16;
    public static final int MAXBLOCK = 65280;

    // on stocke pour chaque face un int qui represente une ligne
    private int[] northFace = new int[Chunk.CHUNKSIZEY * Chunk.CHUNKSIZEZ]; 
    private int[] southFace = new int[Chunk.CHUNKSIZEY * Chunk.CHUNKSIZEZ]; 
    private int[] westFace  = new int[Chunk.CHUNKSIZEY * Chunk.CHUNKSIZEX]; 
    private int[] eastFace  = new int[Chunk.CHUNKSIZEY * Chunk.CHUNKSIZEX]; 
    // plus tard (sub chunk)
    private BitSet upFace    = new BitSet(Chunk.MAXBLOCK); 
    private BitSet downFace  = new BitSet(Chunk.MAXBLOCK); 

    // donne au mesh le chunk au quel il appartient
    public ChunkMesh mesh = new ChunkMesh();
    private Object3D[] objects;



    // contient des 1 et 0 (block plein, block vide(air))
    private BitSet bitSet = new BitSet(Chunk.MAXBLOCK); 

    @ChunkThread
    public Chunk(){
        this.objects = new Object3D[Chunk.MAXBLOCK];
        for (int i = 0; i < Chunk.MAXBLOCK; i++){
            this.objects[i] = new Cube(BlocksType.AIR);
        }
        // définit la taille du buffer et du tableau de vertex  
        this.mesh.setupChunkBuffers();
    }

    /**
     * Place le chunk avec des coordonnées par rapport au autres chunk
     * @param chunkX
     * @param chunkY
     * @param chunkZ
     */
    @ChunkThread
    public void setChunkPosition(int chunkX, int chunkY, int chunkZ){
        this.x = chunkX * Chunk.CHUNKSIZEX;
        this.y = chunkY * Chunk.CHUNKSIZEY;
        this.z = chunkZ * Chunk.CHUNKSIZEZ;
    }

    /**
     * Génère la maille d'un chunk
     */
    @ChunkThread
    public void generateChunkMesh(){
        for(int i = 0; i < Chunk.MAXBLOCK; i++){
            if (this.bitSet.get(i)){ // retourne true si 1
                this.objects[i].setupObjectBuffers();
            }            
        }
        this.fillBitSet();
        this.calculateFaceCulling();
        // envoie la maille des objets à la maille
        this.uploadMesh();
        // rempli le buffer de la maille avec les données pack (vertex packing)
        this.mesh.fillChunkVertexBufferWithArray();
    }

    /**
     * Recréer la maille d'un chunk
     */
    @ChunkThread
    public void rebuildChunkMesh(){
        this.mesh.setupChunkBuffers();
        this.generateChunkMesh();
    }

    /**
     * Remplis le vbo avec les données des objets avant le rendu
     */
    @OpenGLThread
    public void setupChunkVBOVAO(){
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
    @ChunkThread
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
     * Ajoute un block dans le chunk (remplace le bloc AIR<br> 
     * (coordonnées locales du chunk)
     * @param type BlockType
     * @param x int
     * @param y int
     * @param z int
     * @throws IncorrectCoordinates
     */
    @ChunkThread
    public void addBlock(BlocksType type, int x, int y, int z) throws IncorrectCoordinates{
        if (x < 0 || x >= Chunk.CHUNKSIZEX ||
            y < 0 || y >= Chunk.CHUNKSIZEY ||
            z < 0 || z >= Chunk.CHUNKSIZEZ
        ){throw new IncorrectCoordinates("Coordonées dans le chunk invalide");}
        int index = x + y * Chunk.CHUNKSIZEX + z * Chunk.CHUNKSIZEX * Chunk.CHUNKSIZEY;
        // créer un cube et l'ajoute à la liste
        this.objects[index] = new Cube(type);
        // définit la position du cube
        this.objects[index].x = x;
        this.objects[index].y = y;
        this.objects[index].z = z;
    }

    /**
     * Retire un block dans le chunk (transforme en AIR) <br>
     * (coordonnées locales du chunk)
     * @param x int
     * @param y int
     * @param z int
     * @throws IncorrectCoordinates
     */
    @ChunkThread
    public void removeBlock(int x, int y, int z) throws IncorrectCoordinates{
        if (x < 0 || x >= Chunk.CHUNKSIZEX ||
            y < 0 || y >= Chunk.CHUNKSIZEY ||
            z < 0 || z >= Chunk.CHUNKSIZEZ
        ){throw new IncorrectCoordinates("Coordonées dans le chunk invalide");}
        int index = x + y * Chunk.CHUNKSIZEX + z * Chunk.CHUNKSIZEX * Chunk.CHUNKSIZEY;
        this.objects[index] = new Cube(BlocksType.AIR);
        this.objects[index].x = x;
        this.objects[index].y = y;
        this.objects[index].z = z;
    }

    /**
     * Envoie les faces à dessiné calculé avec le face culling à la maille
     */
    @ChunkThread
    public void uploadMesh(){
        // Ouest Est
        for (int y = 0; y < Chunk.CHUNKSIZEY; y++){
            for (int z = 0; z < Chunk.CHUNKSIZEZ; z++){
                int index = y + z * Chunk.CHUNKSIZEY;                

                int ligneEast = this.eastFace[index];
                while (ligneEast != 0){
                    // compte les 0 depuis la gauche, donc donne la position de la face à dessiner
                    int x = Integer.numberOfLeadingZeros(ligneEast);
                    // calcule la position de l'objet
                    int objectIndex = x + y * Chunk.CHUNKSIZEX + z * Chunk.CHUNKSIZEX * Chunk.CHUNKSIZEY;
                    // on dessine la face
                    this.mesh.addObjectFaceVertexToArray(this.objects[objectIndex].mesh.getFaceVertex(Directions.EAST), x, y, z);
                    // (1 << (31 - x)) crée un masque avec un 1 en position x
                    // ~ inverse le masque donc la face dessiner devient un 0 et le reste 1
                    // & fait AND donc applique le masque sur la ligne donc tout reste intact sauf la face dessiné
                    ligneEast &= ~(1 << (31 - x)); // efface le bit
                    // on recommence temps que la ligne n'est pas vide
                }

                int ligneWest = this.westFace[index];
                while (ligneWest != 0){
                    int x = Integer.numberOfLeadingZeros(ligneWest);
                    int objectIndex = x + y * Chunk.CHUNKSIZEX + z * Chunk.CHUNKSIZEX * Chunk.CHUNKSIZEY;
                    this.mesh.addObjectFaceVertexToArray(this.objects[objectIndex].mesh.getFaceVertex(Directions.WEST), x, y, z);
                    ligneWest &= ~(1 << (31 - x));
                }
            }
        }

        // Nord Sud
        for (int y = 0; y < Chunk.CHUNKSIZEY; y++){
            for (int x = 0; x < Chunk.CHUNKSIZEX; x++){
                int index = y + x * Chunk.CHUNKSIZEY;                

                int ligneNorth = this.northFace[index];
                while (ligneNorth != 0){
                    int z = Integer.numberOfLeadingZeros(ligneNorth);
                    int objectIndex = x + y * Chunk.CHUNKSIZEX + z * Chunk.CHUNKSIZEX * Chunk.CHUNKSIZEY;
                    this.mesh.addObjectFaceVertexToArray(this.objects[objectIndex].mesh.getFaceVertex(Directions.NORTH), x, y, z);
                    ligneNorth &= ~(1 << (31 - z));
                }

                int ligneSouth = this.southFace[index];
                while (ligneSouth != 0){
                    int z = Integer.numberOfLeadingZeros(ligneSouth);
                    int objectIndex = x + y * Chunk.CHUNKSIZEX + z * Chunk.CHUNKSIZEX * Chunk.CHUNKSIZEY;
                    this.mesh.addObjectFaceVertexToArray(this.objects[objectIndex].mesh.getFaceVertex(Directions.SOUTH), x, y, z);
                    ligneSouth &= ~(1 << (31 - z));
                }
            }
        }
        // Haut bas plus tard
    }

    /**
     * Comme son nom l'indique
     */
    public void calculateFaceCulling(){
        // Récup ouest est, on remplit l'int qui correspond a la ligne avec le BitSet du chunk
        for (int y = 0; y < Chunk.CHUNKSIZEY; y++){
            for (int z = 0; z < Chunk.CHUNKSIZEZ; z++){
                int index = y + z * Chunk.CHUNKSIZEY;
                for (int x = 0; x < Chunk.CHUNKSIZEX; x++){
                    int bitSetIndex = x + y * Chunk.CHUNKSIZEX + z * Chunk.CHUNKSIZEX * Chunk.CHUNKSIZEY;
                    // prend la valeur du bit set et la pack dans l'entier correspondant a la ligne
                    this.eastFace[index] |= (this.bitSet.get(bitSetIndex) ? 1 : 0) << (31 - x);                    
                }
                int ligne = this.eastFace[index];
                // on décale de 1 puis inverse et on fait le AND avec la face est ce qui a était calculé avant
                this.eastFace[index] = ligne & ~(ligne << 1); // faces vers l'est
                this.westFace[index] = ligne & ~(ligne >> 1); // faces vers l'ouest           
            }
        }
        // Récup nord sud
        for (int y = 0; y < Chunk.CHUNKSIZEY; y++){
            for (int x = 0; x < Chunk.CHUNKSIZEX; x++){
                int index = y + x * Chunk.CHUNKSIZEY;
                for (int z = 0; z < Chunk.CHUNKSIZEZ; z++){
                    int bitSetIndex = x + y * Chunk.CHUNKSIZEX + z * Chunk.CHUNKSIZEX * Chunk.CHUNKSIZEY;
                    this.northFace[index] |= (this.bitSet.get(bitSetIndex) ? 1 : 0) << (31 - z);
                }
                int ligne = this.northFace[index];
                this.northFace[index] = ligne & ~(ligne << 1); // faces vers le nord
                this.southFace[index] = ligne & ~(ligne >> 1); // faces vers le sud  
            }
        }
        // Récup haut bas
        /*
        for (int z = 0; z < Chunk.CHUNKSIZEZ; z++){
            for (int x = 0; x < Chunk.CHUNKSIZEX; x++){
                for (int y = 0; y < Chunk.CHUNKSIZEY; y++){
                    int index = x + y * Chunk.CHUNKSIZEX + z * Chunk.CHUNKSIZEX * Chunk.CHUNKSIZEY;

                }
            }
        }
        */
    }

    /**
     * Retourne les objets du chunk
     * @return
     */
    public Object3D[] getChunkObject(){
        return this.objects;
    }

    /**
     * Remplis le BitSet du chunk (cube plein = 1, cube vide = 0)
     */
    @ChunkThread
    public void fillBitSet(){
        for(int i = 0; i < Chunk.MAXBLOCK; i++){
            if (this.objects[i].blockType != BlocksType.AIR){
                this.bitSet.set(i); // met a 1 l'indice i
            }
        }
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
