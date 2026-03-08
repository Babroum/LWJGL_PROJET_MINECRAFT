package demo.Geometry;

import java.util.BitSet;

import org.joml.Matrix4f;

import demo.Annotations.ChunkThread;
import demo.Annotations.OpenGLThread;
import demo.Enum.BlocksType;
import demo.Enum.Directions;
import demo.Events.KeyboardInput;
import demo.Exceptions.IncorrectCoordinates;
import demo.Managers.ChunksManager;
import demo.Managers.ShadersManager;
import demo.Meshing.SubChunkMesh;
import demo.Rendering.Render;

public class SubChunk extends Node3D{

    public static final int CHUNKSIZEX = 16;
    public static final int CHUNKSIZEY = 16;
    public static final int CHUNKSIZEZ = 16;
    public static final int MAXBLOCK = 4096;    

    private final Matrix4f model = new Matrix4f();

    private SubChunk westNeighbor;
    private SubChunk eastNeighbor;
    private SubChunk northNeighbor;
    private SubChunk southNeighbor;
    private SubChunk upNeighbor;
    private SubChunk downNeighbor;

    // donne au mesh le subChunk au quel il appartient
    public SubChunkMesh mesh = new SubChunkMesh();
    public byte[] blocks;
    public String name;

    private boolean isGenerating = false;

    // contient des 1 et 0 (block plein, block vide(air))
    public BitSet bitSet = new BitSet(SubChunk.MAXBLOCK); 

    @ChunkThread
    public SubChunk(){
        this.blocks = new byte[SubChunk.MAXBLOCK];     
        // définit la taille du buffer et du tableau de vertex  
        this.mesh.setupSubChunkBuffers(this.bitSet.cardinality());   
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
        // Lignes de blocs pleins/vides sur l'axe (ex: ligne X = 16 bits, 1 = bloc plein, 0 = bloc vide)
        // Utilisé pour calculer quelles faces sont visibles (face culling)
        // les tableaux sont créer seulement pour le face culling puis après détruit par le GC 
        // car il ne servent plus à rien tant que le chunk n'est pas rebuils
        int[] northFaceAxis = new int[SubChunk.CHUNKSIZEY * SubChunk.CHUNKSIZEZ];
        int[] southFaceAxis = new int[SubChunk.CHUNKSIZEY * SubChunk.CHUNKSIZEZ];
        int[] eastFaceAxis  = new int[SubChunk.CHUNKSIZEY * SubChunk.CHUNKSIZEX];
        int[] westFaceAxis  = new int[SubChunk.CHUNKSIZEY * SubChunk.CHUNKSIZEX];
        int[] upFaceAxis    = new int[SubChunk.CHUNKSIZEZ * SubChunk.CHUNKSIZEX];
        int[] downFaceAxis  = new int[SubChunk.CHUNKSIZEZ * SubChunk.CHUNKSIZEX];
        // calcule le face culling
        this.calculateSubChunkFaceCulling(northFaceAxis, southFaceAxis, eastFaceAxis, westFaceAxis, upFaceAxis, downFaceAxis);
        // envoie la maille des objets à la maille
        this.uploadSubChunkMesh(northFaceAxis, southFaceAxis, eastFaceAxis, westFaceAxis, upFaceAxis, downFaceAxis);
        // rempli le buffer de la maille avec les données pack (vertex packing)
        this.mesh.fillSubChunkVertexBufferWithArray();
    }

    /**
     * Retourne le block à la position
     * @param x int
     * @param y int
     * @param z int
     * @return BlockType
     */
    public BlocksType getBlock(int x, int y, int z){
        if (x < 0 || x >= SubChunk.CHUNKSIZEX ||
            y < 0 || y >= SubChunk.CHUNKSIZEY ||
            z < 0 || z >= SubChunk.CHUNKSIZEZ
        ){throw new IncorrectCoordinates("Coordonées dans le subChunk invalide");}
        int index = x + y * SubChunk.CHUNKSIZEX + z * SubChunk.CHUNKSIZEX * SubChunk.CHUNKSIZEY;
        return BlocksType.fromId(this.blocks[index]);
    }

    /**
     * Retourne tout les blocks du sous chunk
     * @return byte[]
     */
    public byte[] getAllBlocks(){
        return this.blocks;
    }
   

    /**
     * Remplis le vbo avec les données des objets avant le rendu
     */
    @OpenGLThread
    public void setupSubChunkVBOVAO(){
        // créer les vbo vao avec le buffer rempli
        this.mesh.createSubChunkVBOVAO();
        if (KeyboardInput.isChunkBorderShow && this.mesh.getBorderVAO() == 0 && this.mesh.getVertexCount() > 0){
            this.mesh.createSubChunkBorderVBOVAO();
        }
    }

    /**
     * Supprime le subChunk
     */
    @OpenGLThread
    public void removeSubChunk(){
        // Libère le buffer CPU off-heap s'il n'a pas encore été uploadé
        this.mesh.cleanup();
        // Libère les buffers GPU
        this.mesh.deleteSubChunkVBOVAO();
        this.cleanup();
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

        this.isGenerating = true;
        for(int y = Y; y < Y + nbFloor; y++){
            for(int x = 0; x < SubChunk.CHUNKSIZEX; x++){
                for (int z = 0; z < SubChunk.CHUNKSIZEZ; z++){
                    this.addBlockToSubChunk(type, x, y, z);          
                }    
            }
        }  
        this.isGenerating = false;
        this.rebuildSubChunk();
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
        this.blocks[index] = (byte) type.getId();
        // Ne rebuild pas pendant la génération initiale
        if (!this.isGenerating){
            this.rebuildSubChunk();
        }
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
        this.blocks[index] = (byte)BlocksType.AIR.getId();
        this.fillSubChunkBitSet();

        // rebuild les voisins si le bloc est en bordure
        if (x == SubChunk.CHUNKSIZEX - 1 && this.getNeighborWest() != null) { 
            this.getNeighborWest().rebuildSubChunk(); 
        }
        if (x == 0 && this.getNeighborEast() != null) { 
            this.getNeighborEast().rebuildSubChunk(); 
        }
        if (z == SubChunk.CHUNKSIZEZ - 1 && this.getNeighborNorth() != null) { 
            this.getNeighborNorth().rebuildSubChunk(); 
        }
        if (z == 0 && this.getNeighborSouth() != null) { 
            this.getNeighborSouth().rebuildSubChunk(); 
        }
        if (y == 0 && this.getNeighborDown() != null) { 
            this.getNeighborDown().rebuildSubChunk(); 
        }
        if (y == SubChunk.CHUNKSIZEY - 1 && this.getNeighborUp() != null) { 
            this.getNeighborUp().rebuildSubChunk(); 
        }
        this.rebuildSubChunk();
    }

    @ChunkThread
    private void rebuildSubChunk(){
        this.fillSubChunkBitSet();
        synchronized(this.mesh){
            this.mesh.setupSubChunkBuffers(this.bitSet.cardinality());
            this.generateSubChunkMesh();
            this.linkNeighbors();
        }
        // met le thread en pause si l'array est plein, le thread se relance automatiquement au moment ou OpenGLThread poll un chunk
        try { Render.dirtySubChunks.put(this); } catch(InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    /**
     * Envoie les quads à dessiné avec le face cullings
     */
    @ChunkThread
    private void uploadSubChunkMesh(int[] northFaceAxis, int[] southFaceAxis,
                                    int[] eastFaceAxis,  int[] westFaceAxis,
                                    int[] upFaceAxis,    int[] downFaceAxis){
        // Ouest Est
        for (int y = 0; y < SubChunk.CHUNKSIZEY; y++){
            for (int z = 0; z < SubChunk.CHUNKSIZEZ; z++){
                int index = y + z * SubChunk.CHUNKSIZEY;                

                int ligneEast = eastFaceAxis[index];
                while (ligneEast != 0){
                    // compte les 0 depuis la gauche, donc donne la position de la face à dessiner
                    int x = Integer.numberOfLeadingZeros(ligneEast);
                    // calcule la position de l'objet
                    int objectIndex = x + y * SubChunk.CHUNKSIZEX + z * SubChunk.CHUNKSIZEX * SubChunk.CHUNKSIZEY;
                    if (this.blocks[objectIndex] == (byte)BlocksType.AIR.getId()) continue;
                    BlocksType type = BlocksType.fromId(this.blocks[objectIndex]);
                    this.mesh.addObjectFaceVertexToArray(
                        type.getCube().mesh.getFaceVertex(Directions.EAST), x, y, z);
                    // (1 << (31 - x)) crée un masque avec un 1 en position x
                    // ~ inverse le masque donc la face dessiner devient un 0 et le reste 1
                    // & fait AND donc applique le masque sur la ligne donc tout reste intact sauf la face dessiné
                    ligneEast &= ~(1 << (31 - x)); // efface le bit
                    // on recommence temps que la ligne n'est pas vide
                }

                int ligneWest = westFaceAxis[index];
                while (ligneWest != 0){
                    int x = Integer.numberOfLeadingZeros(ligneWest);
                    int objectIndex = x + y * SubChunk.CHUNKSIZEX + z * SubChunk.CHUNKSIZEX * SubChunk.CHUNKSIZEY;
                    if (this.blocks[objectIndex] == (byte)BlocksType.AIR.getId()) continue;
                    BlocksType type = BlocksType.fromId(this.blocks[objectIndex]);
                    this.mesh.addObjectFaceVertexToArray(
                        type.getCube().mesh.getFaceVertex(Directions.WEST), x, y, z);
                    ligneWest &= ~(1 << (31 - x));
                }
            }
        }

        // Nord Sud
        for (int y = 0; y < SubChunk.CHUNKSIZEY; y++){
            for (int x = 0; x < SubChunk.CHUNKSIZEX; x++){
                int index = y + x * SubChunk.CHUNKSIZEY;                

                int ligneSouth = southFaceAxis[index];
                while (ligneSouth != 0){
                    int z = Integer.numberOfLeadingZeros(ligneSouth);
                    int objectIndex = x + y * SubChunk.CHUNKSIZEX + z * SubChunk.CHUNKSIZEX * SubChunk.CHUNKSIZEY;
                    if (this.blocks[objectIndex] == (byte)BlocksType.AIR.getId()) continue;
                    BlocksType type = BlocksType.fromId(this.blocks[objectIndex]);
                    this.mesh.addObjectFaceVertexToArray(
                        type.getCube().mesh.getFaceVertex(Directions.SOUTH), x, y, z);
                    ligneSouth &= ~(1 << (31 - z));
                }

                int ligneNorth = northFaceAxis[index];
                while (ligneNorth != 0){
                    int z = Integer.numberOfLeadingZeros(ligneNorth);
                    int objectIndex = x + y * SubChunk.CHUNKSIZEX + z * SubChunk.CHUNKSIZEX * SubChunk.CHUNKSIZEY;
                    if (this.blocks[objectIndex] == (byte)BlocksType.AIR.getId()) continue;
                    BlocksType type = BlocksType.fromId(this.blocks[objectIndex]);
                    this.mesh.addObjectFaceVertexToArray(
                        type.getCube().mesh.getFaceVertex(Directions.NORTH), x, y, z);
                    ligneNorth &= ~(1 << (31 - z));
                }
            }
        }

        // Haut bas
        for (int z = 0; z < SubChunk.CHUNKSIZEZ; z++){
            for (int x = 0; x < SubChunk.CHUNKSIZEX; x++){
                int index = z + x * SubChunk.CHUNKSIZEZ;        

                int ligneDown = downFaceAxis[index];
                while (ligneDown != 0){
                    int y = Integer.numberOfLeadingZeros(ligneDown);
                    int objectIndex = x + y * SubChunk.CHUNKSIZEX + z * SubChunk.CHUNKSIZEX * SubChunk.CHUNKSIZEY;
                    if (this.blocks[objectIndex] == (byte)BlocksType.AIR.getId()) continue;
                    BlocksType type = BlocksType.fromId(this.blocks[objectIndex]);
                    this.mesh.addObjectFaceVertexToArray(
                        type.getCube().mesh.getFaceVertex(Directions.DOWN), x, y, z);
                    ligneDown &= ~(1 << (31 - y));
                }

                int ligneUp = upFaceAxis[index];
                while (ligneUp != 0){
                    int y = Integer.numberOfLeadingZeros(ligneUp);
                    int objectIndex = x + y * SubChunk.CHUNKSIZEX + z * SubChunk.CHUNKSIZEX * SubChunk.CHUNKSIZEY;
                    if (this.blocks[objectIndex] == (byte)BlocksType.AIR.getId()) continue;
                    BlocksType type = BlocksType.fromId(this.blocks[objectIndex]);
                    this.mesh.addObjectFaceVertexToArray(
                        type.getCube().mesh.getFaceVertex(Directions.UP), x, y, z);
                    ligneUp &= ~(1 << (31 - y));
                }
            }
        }
    }

    /**
     * Comme son nom l'indique
     * Calcule aussi avec les faces des chunks voisins
     */
    @ChunkThread
    private void calculateSubChunkFaceCulling(int[] northFaceAxis, int[] southFaceAxis,
                                              int[] eastFaceAxis,  int[] westFaceAxis,
                                              int[] upFaceAxis,    int[] downFaceAxis){
        // Récup ouest est, on remplit l'int qui correspond a la ligne avec le BitSet du subChunk
        for (int y = 0; y < SubChunk.CHUNKSIZEY; y++){
            for (int z = 0; z < SubChunk.CHUNKSIZEZ; z++){
                int index = y + z * SubChunk.CHUNKSIZEY;      
                for (int x = 0; x < SubChunk.CHUNKSIZEX; x++){
                    int bitSetIndex = x + y * SubChunk.CHUNKSIZEX + z * SubChunk.CHUNKSIZEX * SubChunk.CHUNKSIZEY;                    
                    eastFaceAxis[index] |= (this.bitSet.get(bitSetIndex) ? 1 : 0) << (31 - x);                    
                }
                int ligne = eastFaceAxis[index];
                
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
                eastFaceAxis[index] = ligne & ~((ligne >>> 1) | ligneNeighborEast); // faces vers l'est + le bloc voisin
                westFaceAxis[index] = ligne & ~((ligne << 1) | ligneNeighborWest); // faces vers l'ouest + le bloc voisin     
            }
        }
        // Récup nord sud
        for (int y = 0; y < SubChunk.CHUNKSIZEY; y++){
            for (int x = 0; x < SubChunk.CHUNKSIZEX; x++){
                int index = y + x * SubChunk.CHUNKSIZEY;
                for (int z = 0; z < SubChunk.CHUNKSIZEZ; z++){
                    int bitSetIndex = x + y * SubChunk.CHUNKSIZEX + z * SubChunk.CHUNKSIZEX * SubChunk.CHUNKSIZEY;
                    northFaceAxis[index] |= (this.bitSet.get(bitSetIndex) ? 1 : 0) << (31 - z);
                }
                int ligne = northFaceAxis[index];

                int ligneNeighborSouth = 0;
                if (this.getNeighborSouth() != null){
                    int bitSetIndex = x + y * SubChunk.CHUNKSIZEX + 15 * SubChunk.CHUNKSIZEX * SubChunk.CHUNKSIZEY;
                    ligneNeighborSouth = (this.getNeighborSouth().bitSet.get(bitSetIndex) ? 1 : 0);
                }
                ligneNeighborSouth = ligneNeighborSouth << 31;     

                int ligneNeighborNorth = 0;
                if (this.getNeighborNorth() != null){
                    int bitSetIndex = x + y * SubChunk.CHUNKSIZEX + 0 * SubChunk.CHUNKSIZEX * SubChunk.CHUNKSIZEY;
                    ligneNeighborNorth = (this.getNeighborNorth().bitSet.get(bitSetIndex) ? 1 : 0);
                }
                ligneNeighborNorth = ligneNeighborNorth << 16;  

                southFaceAxis[index] = ligne & ~((ligne >>> 1) | ligneNeighborSouth); // faces vers le sud  
                northFaceAxis[index] = ligne & ~((ligne << 1) | ligneNeighborNorth); // faces vers le nord
            }
        }
        // Récup haut bas
        for (int z = 0; z < SubChunk.CHUNKSIZEZ; z++){
            for (int x = 0; x < SubChunk.CHUNKSIZEX; x++){
                int index = z + x * SubChunk.CHUNKSIZEZ;
                for (int y = 0; y < SubChunk.CHUNKSIZEY; y++){
                    int bitSetIndex = x + y * SubChunk.CHUNKSIZEX + z * SubChunk.CHUNKSIZEX * SubChunk.CHUNKSIZEY;
                    upFaceAxis[index] |= (this.bitSet.get(bitSetIndex) ? 1 : 0) << (31 - y);
                }
                int ligne = upFaceAxis[index];
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
                upFaceAxis[index] = ligne & ~((ligne << 1) | ligneNeighborUp); // faces vers le haut
                downFaceAxis[index] = ligne & ~((ligne >>> 1) | ligneNeighborDown); // faces vers le bas  
            }
        }
    }

    /**
     * Remplis le BitSet du subChunk (cube plein = 1, cube vide = 0)
     */
    @ChunkThread
    public void fillSubChunkBitSet(){
        this.bitSet.clear();
        for(int i = 0; i < SubChunk.MAXBLOCK; i++){
            if (this.blocks[i] != (byte)BlocksType.AIR.getId()){
                this.bitSet.set(i); // met a 1 l'indice i
            }
        }
    }
    
    /**
     * Récupère tout les voisins du subChunk
     */
    @ChunkThread
    public void linkNeighbors(){
        // ouest
        try{
            int chunkX = (int) Math.floor((float)this.x / SubChunk.CHUNKSIZEX);
            int chunkZ = (int) Math.floor((float)this.z / SubChunk.CHUNKSIZEZ);
            Chunk chunk = ChunksManager.getChunk(chunkX + 1, chunkZ);
            this.westNeighbor = chunk.getSubChunk(this.y / SubChunk.CHUNKSIZEY);
        }catch(IncorrectCoordinates e){
            this.westNeighbor = null;
        }  
        // est
        try{
            int chunkX = (int) Math.floor((float)this.x / SubChunk.CHUNKSIZEX);
            int chunkZ = (int) Math.floor((float)this.z / SubChunk.CHUNKSIZEZ);
            Chunk chunk = ChunksManager.getChunk(chunkX - 1, chunkZ);
            this.eastNeighbor = chunk.getSubChunk(this.y / SubChunk.CHUNKSIZEY);
        }catch(IncorrectCoordinates e){
            this.eastNeighbor = null;
        }   
        // nord
        try{
            int chunkX = (int) Math.floor((float)this.x / SubChunk.CHUNKSIZEX);
            int chunkZ = (int) Math.floor((float)this.z / SubChunk.CHUNKSIZEZ);
            Chunk chunk = ChunksManager.getChunk(chunkX, chunkZ + 1);
            this.northNeighbor = chunk.getSubChunk(this.y / SubChunk.CHUNKSIZEY);
        }catch(IncorrectCoordinates e){
            this.northNeighbor = null;
        }   
        // sud
        try{
            int chunkX = (int) Math.floor((float)this.x / SubChunk.CHUNKSIZEX);
            int chunkZ = (int) Math.floor((float)this.z / SubChunk.CHUNKSIZEZ);
            Chunk chunk = ChunksManager.getChunk(chunkX, chunkZ - 1);
            this.southNeighbor = chunk.getSubChunk(this.y / SubChunk.CHUNKSIZEY);
        }catch(IncorrectCoordinates e){
            this.southNeighbor = null;
        } 
        // haut
        try{
            int chunkX = (int) Math.floor((float)this.x / SubChunk.CHUNKSIZEX);
            int chunkZ = (int) Math.floor((float)this.z / SubChunk.CHUNKSIZEZ);
            Chunk chunk = ChunksManager.getChunk(chunkX, chunkZ);
            this.upNeighbor = chunk.getSubChunk((this.y / SubChunk.CHUNKSIZEY) + 1);
        }catch(IncorrectCoordinates e){
            this.upNeighbor = null;
        }  
        // bas
        try{
            int chunkX = (int) Math.floor((float)this.x / SubChunk.CHUNKSIZEX);
            int chunkZ = (int) Math.floor((float)this.z / SubChunk.CHUNKSIZEZ);
            Chunk chunk = ChunksManager.getChunk(chunkX, chunkZ);
            this.downNeighbor = chunk.getSubChunk((this.y / SubChunk.CHUNKSIZEY) - 1);
        }catch(IncorrectCoordinates e){
            this.downNeighbor = null;
        }  
    }

    /**
     * Retourne le subChunk voisin Ouest
     * @return SubChunk
     */
    public SubChunk getNeighborWest(){
        return this.westNeighbor;    
    }
    /**
     * Retourne le subChunk voisin Est
     * @return SubChunk
     */
    public SubChunk getNeighborEast(){
         return this.eastNeighbor;
    }
    /**
     * Retourne le subChunk voisin Nord
     * @return SubChunk
     */
    public SubChunk getNeighborNorth(){
        return this.northNeighbor;     
    }
    /**
     * Retourne le subChunk voisin Sud
     * @return SubChunk
     */
    public SubChunk getNeighborSouth(){
        return this.southNeighbor;   
    }
    /**
     * Retourne le subChunk voisin Haut
     * @return SubChunk
     */
    public SubChunk getNeighborUp(){
        return this.upNeighbor;  
    }
    /**
     * Retourne le subChunk voisin Bas
     * @return SubChunk
     */
    public SubChunk getNeighborDown(){
        return this.downNeighbor;  
    }

    public void setGenerating(boolean generating){
        this.isGenerating = generating;
    }

    /**
     * Dessine la maille du subChunk
     */
    @OpenGLThread
    @Override
    public void drawSpecific(){
        this.mesh.drawSubChunkMesh(this.buffer);
    }

    @ChunkThread
    @Override
    public void drawNode(Matrix4f parentModel){
        parentModel.translate(this.x, this.y, this.z, this.model);
        this.model.get(this.buffer);
        ShadersManager.runModel(this.buffer);
        this.drawSpecific();
        // pas d'itération sur les cubes enfants
    }
}
