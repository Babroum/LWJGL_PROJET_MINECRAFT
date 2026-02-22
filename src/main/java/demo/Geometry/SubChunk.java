package demo.Geometry;

import java.util.BitSet;

import org.joml.Matrix4f;

import demo.Annotations.ChunkThread;
import demo.Annotations.OpenGLThread;
import demo.Enum.BlocksType;
import demo.Enum.Directions;
import demo.Exceptions.IncorrectCoordinates;
import demo.Managers.ChunksManager;
import demo.Managers.ShadersManager;
import demo.Meshing.SubChunkMesh;

public class SubChunk extends Node3D{

    public static final int CHUNKSIZEX = 16;
    public static final int CHUNKSIZEY = 16;
    public static final int CHUNKSIZEZ = 16;
    public static final int MAXBLOCK = 4096;

    // Lignes de blocs pleins/vides sur l'axe (ex: ligne X = 16 bits, 1 = bloc plein, 0 = bloc vide)
    // Utilisé pour calculer quelles faces sont visibles (face culling)
    private int[] northFaceAxis = new int[SubChunk.CHUNKSIZEY * SubChunk.CHUNKSIZEZ]; 
    private int[] southFaceAxis = new int[SubChunk.CHUNKSIZEY * SubChunk.CHUNKSIZEZ]; 
    private int[] westFaceAxis  = new int[SubChunk.CHUNKSIZEY * SubChunk.CHUNKSIZEX]; 
    private int[] eastFaceAxis  = new int[SubChunk.CHUNKSIZEY * SubChunk.CHUNKSIZEX]; 
    private int[] upFaceAxis    = new int[SubChunk.CHUNKSIZEZ * SubChunk.CHUNKSIZEX]; 
    private int[] downFaceAxis  = new int[SubChunk.CHUNKSIZEZ * SubChunk.CHUNKSIZEX]; 

    // Lignes de faces visibles perpendiculaires à la direction (ex: ligne X = 16 bits, 1 = face visible ici)
    // Utilisé pour fusionner les faces adjacentes (binary greedy meshing)
    private int[] northFaceMask = new int[SubChunk.CHUNKSIZEY * SubChunk.CHUNKSIZEZ]; 
    private int[] southFaceMask = new int[SubChunk.CHUNKSIZEY * SubChunk.CHUNKSIZEZ]; 
    private int[] westFaceMask  = new int[SubChunk.CHUNKSIZEY * SubChunk.CHUNKSIZEX]; 
    private int[] eastFaceMask  = new int[SubChunk.CHUNKSIZEY * SubChunk.CHUNKSIZEX]; 
    private int[] upFaceMask    = new int[SubChunk.CHUNKSIZEZ * SubChunk.CHUNKSIZEX]; 
    private int[] downFaceMask  = new int[SubChunk.CHUNKSIZEZ * SubChunk.CHUNKSIZEX]; 

    // donne au mesh le subChunk au quel il appartient
    public SubChunkMesh mesh = new SubChunkMesh();
    private Object3D[] objects;
    public String name;

    // contient des 1 et 0 (block plein, block vide(air))
    public BitSet bitSet = new BitSet(SubChunk.MAXBLOCK); 

    @ChunkThread
    public SubChunk(){
        this.objects = new Object3D[SubChunk.MAXBLOCK];     
        // définit la taille du buffer et du tableau de vertex  
        this.mesh.setupSubChunkBuffers();   
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
        // calcule le face culling
        this.calculateSubChunkFaceCulling();
        // calcule le greedy meshing
        this.calculateSubChunkBinaryGreedyMeshing();
        // envoie la maille des objets à la maille
        this.uploadSubChunkMesh();
        // rempli le buffer de la maille avec les données pack (vertex packing)
        this.mesh.fillSubChunkVertexBufferWithArray();
    }

    /**
     * Remplis le vbo avec les données des objets avant le rendu
     */
    @OpenGLThread
    public void setupSubChunkVBOVAO(){
        // créer les vbo vao avec le buffer rempli
        this.mesh.createSubChunkVBOVAO();
        this.mesh.createSubChunkBorderVBOVAO();
    }

    @OpenGLThread
    public void removeSubChunk(){
        this.mesh.deleteSubChunkVBOVAO();
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
     * Envoie les faces à dessiné calculé avec le face culling de la maille
     */
    @ChunkThread
    private void uploadSubChunkMesh(){
        // Ouest Est
        for (int y = 0; y < SubChunk.CHUNKSIZEY; y++){
            for (int z = 0; z < SubChunk.CHUNKSIZEZ; z++){
                int index = y + z * SubChunk.CHUNKSIZEY;                

                int ligneEast = this.eastFaceAxis[index];
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

                int ligneWest = this.westFaceAxis[index];
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

                int ligneSouth = this.southFaceAxis[index];
                while (ligneSouth != 0){
                    int z = Integer.numberOfLeadingZeros(ligneSouth);
                    int objectIndex = x + y * SubChunk.CHUNKSIZEX + z * SubChunk.CHUNKSIZEX * SubChunk.CHUNKSIZEY;
                    if (this.objects[objectIndex] == null) continue;
                    this.mesh.addObjectFaceVertexToArray(this.objects[objectIndex].mesh.getFaceVertex(Directions.SOUTH), x, y, z);
                    ligneSouth &= ~(1 << (31 - z));
                }

                int ligneNorth = this.northFaceAxis[index];
                while (ligneNorth != 0){
                    int z = Integer.numberOfLeadingZeros(ligneNorth);
                    int objectIndex = x + y * SubChunk.CHUNKSIZEX + z * SubChunk.CHUNKSIZEX * SubChunk.CHUNKSIZEY;
                    if (this.objects[objectIndex] == null) continue;
                    this.mesh.addObjectFaceVertexToArray(this.objects[objectIndex].mesh.getFaceVertex(Directions.NORTH), x, y, z);
                    ligneNorth &= ~(1 << (31 - z));
                }
            }
        }

        // Haut bas
        for (int z = 0; z < SubChunk.CHUNKSIZEZ; z++){
            for (int x = 0; x < SubChunk.CHUNKSIZEX; x++){
                int index = z + x * SubChunk.CHUNKSIZEZ;        

                int ligneUp = this.upFaceAxis[index];
                while (ligneUp != 0){
                    int y = Integer.numberOfLeadingZeros(ligneUp);
                    int objectIndex = x + y * SubChunk.CHUNKSIZEX + z * SubChunk.CHUNKSIZEX * SubChunk.CHUNKSIZEY;
                    if (this.objects[objectIndex] == null) continue;
                    this.mesh.addObjectFaceVertexToArray(this.objects[objectIndex].mesh.getFaceVertex(Directions.UP), x, y, z);
                    ligneUp &= ~(1 << (31 - y));
                }

                int ligneDown = this.downFaceAxis[index];
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
     * Calcule aussi avec les faces des chunks voisins
     */
    private void calculateSubChunkFaceCulling(){
        // Récup ouest est, on remplit l'int qui correspond a la ligne avec le BitSet du subChunk
        for (int y = 0; y < SubChunk.CHUNKSIZEY; y++){
            for (int z = 0; z < SubChunk.CHUNKSIZEZ; z++){
                int index = y + z * SubChunk.CHUNKSIZEY;      
                for (int x = 0; x < SubChunk.CHUNKSIZEX; x++){
                    int bitSetIndex = x + y * SubChunk.CHUNKSIZEX + z * SubChunk.CHUNKSIZEX * SubChunk.CHUNKSIZEY;                    
                    this.eastFaceAxis[index] |= (this.bitSet.get(bitSetIndex) ? 1 : 0) << (31 - x);                    
                }
                int ligne = this.eastFaceAxis[index];
                
                // Est : récupère les bits les plus à droite du voisin est (x = 15)
                int ligneNeighborEast = 0;
                // si il y a un voisin
                if (this.getNeighborEast() != null){
                    int bitSetIndex = 15 + y * SubChunk.CHUNKSIZEX + z * SubChunk.CHUNKSIZEX * SubChunk.CHUNKSIZEY;
                    // on récupère la valeur du bitSet à l'index
                    // met le dernier bit a 1
                    ligneNeighborEast = (this.getNeighborEast().bitSet.get(bitSetIndex) ? 1 : 0);
                }                                 
                // si on récup le bit le plus a droite du voisin il faut le mettre le plus a gauche pour le subChunk actuel
                ligneNeighborEast = ligneNeighborEast << 31;

                int ligneNeighborWest = 0;
                if (this.getNeighborWest() != null){
                    int bitSetIndex = 0 + y * SubChunk.CHUNKSIZEX + z * SubChunk.CHUNKSIZEX * SubChunk.CHUNKSIZEY;
                    ligneNeighborWest = (this.getNeighborWest().bitSet.get(bitSetIndex) ? 1 : 0);
                }   
                // si on récup le bit le plus a droite du voisin il faut le mettre le plus a droite par rapport a la ligne donc au milieu de l'int pour le subChunk actuel
                ligneNeighborWest = ligneNeighborWest << 16;

                // le >> ajoute un 1 au début le >>> ajoute un 0
                this.eastFaceAxis[index] = ligne & ~((ligne >>> 1) | ligneNeighborEast); // faces vers l'est + le bloc voisin
                this.westFaceAxis[index] = ligne & ~((ligne << 1) | ligneNeighborWest); // faces vers l'ouest + le bloc voisin     
            }
        }
        // Récup nord sud
        for (int y = 0; y < SubChunk.CHUNKSIZEY; y++){
            for (int x = 0; x < SubChunk.CHUNKSIZEX; x++){
                int index = y + x * SubChunk.CHUNKSIZEY;
                for (int z = 0; z < SubChunk.CHUNKSIZEZ; z++){
                    int bitSetIndex = x + y * SubChunk.CHUNKSIZEX + z * SubChunk.CHUNKSIZEX * SubChunk.CHUNKSIZEY;
                    this.northFaceAxis[index] |= (this.bitSet.get(bitSetIndex) ? 1 : 0) << (31 - z);
                }
                int ligne = this.northFaceAxis[index];

                int ligneNeighborSouth = 0;
                if (this.getNeighborSouth() != null){
                    int bitSetIndex = x + y * SubChunk.CHUNKSIZEX + 15 * SubChunk.CHUNKSIZEX * SubChunk.CHUNKSIZEY;
                    ligneNeighborSouth = (this.getNeighborSouth().bitSet.get(bitSetIndex) ? 1 : 0);
                }
                ligneNeighborSouth = ligneNeighborSouth << 16;     

                int ligneNeighborNorth = 0;
                if (this.getNeighborNorth() != null){
                    int bitSetIndex = x + y * SubChunk.CHUNKSIZEX + 0 * SubChunk.CHUNKSIZEX * SubChunk.CHUNKSIZEY;
                    ligneNeighborNorth = (this.getNeighborNorth().bitSet.get(bitSetIndex) ? 1 : 0);
                }
                ligneNeighborNorth = ligneNeighborNorth << 31;  

                this.southFaceAxis[index] = ligne & ~((ligne << 1) | ligneNeighborSouth); // faces vers le sud  
                this.northFaceAxis[index] = ligne & ~((ligne >>> 1) | ligneNeighborNorth); // faces vers le nord
            }
        }
        // Récup haut bas
        for (int z = 0; z < SubChunk.CHUNKSIZEZ; z++){
            for (int x = 0; x < SubChunk.CHUNKSIZEX; x++){
                int index = z + x * SubChunk.CHUNKSIZEZ;
                for (int y = 0; y < SubChunk.CHUNKSIZEY; y++){
                    int bitSetIndex = x + y * SubChunk.CHUNKSIZEX + z * SubChunk.CHUNKSIZEX * SubChunk.CHUNKSIZEY;
                    this.upFaceAxis[index] |= (this.bitSet.get(bitSetIndex) ? 1 : 0) << (31 - y);
                }
                int ligne = this.upFaceAxis[index];
                int ligneNeighborUp = 0;
                if (this.getNeighborUp() != null){
                    int bitSetIndex = x + 0 * SubChunk.CHUNKSIZEX + z * SubChunk.CHUNKSIZEX * SubChunk.CHUNKSIZEY;
                    ligneNeighborUp = (this.getNeighborUp().bitSet.get(bitSetIndex) ? 1 : 0);
                }
                ligneNeighborUp = ligneNeighborUp << 16;
                int ligneNeighborDown = 0;
                if (this.getNeighborDown() != null){
                    int bitSetIndex = x + 15 * SubChunk.CHUNKSIZEX + z * SubChunk.CHUNKSIZEX * SubChunk.CHUNKSIZEY;
                    ligneNeighborDown = (this.getNeighborDown().bitSet.get(bitSetIndex) ? 1 : 0);
                }
                ligneNeighborDown = ligneNeighborDown << 31;
                this.upFaceAxis[index] = ligne & ~((ligne << 1) | ligneNeighborUp); // faces vers le haut
                this.downFaceAxis[index] = ligne & ~((ligne >>> 1) | ligneNeighborDown); // faces vers le bas  
            }
        }
    }

    /**
     * Peut pas faire plus simple comme nom
     */
    private void calculateSubChunkBinaryGreedyMeshing(){
        // rempli les masques avec les infos du face culling
        for (int y = 0; y < SubChunk.CHUNKSIZEY; y++){
            for (int z = 0; z < SubChunk.CHUNKSIZEZ; z++){                
                for (int x = 0; x < SubChunk.CHUNKSIZEX; x++){
                    int westBit = (this.westFaceAxis[y + z * SubChunk.CHUNKSIZEY] >> (16 + x)) & 1;
                    this.westFaceMask[y + x * SubChunk.CHUNKSIZEX] |= (westBit << (31 - z));  
                    int eastBit = (this.eastFaceAxis[y + z * SubChunk.CHUNKSIZEY] >> (31 - x)) & 1;
                    this.eastFaceMask[y + x * SubChunk.CHUNKSIZEX] |= (eastBit << (31 - z));
                }
            }
        }        
        for (int y = 0; y < SubChunk.CHUNKSIZEY; y++){
            for (int x = 0; x < SubChunk.CHUNKSIZEX; x++){
                for (int z = 0; z < SubChunk.CHUNKSIZEZ; z++){
                    // En gros je récup tous les bits d'un int d'Axis, que je met en 1er position des int de mask a chaque boucle
                    int northBit = (this.northFaceAxis[y + x * SubChunk.CHUNKSIZEY] >> (16 + z)) & 1; // décalle de 16 bits + z vers la droite puis récupère le 1er bit
                    if (northBit == 1) System.out.println("north bit placé à z=" + z + " x=" + x + " y=" + y);
                    this.northFaceMask[y + z * SubChunk.CHUNKSIZEZ] |= (northBit << (31 - x));
                    int southBit = (this.southFaceAxis[y + x * SubChunk.CHUNKSIZEY] >> (31 - z)) & 1; // décalle de 31 bits - z vers la droite puis récupère le 1er bit*
                    if (southBit == 1) System.out.println("south bit placé à z=" + z + " x=" + x + " y=" + y);
                    this.southFaceMask[y + z * SubChunk.CHUNKSIZEZ] |= (southBit << (31 - x));

                }
            }
        }
        for (int z = 0; z < SubChunk.CHUNKSIZEZ; z++){
            for (int x = 0; x < SubChunk.CHUNKSIZEX; x++){
                for (int y = 0; y < SubChunk.CHUNKSIZEY; y++){
                    int upBit = (this.upFaceAxis[z + x * SubChunk.CHUNKSIZEZ] >> (16 + y)) & 1;
                    this.upFaceMask[z + y * SubChunk.CHUNKSIZEY] |= (upBit << (31 - x));
                    int downBit = (this.downFaceAxis[z + x * SubChunk.CHUNKSIZEZ] >> (31 - y)) & 1;
                    this.downFaceMask[z + y * SubChunk.CHUNKSIZEY] |= (downBit << (31 - x));
                }
            }
        }

        int northCount = 0, southCount = 0, westCount = 0, eastCount = 0, upCount = 0, downCount = 0;
        for (int i = 0; i < northFaceMask.length; i++) northCount += Integer.bitCount(northFaceMask[i]);
        for (int i = 0; i < southFaceMask.length; i++) southCount += Integer.bitCount(southFaceMask[i]);
        for (int i = 0; i < westFaceMask.length; i++)  westCount  += Integer.bitCount(westFaceMask[i]);
        for (int i = 0; i < eastFaceMask.length; i++)  eastCount  += Integer.bitCount(eastFaceMask[i]);
        for (int i = 0; i < upFaceMask.length; i++)    upCount    += Integer.bitCount(upFaceMask[i]);
        for (int i = 0; i < downFaceMask.length; i++)  downCount  += Integer.bitCount(downFaceMask[i]);

        if ((northCount > 0 || southCount > 0 || westCount > 0 || eastCount > 0 || upCount > 0 || downCount > 0) && false){
            System.out.println("Greedy : ============coord : " + this.name + " =====");
            System.out.println("north : " + northCount + " south : " + southCount);
            System.out.println("west : "  + westCount  + " east : "  + eastCount);
            System.out.println("up : "    + upCount    + " down : "  + downCount);
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
    public void fillSubChunkBitSet(){
        this.bitSet.clear();
        for(int i = 0; i < SubChunk.MAXBLOCK; i++){
            if (this.objects[i] != null){
                this.bitSet.set(i); // met a 1 l'indice i
            }
        }
    }
    
    /**
     * Retourne le subChunk voisin Nord
     * @return SubChunk
     */
    public SubChunk getNeighborNorth(){
        try{
            Chunk chunk = ChunksManager.getChunk(this.x / SubChunk.CHUNKSIZEX, (this.z / SubChunk.CHUNKSIZEZ) - 1);
            return chunk.getSubChunk(this.y / SubChunk.CHUNKSIZEY);
        }catch (IncorrectCoordinates e){
            return null;
        }        
    }
    /**
     * Retourne le subChunk voisin Sud
     * @return SubChunk
     */
    public SubChunk getNeighborSouth(){
        try{
            Chunk chunk = ChunksManager.getChunk(this.x / SubChunk.CHUNKSIZEX, (this.z / SubChunk.CHUNKSIZEZ) + 1);
            return chunk.getSubChunk(this.y / SubChunk.CHUNKSIZEY);
        }catch(IncorrectCoordinates e){
            return null;
        }    
    }
    /**
     * Retourne le subChunk voisin Ouest
     * @return SubChunk
     */
    public SubChunk getNeighborWest(){
        try{
            Chunk chunk = ChunksManager.getChunk((this.x / SubChunk.CHUNKSIZEX) + 1, this.z / SubChunk.CHUNKSIZEZ);
            return chunk.getSubChunk(this.y / SubChunk.CHUNKSIZEY);
        }catch(IncorrectCoordinates e){
            return null;
        }    
    }
    /**
     * Retourne le subChunk voisin Est
     * @return SubChunk
     */
    public SubChunk getNeighborEast(){
        try{
            Chunk chunk = ChunksManager.getChunk((this.x / SubChunk.CHUNKSIZEX) - 1, this.z / SubChunk.CHUNKSIZEZ);
            return chunk.getSubChunk(this.y / SubChunk.CHUNKSIZEY);
        }catch(IncorrectCoordinates e){
            return null;
        }    
    }
    /**
     * Retourne le subChunk voisin Haut
     * @return SubChunk
     */
    public SubChunk getNeighborUp(){
        try{
            Chunk chunk = ChunksManager.getChunk(this.x / SubChunk.CHUNKSIZEX, this.z / SubChunk.CHUNKSIZEZ);
            return chunk.getSubChunk((this.y / SubChunk.CHUNKSIZEY) + 1);
        }catch(IncorrectCoordinates e){
            return null;
        }    
    }
    /**
     * Retourne le subChunk voisin Bas
     * @return SubChunk
     */
    public SubChunk getNeighborDown(){
        try{
            Chunk chunk = ChunksManager.getChunk(this.x / SubChunk.CHUNKSIZEX, this.z / SubChunk.CHUNKSIZEZ);
            return chunk.getSubChunk((this.y / SubChunk.CHUNKSIZEY) - 1);
        }catch(IncorrectCoordinates e){
            return null;
        }    
    }

    /**
     * Dessine la maille du subChunk
     */
    @OpenGLThread
    @Override
    public void drawSpecific(){
        this.mesh.drawSubChunkMesh(this.buffer);
    }

    @Override
    public void drawNode(Matrix4f parentModel){
        Matrix4f model = new Matrix4f(parentModel)
            .translate(this.x, this.y, this.z);
        model.get(this.buffer);
        ShadersManager.runModel(this.buffer);
        this.drawSpecific();
        // pas d'itération sur les cubes enfants
    }
}
