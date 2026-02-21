package demo.Geometry;

import java.util.BitSet;

import org.joml.Matrix4f;

import demo.Annotations.ChunkThread;
import demo.Annotations.OpenGLThread;
import demo.Enum.BlocksType;
import demo.Enum.Directions;
import demo.Exceptions.IncorrectCoordinates;
import demo.Managers.ShaderManager;
import demo.Meshing.SubChunkMesh;

public class SubChunk extends Node3D{

    public static final int CHUNKSIZEX = 16;
    public static final int CHUNKSIZEY = 16;
    public static final int CHUNKSIZEZ = 16;
    public static final int MAXBLOCK = 4096;

    // on stocke pour chaque face un int qui represente une ligne
    private int[] northFace = new int[SubChunk.CHUNKSIZEY * SubChunk.CHUNKSIZEZ]; 
    private int[] southFace = new int[SubChunk.CHUNKSIZEY * SubChunk.CHUNKSIZEZ]; 
    private int[] westFace  = new int[SubChunk.CHUNKSIZEY * SubChunk.CHUNKSIZEX]; 
    private int[] eastFace  = new int[SubChunk.CHUNKSIZEY * SubChunk.CHUNKSIZEX]; 
    private int[] upFace    = new int[SubChunk.CHUNKSIZEY * SubChunk.CHUNKSIZEX]; 
    private int[] downFace  = new int[SubChunk.CHUNKSIZEY * SubChunk.CHUNKSIZEX]; 

    // donne au mesh le subChunk au quel il appartient
    public SubChunkMesh mesh = new SubChunkMesh();
    private Object3D[] objects;

    // contient des 1 et 0 (block plein, block vide(air))
    private BitSet bitSet = new BitSet(SubChunk.MAXBLOCK); 

    @ChunkThread
    public SubChunk(){
        this.objects = new Object3D[SubChunk.MAXBLOCK];
        // définit la taille du buffer et du tableau de vertex  
        this.mesh.setupChunkBuffers();
    }

    /**
     * Place le subChunk avec des coordonnées par rapport au autres subChunk
     * @param x int
     * @param y int
     * @param z int
     */
    @ChunkThread
    public void setSubChunkPosition(int x, int y, int z){
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /**
     * Génère la maille d'un subChunk
     */
    @ChunkThread
    public void generateSubChunkMesh(){
        this.fillSubChunkBitSet();
        this.calculateSubChunkFaceCulling();
        // envoie la maille des objets à la maille
        this.uploadSubChunkMesh();
        // rempli le buffer de la maille avec les données pack (vertex packing)
        this.mesh.fillChunkVertexBufferWithArray();
    }

    /**
     * Recréer la maille d'un subChunk
     */
    @ChunkThread
    public void rebuildSubChunkMesh(){
        this.mesh.setupChunkBuffers();
        this.generateSubChunkMesh();
    }

    /**
     * Remplis le vbo avec les données des objets avant le rendu
     */
    @OpenGLThread
    public void setupSubChunkVBOVAO(){
        // créer les vbo vao avec le buffer rempli
        this.mesh.createChunkVBOVAO();
        this.mesh.createChunkBorderVBOVAO();
    }

    /**
     * Ajoute un sol au subChunk
     * @param Y int, hauteur du sol
     * @param nbFloor int, épaisseur du sol
     * @param type BlocksType
     */
    @ChunkThread
    public void addFloorToSubChunk(int Y, int nbFloor, BlocksType type){
        if (Y < 0 || Y + nbFloor > SubChunk.CHUNKSIZEY
        ){throw new IncorrectCoordinates("Coordonées dans le subChunk invalide");}
        for(int y = Y; y < Y + nbFloor; y++){
            for(int x = 0; x < SubChunk.CHUNKSIZEX; x++){
                for (int z = 0; z < SubChunk.CHUNKSIZEZ; z++){
                    this.addBlockToSubChunk(type, x, y, z);          
                }    
            }
        }  
    }

    /**
     * Ajoute un block dans le subChunk <br> 
     * (coordonnées locales du subChunk)
     * @param type BlockType
     * @param x int
     * @param y int
     * @param z int
     * @throws IncorrectCoordinates
     */
    @ChunkThread
    public void addBlockToSubChunk(BlocksType type, int x, int y, int z) throws IncorrectCoordinates{
        if (x < 0 || x >= SubChunk.CHUNKSIZEX ||
            y < 0 || y >= SubChunk.CHUNKSIZEY ||
            z < 0 || z >= SubChunk.CHUNKSIZEZ
        ){throw new IncorrectCoordinates("Coordonées dans le subChunk invalide");}
        int index = x + y * SubChunk.CHUNKSIZEX + z * SubChunk.CHUNKSIZEX * SubChunk.CHUNKSIZEY;
        // créer un cube et l'ajoute à la liste
        this.objects[index] = new Cube(type);
        // définit la position du cube
        this.objects[index].x = x;
        this.objects[index].y = y;
        this.objects[index].z = z;
        this.objects[index].setupObjectBuffers();
    }

    /**
     * Retire un block dans le subChunk <br>
     * (coordonnées locales du subChunk)
     * @param x int
     * @param y int
     * @param z int
     * @throws IncorrectCoordinates
     */
    @ChunkThread
    public void removeBlockFromSubChunk(int x, int y, int z) throws IncorrectCoordinates{
        if (x < 0 || x >= SubChunk.CHUNKSIZEX ||
            y < 0 || y >= SubChunk.CHUNKSIZEY ||
            z < 0 || z >= SubChunk.CHUNKSIZEZ
        ){throw new IncorrectCoordinates("Coordonées dans le subChunk invalide");}
        int index = x + y * SubChunk.CHUNKSIZEX + z * SubChunk.CHUNKSIZEX * SubChunk.CHUNKSIZEY;
        this.objects[index] = null;
    }

    /**
     * Envoie les faces à dessiné calculé avec le face culling à la maille
     */
    @ChunkThread
    private void uploadSubChunkMesh(){
        // Ouest Est
        for (int y = 0; y < SubChunk.CHUNKSIZEY; y++){
            for (int z = 0; z < SubChunk.CHUNKSIZEZ; z++){
                int index = y + z * SubChunk.CHUNKSIZEY;                

                int ligneEast = this.eastFace[index];
                while (ligneEast != 0){
                    // compte les 0 depuis la gauche, donc donne la position de la face à dessiner
                    int x = Integer.numberOfLeadingZeros(ligneEast);
                    // calcule la position de l'objet
                    int objectIndex = x + y * SubChunk.CHUNKSIZEX + z * SubChunk.CHUNKSIZEX * SubChunk.CHUNKSIZEY;
                    if (this.objects[objectIndex] == null) continue;
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
                    int objectIndex = x + y * SubChunk.CHUNKSIZEX + z * SubChunk.CHUNKSIZEX * SubChunk.CHUNKSIZEY;
                    if (this.objects[objectIndex] == null) continue;
                    this.mesh.addObjectFaceVertexToArray(this.objects[objectIndex].mesh.getFaceVertex(Directions.WEST), x, y, z);
                    ligneWest &= ~(1 << (31 - x));
                }
            }
        }

        // Nord Sud
        for (int y = 0; y < SubChunk.CHUNKSIZEY; y++){
            for (int x = 0; x < SubChunk.CHUNKSIZEX; x++){
                int index = y + x * SubChunk.CHUNKSIZEY;                

                int ligneNorth = this.northFace[index];
                while (ligneNorth != 0){
                    int z = Integer.numberOfLeadingZeros(ligneNorth);
                    int objectIndex = x + y * SubChunk.CHUNKSIZEX + z * SubChunk.CHUNKSIZEX * SubChunk.CHUNKSIZEY;
                    if (this.objects[objectIndex] == null) continue;
                    this.mesh.addObjectFaceVertexToArray(this.objects[objectIndex].mesh.getFaceVertex(Directions.NORTH), x, y, z);
                    ligneNorth &= ~(1 << (31 - z));
                }

                int ligneSouth = this.southFace[index];
                while (ligneSouth != 0){
                    int z = Integer.numberOfLeadingZeros(ligneSouth);
                    int objectIndex = x + y * SubChunk.CHUNKSIZEX + z * SubChunk.CHUNKSIZEX * SubChunk.CHUNKSIZEY;
                    if (this.objects[objectIndex] == null) continue;
                    this.mesh.addObjectFaceVertexToArray(this.objects[objectIndex].mesh.getFaceVertex(Directions.SOUTH), x, y, z);
                    ligneSouth &= ~(1 << (31 - z));
                }
            }
        }

        // Haut bas
        for (int z = 0; z < SubChunk.CHUNKSIZEZ; z++){
            for (int x = 0; x < SubChunk.CHUNKSIZEX; x++){
                int index = z + x * SubChunk.CHUNKSIZEZ;        

                int ligneUp = this.upFace[index];
                while (ligneUp != 0){
                    int y = Integer.numberOfLeadingZeros(ligneUp);
                    int objectIndex = x + y * SubChunk.CHUNKSIZEX + z * SubChunk.CHUNKSIZEX * SubChunk.CHUNKSIZEY;
                    if (this.objects[objectIndex] == null) continue;
                    this.mesh.addObjectFaceVertexToArray(this.objects[objectIndex].mesh.getFaceVertex(Directions.UP), x, y, z);
                    ligneUp &= ~(1 << (31 - y));
                }

                int ligneDown = this.downFace[index];
                while (ligneDown != 0){
                    int y = Integer.numberOfLeadingZeros(ligneDown);
                    int objectIndex = x + y * SubChunk.CHUNKSIZEX + z * SubChunk.CHUNKSIZEX * SubChunk.CHUNKSIZEY;
                    if (this.objects[objectIndex] == null) continue;
                    this.mesh.addObjectFaceVertexToArray(this.objects[objectIndex].mesh.getFaceVertex(Directions.DOWN), x, y, z);
                    ligneDown &= ~(1 << (31 - y));
                }
            }
        }
    }

    /**
     * Comme son nom l'indique
     */
    private void calculateSubChunkFaceCulling(){
        // Récup ouest est, on remplit l'int qui correspond a la ligne avec le BitSet du subChunk
        for (int y = 0; y < SubChunk.CHUNKSIZEY; y++){
            for (int z = 0; z < SubChunk.CHUNKSIZEZ; z++){
                int index = y + z * SubChunk.CHUNKSIZEY;      
                for (int x = 0; x < SubChunk.CHUNKSIZEX; x++){
                    int bitSetIndex = x + y * SubChunk.CHUNKSIZEX + z * SubChunk.CHUNKSIZEX * SubChunk.CHUNKSIZEY;                    
                    this.eastFace[index] |= (this.bitSet.get(bitSetIndex) ? 1 : 0) << (31 - x);                    
                }
                int ligne = this.eastFace[index];
                // le >> ajoute un 1 au début le >>> ajoute un 0
                this.eastFace[index] = ligne & ~(ligne >>> 1); // faces vers l'est
                this.westFace[index] = ligne & ~(ligne << 1); // faces vers l'ouest      
            }
        }
        // Récup nord sud
        for (int y = 0; y < SubChunk.CHUNKSIZEY; y++){
            for (int x = 0; x < SubChunk.CHUNKSIZEX; x++){
                int index = y + x * SubChunk.CHUNKSIZEY;
                for (int z = 0; z < SubChunk.CHUNKSIZEZ; z++){
                    int bitSetIndex = x + y * SubChunk.CHUNKSIZEX + z * SubChunk.CHUNKSIZEX * SubChunk.CHUNKSIZEY;
                    this.northFace[index] |= (this.bitSet.get(bitSetIndex) ? 1 : 0) << (31 - z);
                }
                int ligne = this.northFace[index];
                this.northFace[index] = ligne & ~(ligne << 1); // faces vers le nord
                this.southFace[index] = ligne & ~(ligne >>> 1); // faces vers le sud  
            }
        }

        // Récup haut bas
        for (int z = 0; z < SubChunk.CHUNKSIZEZ; z++){
            for (int x = 0; x < SubChunk.CHUNKSIZEX; x++){
                int index = z + x * SubChunk.CHUNKSIZEZ;
                for (int y = 0; y < SubChunk.CHUNKSIZEY; y++){
                    int bitSetIndex = x + y * SubChunk.CHUNKSIZEX + z * SubChunk.CHUNKSIZEX * SubChunk.CHUNKSIZEY;
                    this.upFace[index] |= (this.bitSet.get(bitSetIndex) ? 1 : 0) << (31 - y);
                }
                int ligne = this.upFace[index];
                this.upFace[index] = ligne & ~(ligne << 1); // faces vers le haut
                this.downFace[index] = ligne & ~(ligne >>> 1); // faces vers le bas  
            }
        }
    }

    /**
     * Retourne les objets du subChunk
     * @return
     */
    public Object3D[] getSubChunkObject(){
        return this.objects;
    }

    /**
     * Remplis le BitSet du subChunk (cube plein = 1, cube vide = 0)
     */
    @ChunkThread
    private void fillSubChunkBitSet(){
        for(int i = 0; i < SubChunk.MAXBLOCK; i++){
            if (this.objects[i] != null){
                this.bitSet.set(i); // met a 1 l'indice i
            }
        }
    }

    /**
     * Dessine la maille du subChunk
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
