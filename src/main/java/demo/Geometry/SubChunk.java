package demo.Geometry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

import org.joml.Matrix4f;

import demo.Annotations.ChunkThread;
import demo.Annotations.OpenGLThread;
import demo.Enum.BlocksType;
import demo.Enum.Directions;
import demo.Events.KeyboardInput;
import demo.Exceptions.IncorrectCoordinates;
import demo.Managers.ChunksManager;
import demo.Managers.ShadersManager;
import demo.Managers.TexturesManager;
import demo.Meshing.SubChunkMesh;
import demo.Meshing.Quad;
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
    public byte[] lightMap;

    private List<Quad> quads;

    private boolean isGenerating = false;

    // contient des 1 et 0 (block plein, block vide(air))
    public BitSet bitSet = new BitSet(SubChunk.MAXBLOCK); 

    private static class FaceData {
        int[] northAxis = new int[SubChunk.CHUNKSIZEY * SubChunk.CHUNKSIZEZ];
        int[] southAxis = new int[SubChunk.CHUNKSIZEY * SubChunk.CHUNKSIZEZ];
        int[] westAxis  = new int[SubChunk.CHUNKSIZEY * SubChunk.CHUNKSIZEX];
        int[] eastAxis  = new int[SubChunk.CHUNKSIZEY * SubChunk.CHUNKSIZEX];
        int[] upAxis    = new int[SubChunk.CHUNKSIZEZ * SubChunk.CHUNKSIZEX];
        int[] downAxis  = new int[SubChunk.CHUNKSIZEZ * SubChunk.CHUNKSIZEX];

        int[] northMask = new int[SubChunk.CHUNKSIZEY * SubChunk.CHUNKSIZEZ];
        int[] southMask = new int[SubChunk.CHUNKSIZEY * SubChunk.CHUNKSIZEZ];
        int[] westMask  = new int[SubChunk.CHUNKSIZEY * SubChunk.CHUNKSIZEX];
        int[] eastMask  = new int[SubChunk.CHUNKSIZEY * SubChunk.CHUNKSIZEX];
        int[] upMask    = new int[SubChunk.CHUNKSIZEZ * SubChunk.CHUNKSIZEX];
        int[] downMask  = new int[SubChunk.CHUNKSIZEZ * SubChunk.CHUNKSIZEX];

        int[] northAoMask = new int[SubChunk.CHUNKSIZEY * SubChunk.CHUNKSIZEZ * SubChunk.CHUNKSIZEX];
        int[] southAoMask = new int[SubChunk.CHUNKSIZEY * SubChunk.CHUNKSIZEZ * SubChunk.CHUNKSIZEX];
        int[] westAoMask  = new int[SubChunk.CHUNKSIZEY * SubChunk.CHUNKSIZEX * SubChunk.CHUNKSIZEX];
        int[] eastAoMask  = new int[SubChunk.CHUNKSIZEY * SubChunk.CHUNKSIZEX * SubChunk.CHUNKSIZEX];
        int[] upAoMask    = new int[SubChunk.CHUNKSIZEZ * SubChunk.CHUNKSIZEX * SubChunk.CHUNKSIZEX];
        int[] downAoMask  = new int[SubChunk.CHUNKSIZEZ * SubChunk.CHUNKSIZEX * SubChunk.CHUNKSIZEX];
    }

    // obligé sinon les threads qui construisent les chunks pour modifié les infos
    // Thread A écrit dans northAxis[0], Thread B écrit dans northAxis[0] au même moment -> les données se corrompent mutuellement
    // c'est pour cela qu'un threadlocal, va créer une instance de FACE_DATA_LOCAL sur chaque thread
    private static final ThreadLocal<FaceData> FACE_DATA_LOCAL = ThreadLocal.withInitial(() -> new FaceData());

    @ChunkThread
    public SubChunk(){
        this.quads = new ArrayList<Quad>();
        this.blocks = new byte[SubChunk.MAXBLOCK];    
        this.lightMap = new byte[SubChunk.MAXBLOCK]; 
        Arrays.fill(this.lightMap, (byte)15);
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

        FaceData faceData = new FaceData();
        //FaceData faceData = FACE_DATA_LOCAL.get(); TODO : fait bug le face culling

        if (this.quads == null) this.quads = new ArrayList<>(); // réinitialise si null après un précédent mesh

        // calcule le face culling
        this.calculateSubChunkFaceCulling(faceData);

        // collecte les types uniques
        // le Set n'accepte pas les doublons donc si j'ai 500 GRASS dans le set il n'y aura que 1 GRASS
        java.util.Set<Byte> types = new java.util.HashSet<>();
        for(byte b : this.blocks){
            if(b != (byte)BlocksType.AIR.getId()) types.add(b);
        }     

        // calcule le greedy meshing pour chaque type de block
        for(byte blockType : types){
            this.calculateSubChunkBinaryGreedyMeshing(blockType, faceData);
            this.uploadSubChunkMesh(blockType);
        }
        // rempli le buffer de la maille avec les données pack (vertex packing)
        this.mesh.fillSubChunkVertexBufferWithArray();

        this.quads = null;
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
     * Récupère la lumière d'un block
     * @param x
     * @param y
     * @param z
     * @return
     */
    public byte getLightAt(int x, int y, int z){
        if (x >= 0 && x < CHUNKSIZEX && y >= 0 && y < CHUNKSIZEY && z >= 0 && z < CHUNKSIZEZ){
            return this.lightMap[x + y * CHUNKSIZEX + z * CHUNKSIZEX * CHUNKSIZEY];
        }
        if (y >= CHUNKSIZEY && this.upNeighbor != null)
            return this.upNeighbor.getLightAt(x, y - CHUNKSIZEY, z);
        if (y < 0 && this.downNeighbor != null)
            return this.downNeighbor.getLightAt(x, y + CHUNKSIZEY, z);
        if (x >= CHUNKSIZEX && this.westNeighbor != null)
            return this.westNeighbor.getLightAt(x - CHUNKSIZEX, y, z);
        if (x < 0 && this.eastNeighbor != null)
            return this.eastNeighbor.getLightAt(x + CHUNKSIZEX, y, z);
        if (z >= CHUNKSIZEZ && this.northNeighbor != null)
            return this.northNeighbor.getLightAt(x, y, z - CHUNKSIZEZ);
        if (z < 0 && this.southNeighbor != null)
            return this.southNeighbor.getLightAt(x, y, z + CHUNKSIZEZ);
        return 0;
    }

    /**
     * Définis la lumière d'un block
     * @param x
     * @param y
     * @param z
     * @param value
     */
    public void setLightAt(int x, int y, int z, byte value){
        if (x >= 0 && x < CHUNKSIZEX && y >= 0 && y < CHUNKSIZEY && z >= 0 && z < CHUNKSIZEZ){
            this.lightMap[x + y * CHUNKSIZEX + z * CHUNKSIZEX * CHUNKSIZEY] = value;
            return;
        }
        if (y >= CHUNKSIZEY && this.upNeighbor != null)
            { this.upNeighbor.setLightAt(x, y - CHUNKSIZEY, z, value); return; }
        if (y < 0 && this.downNeighbor != null)
            { this.downNeighbor.setLightAt(x, y + CHUNKSIZEY, z, value); return; }
        if (x >= CHUNKSIZEX && this.westNeighbor != null)
            { this.westNeighbor.setLightAt(x - CHUNKSIZEX, y, z, value); return; }
        if (x < 0 && this.eastNeighbor != null)
            { this.eastNeighbor.setLightAt(x + CHUNKSIZEX, y, z, value); return; }
        if (z >= CHUNKSIZEZ && this.northNeighbor != null)
            { this.northNeighbor.setLightAt(x, y, z - CHUNKSIZEZ, value); return; }
        if (z < 0 && this.southNeighbor != null)
            { this.southNeighbor.setLightAt(x, y, z + CHUNKSIZEZ, value); return; }
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
        if (this.mesh.getVertexCount() == 0) return;  // subchunk vide, rien à uploader
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
    private void uploadSubChunkMesh(byte blockType){

        for(Quad quad : this.quads){
            BlocksType type = BlocksType.fromId(blockType);
            if(type == BlocksType.NULL) continue;
            int textureId = TexturesManager.getIndex(type.getTexture(quad.direction));
            int blocksIndex = quad.x + quad.y * SubChunk.CHUNKSIZEX + quad.z * SubChunk.CHUNKSIZEX * SubChunk.CHUNKSIZEY;
            int light = this.lightMap[blocksIndex] & 0xF;
            this.mesh.addQuadVertexToArray(quad, textureId, light);
        }        
    }

    /**
     * Comme son nom l'indique
     * Calcule aussi avec les faces des chunks voisins
     */
    @ChunkThread
    private void calculateSubChunkFaceCulling(FaceData faceData){
        // Récup ouest est, on remplit l'int qui correspond a la ligne avec le BitSet du subChunk
        for (int y = 0; y < SubChunk.CHUNKSIZEY; y++){
            for (int z = 0; z < SubChunk.CHUNKSIZEZ; z++){
                int index = y + z * SubChunk.CHUNKSIZEY;      
                for (int x = 0; x < SubChunk.CHUNKSIZEX; x++){
                    int bitSetIndex = x + y * SubChunk.CHUNKSIZEX + z * SubChunk.CHUNKSIZEX * SubChunk.CHUNKSIZEY;                    
                    faceData.eastAxis[index] |= (this.bitSet.get(bitSetIndex) ? 1 : 0) << (31 - x);                    
                }
                int ligne = faceData.eastAxis[index];
                
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
                faceData.eastAxis[index] = ligne & ~((ligne >>> 1) | ligneNeighborEast); // faces vers l'est + le bloc voisin
                faceData.westAxis[index] = ligne & ~((ligne << 1) | ligneNeighborWest); // faces vers l'ouest + le bloc voisin     
            }
        }
        // Récup nord sud
        for (int y = 0; y < SubChunk.CHUNKSIZEY; y++){
            for (int x = 0; x < SubChunk.CHUNKSIZEX; x++){
                int index = y + x * SubChunk.CHUNKSIZEY;
                for (int z = 0; z < SubChunk.CHUNKSIZEZ; z++){
                    int bitSetIndex = x + y * SubChunk.CHUNKSIZEX + z * SubChunk.CHUNKSIZEX * SubChunk.CHUNKSIZEY;
                    faceData.northAxis[index] |= (this.bitSet.get(bitSetIndex) ? 1 : 0) << (31 - z);
                }
                int ligne = faceData.northAxis[index];

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

                faceData.southAxis[index] = ligne & ~((ligne >>> 1) | ligneNeighborSouth); // faces vers le sud  
                faceData.northAxis[index] = ligne & ~((ligne << 1) | ligneNeighborNorth); // faces vers le nord
            }
        }
        // Récup haut bas
        for (int z = 0; z < SubChunk.CHUNKSIZEZ; z++){
            for (int x = 0; x < SubChunk.CHUNKSIZEX; x++){
                int index = z + x * SubChunk.CHUNKSIZEZ;
                for (int y = 0; y < SubChunk.CHUNKSIZEY; y++){
                    int bitSetIndex = x + y * SubChunk.CHUNKSIZEX + z * SubChunk.CHUNKSIZEX * SubChunk.CHUNKSIZEY;
                    faceData.upAxis[index] |= (this.bitSet.get(bitSetIndex) ? 1 : 0) << (31 - y);
                }
                int ligne = faceData.upAxis[index];
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
                faceData.upAxis[index] = ligne & ~((ligne << 1) | ligneNeighborUp); // faces vers le haut
                faceData.downAxis[index] = ligne & ~((ligne >>> 1) | ligneNeighborDown); // faces vers le bas  
            }
        }
    }

    /**
     * Peut pas faire plus simple comme nom
     */
    private void calculateSubChunkBinaryGreedyMeshing(byte blockType, FaceData faceData){
        this.quads.clear();

        // rempli les masques avec les infos du face culling
        for (int y = 0; y < SubChunk.CHUNKSIZEY; y++){
            for (int z = 0; z < SubChunk.CHUNKSIZEZ; z++){                
                for (int x = 0; x < SubChunk.CHUNKSIZEX; x++){
                    // filtre par type
                    int blockIndex = x + y * SubChunk.CHUNKSIZEX + z * SubChunk.CHUNKSIZEX * SubChunk.CHUNKSIZEY;
                    if(this.blocks[blockIndex] != blockType) continue;

                    int westBit = (faceData.westAxis[y + z * SubChunk.CHUNKSIZEY] >>> (31 - x)) & 1;
                    faceData.westMask[y + x * SubChunk.CHUNKSIZEX] |= (westBit << (31 - z));  
                    if(westBit == 1){
                        int ao0 = computeAO(x+1,y,z-1,  x+1,y-1,z,  x+1,y-1,z-1);
                        int ao1 = computeAO(x+1,y,z+1,  x+1,y-1,z,  x+1,y-1,z+1);
                        int ao2 = computeAO(x+1,y,z-1,  x+1,y+1,z,  x+1,y+1,z-1);
                        int ao3 = computeAO(x+1,y,z+1,  x+1,y+1,z,  x+1,y+1,z+1);
                        int ligne = y + x * SubChunk.CHUNKSIZEX;
                        faceData.westAoMask[ligne * SubChunk.CHUNKSIZEX + z] = ao0|(ao1<<2)|(ao2<<4)|(ao3<<6);
                    }

                    int eastBit = (faceData.eastAxis[y + z * SubChunk.CHUNKSIZEY] >> (31 - x)) & 1;
                    faceData.eastMask[y + x * SubChunk.CHUNKSIZEX] |= (eastBit << (31 - z));
                    if(eastBit == 1){
                        int ao0 = computeAO(x,y,z-1,  x,y-1,z,  x,y-1,z-1);
                        int ao1 = computeAO(x,y,z+1,  x,y-1,z,  x,y-1,z+1);
                        int ao2 = computeAO(x,y,z-1,  x,y+1,z,  x,y+1,z-1);
                        int ao3 = computeAO(x,y,z+1,  x,y+1,z,  x,y+1,z+1);
                        int ligne = y + x * SubChunk.CHUNKSIZEX;
                        faceData.eastAoMask[ligne * SubChunk.CHUNKSIZEX + z] = ao0|(ao1<<2)|(ao2<<4)|(ao3<<6);
                    }
                }
            }
        }        
        for (int y = 0; y < SubChunk.CHUNKSIZEY; y++){
            for (int x = 0; x < SubChunk.CHUNKSIZEX; x++){
                for (int z = 0; z < SubChunk.CHUNKSIZEZ; z++){
                    int blockIndex = x + y * SubChunk.CHUNKSIZEX + z * SubChunk.CHUNKSIZEX * SubChunk.CHUNKSIZEY;
                    if(this.blocks[blockIndex] != blockType) continue;

                    // En gros je récup tous les bits d'un int d'Axis, que je met en 1er position des int de mask a chaque boucle
                    int northBit = (faceData.northAxis[y + x * SubChunk.CHUNKSIZEY] >>> (31 - z)) & 1; // décalle de 31 bits + z vers la droite puis récupère le 1er bit
                    faceData.northMask[y + z * SubChunk.CHUNKSIZEZ] |= (northBit << (31 - x));
                    if(northBit == 1){
                        int ao0 = computeAO(x-1,y,z+1,  x,y-1,z+1,  x-1,y-1,z+1);
                        int ao1 = computeAO(x+1,y,z+1,  x,y-1,z+1,  x+1,y-1,z+1);
                        int ao2 = computeAO(x-1,y,z+1,  x,y+1,z+1,  x-1,y+1,z+1);
                        int ao3 = computeAO(x+1,y,z+1,  x,y+1,z+1,  x+1,y+1,z+1);
                        int ligne = y + z * SubChunk.CHUNKSIZEZ;
                        faceData.northAoMask[ligne * SubChunk.CHUNKSIZEX + x] = ao0|(ao1<<2)|(ao2<<4)|(ao3<<6);
                    }

                    int southBit = (faceData.southAxis[y + x * SubChunk.CHUNKSIZEY] >> (31 - z)) & 1; // décalle de 31 bits - z vers la droite puis récupère le 1er bit*
                    faceData.southMask[y + z * SubChunk.CHUNKSIZEZ] |= (southBit << (31 - x));
                    if(southBit == 1){
                        int ao0 = computeAO(x-1,y,z,  x,y-1,z,  x-1,y-1,z);
                        int ao1 = computeAO(x+1,y,z,  x,y-1,z,  x+1,y-1,z);
                        int ao2 = computeAO(x-1,y,z,  x,y+1,z,  x-1,y+1,z);
                        int ao3 = computeAO(x+1,y,z,  x,y+1,z,  x+1,y+1,z);
                        int ligne = y + z * SubChunk.CHUNKSIZEZ;
                        faceData.southAoMask[ligne * SubChunk.CHUNKSIZEX + x] = ao0|(ao1<<2)|(ao2<<4)|(ao3<<6);
                    }

                }
            }
        }
        for (int y = 0; y < SubChunk.CHUNKSIZEY; y++){
            for (int z = 0; z < SubChunk.CHUNKSIZEZ; z++){
                for (int x = 0; x < SubChunk.CHUNKSIZEX; x++){
                    int blockIndex = x + y * SubChunk.CHUNKSIZEX + z * SubChunk.CHUNKSIZEX * SubChunk.CHUNKSIZEY;
                    if(this.blocks[blockIndex] != blockType) continue;

                    int upBit = (faceData.upAxis[z + x * SubChunk.CHUNKSIZEZ] >>> (31 - y)) & 1;
                    faceData.upMask[z + y * SubChunk.CHUNKSIZEZ] |= (upBit << (31 - x));
                    if(upBit == 1){
                        int ao0 = computeAO(x-1,y+1,z,  x,y+1,z-1,  x-1,y+1,z-1);
                        int ao1 = computeAO(x+1,y+1,z,  x,y+1,z-1,  x+1,y+1,z-1);
                        int ao2 = computeAO(x-1,y+1,z,  x,y+1,z+1,  x-1,y+1,z+1);
                        int ao3 = computeAO(x+1,y+1,z,  x,y+1,z+1,  x+1,y+1,z+1);
                        int ligne = z + y * SubChunk.CHUNKSIZEZ;
                        faceData.upAoMask[ligne * SubChunk.CHUNKSIZEX + x] = ao0|(ao1<<2)|(ao2<<4)|(ao3<<6);
                    }

                    int downBit = (faceData.downAxis[z + x * SubChunk.CHUNKSIZEZ] >>> (31 - y)) & 1;
                    faceData.downMask[z + y * SubChunk.CHUNKSIZEZ] |= (downBit << (31 - x));
                    if(downBit == 1){
                        int ao0 = computeAO(x-1,y,z,  x,y,z-1,  x-1,y,z-1);
                        int ao1 = computeAO(x+1,y,z,  x,y,z-1,  x+1,y,z-1);
                        int ao2 = computeAO(x-1,y,z,  x,y,z+1,  x-1,y,z+1);
                        int ao3 = computeAO(x+1,y,z,  x,y,z+1,  x+1,y,z+1);
                        int ligne = z + y * SubChunk.CHUNKSIZEZ;
                        faceData.downAoMask[ligne * SubChunk.CHUNKSIZEX + x] = ao0|(ao1<<2)|(ao2<<4)|(ao3<<6);
                    }
                }
            }
        }

        this.greedyMeshingPerFace(faceData.westMask, faceData.westAoMask, Directions.WEST);
        this.greedyMeshingPerFace(faceData.eastMask, faceData.eastAoMask, Directions.EAST);
        this.greedyMeshingPerFace(faceData.northMask, faceData.northAoMask, Directions.NORTH);
        this.greedyMeshingPerFace(faceData.southMask, faceData.southAoMask, Directions.SOUTH);
        this.greedyMeshingPerFace(faceData.upMask, faceData.upAoMask, Directions.UP);
        this.greedyMeshingPerFace(faceData.downMask, faceData.downAoMask, Directions.DOWN);
    }

    // fait le greedy pour chaque face
    private void greedyMeshingPerFace(int[] faceMask, int[] faceAoMask, Directions dir){
        for(int ligne = 0; ligne < faceMask.length; ligne++){
            int i = 0; // indice en largeur byte car dépasse pas 15
            while(i < SubChunk.CHUNKSIZEX){              
                if (faceMask[ligne] == 0) break; // si il n'y a pas de face           
                int j = 1; // indice en longueur/hauteur

                int[] coord = new int[3]; // point de départ, fin quad en x, fin quad en y
                while((i < SubChunk.CHUNKSIZEX) && ((faceMask[ligne] >>> (31 - i)) & 1) != 1){ // tant que l'on a pas trouvé la 1er face pleine
                    i++;
                }
                coord[0] = i;// x commence à i
                while((i < SubChunk.CHUNKSIZEX) && ((faceMask[ligne] >>> (31 - i)) & 1) != 0
                    && faceAoMask[ligne * SubChunk.CHUNKSIZEX + i] == faceAoMask[ligne * SubChunk.CHUNKSIZEX + coord[0]]){
                    i++;
                }
                coord[1] = i;// x se termine à i
                 
                if (coord[0] >= coord[1]){
                    i++;
                    continue;
                }
                // exemple x=2 x=5 (5 - (2 - 1)) = 4 largeur
                //        1 << 4 = 00010000
                //  (1 << 4) - 1 = 00001111
                int width = coord[1] - coord[0];
                int mask = ((1 << width) - 1) << (32 - coord[1]);
                faceMask[ligne] = faceMask[ligne] & ~mask;
                
                // parcourt en hauteur 
                // si la ligne en dessous a les même bits entre les 2 x à 1 alors on la récup 
                int sliceStart = (ligne / SubChunk.CHUNKSIZEX) * SubChunk.CHUNKSIZEX;
                while((ligne + j < sliceStart + SubChunk.CHUNKSIZEX)
                    && (ligne + j < faceMask.length) 
                    && (faceMask[ligne + j] & mask) == mask){

                    boolean aoMatch = true;
                    for(int k = coord[0]; k < coord[1]; k++){
                        if(faceAoMask[(ligne+j) * SubChunk.CHUNKSIZEX + k] != faceAoMask[ligne * SubChunk.CHUNKSIZEX + k]){
                            aoMatch = false;
                            break;
                        }
                    }
                    if(!aoMatch) break;
                    faceMask[ligne + j] = faceMask[ligne + j] & ~mask;
                    j++;
                }
                // le quad descent de j
                coord[2] = j;       

                if (coord[0] < coord[1]){
                    int qx, qy, qz;
                    switch(dir){
                        case NORTH: case SOUTH:
                            // masque index = y + z * CHUNKSIZEZ, bit = X
                            qx = coord[0];
                            qy = ligne % SubChunk.CHUNKSIZEY;
                            qz = ligne / SubChunk.CHUNKSIZEZ;
                            break;
                        case WEST: case EAST:
                            // masque index = y + x * CHUNKSIZEX, bit = Z
                            qx = ligne / SubChunk.CHUNKSIZEY;
                            qy = ligne % SubChunk.CHUNKSIZEY;
                            qz = coord[0];
                            break;
                        case UP: case DOWN:
                            // masque index = y + z * CHUNKSIZEY, bit = X
                            qx = coord[0];
                            qy = ligne / SubChunk.CHUNKSIZEZ;  
                            qz = ligne % SubChunk.CHUNKSIZEZ; 
                            break;
                        default:
                            qx = qy = qz = 0;
                    }
                    int packedAo = faceAoMask[ligne * SubChunk.CHUNKSIZEX + coord[0]];
                    this.quads.add(new Quad(qx, qy, qz, coord[1]-coord[0], coord[2], dir,
                        packedAo & 0x3, (packedAo>>2) & 0x3, (packedAo>>4) & 0x3, (packedAo>>6) & 0x3));
                }   
            }           
        }
    }

    /**
     * Détermine si un block est solide <br>
     * Si le block n'est pas dans le chunk (voisin d'un chunk) <br>
     * On demande au voisin
     * @param x
     * @param y
     * @param z
     * @return
     */
    private boolean isBlockSolid(int x, int y, int z){
        // dans le subchunk courant
        if (x >= 0 && x < CHUNKSIZEX && y >= 0 && y < CHUNKSIZEY && z >= 0 && z < CHUNKSIZEZ){
            int index = x + y * CHUNKSIZEX + z * CHUNKSIZEX * CHUNKSIZEY;
            return this.blocks[index] != (byte)BlocksType.AIR.getId();
        }
        // en dehors délègue aux voisins, blocks en bordure
        if (y >= CHUNKSIZEY && this.upNeighbor != null){
            return this.upNeighbor.isBlockSolid(x, y - CHUNKSIZEY, z);
        }
        if (y < 0 && this.downNeighbor != null){
            return this.downNeighbor.isBlockSolid(x, y + CHUNKSIZEY, z);
        }
        if (x >= CHUNKSIZEX && this.westNeighbor != null){
            return this.westNeighbor.isBlockSolid(x - CHUNKSIZEX, y, z);
        }
        if (x < 0 && this.eastNeighbor != null){
            return this.eastNeighbor.isBlockSolid(x + CHUNKSIZEX, y, z);
        }
        if (z >= CHUNKSIZEZ && this.northNeighbor != null){
            return this.northNeighbor.isBlockSolid(x, y, z - CHUNKSIZEZ);
        }
        if (z < 0 && this.southNeighbor != null){
            return this.southNeighbor.isBlockSolid(x, y, z + CHUNKSIZEZ);
        }
        return false;
    }

    /**
     * Calcule l'ambient occlusion à appliquer sur le block
     * @param side1X
     * @param side1Y
     * @param side1Z
     * @param side2X
     * @param side2Y
     * @param side2Z
     * @param cornerX
     * @param cornerY
     * @param cornerZ
     * @return
     */
    private byte computeAO(int side1X, int side1Y, int side1Z,
                           int side2X, int side2Y, int side2Z,
                           int cornerX, int cornerY, int cornerZ){
        boolean s1 = isBlockSolid(side1X, side1Y, side1Z);
        boolean s2 = isBlockSolid(side2X, side2Y, side2Z);
        boolean c  = isBlockSolid(cornerX, cornerY, cornerZ);

        if (s1 && s2) return 3;
        return (byte)((s1 ? 1 : 0) + (s2 ? 1 : 0) + (c ? 1 : 0));
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
        this.mesh.drawSubChunkMesh();
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
