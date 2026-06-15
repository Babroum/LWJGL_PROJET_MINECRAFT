package demo.Geometry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

import org.joml.Matrix4f;

import demo.Annotations.ChunkThread;
import demo.Annotations.OpenGLThread;
import demo.Events.KeyboardInput;
import demo.Managers.BlocksManager;
import demo.Managers.ChunksManager;
import demo.Managers.ShadersManager;
import demo.Meshing.SubChunkMesh;
import demo.Others.BlockDefinition;
import demo.Others.Blocks;
import demo.Meshing.Quad;
import demo.Rendering.Render;

public class SubChunk extends Node3D{

    public static final int CHUNKSIZEXZ = 16;
    public static final int CHUNKSIZEHEIGHT = 256;
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
    public byte[] blocks; // id des blocks (MAX 256 blocks différent car 8bits)
    public byte[] lightMap; // contient la lumière du ciel et celle du block 4bits et 4bits

    private List<Quad> quads;

    public boolean dirty = false;

    public Chunk parent;

    // contient des 1 et 0 (block plein, block vide(air))
    public BitSet bitSet = new BitSet(SubChunk.MAXBLOCK); 

    private static class FaceData {
        int[] northAxis = new int[SubChunk.CHUNKSIZEXZ * SubChunk.CHUNKSIZEXZ];
        int[] southAxis = new int[SubChunk.CHUNKSIZEXZ * SubChunk.CHUNKSIZEXZ];
        int[] westAxis  = new int[SubChunk.CHUNKSIZEXZ * SubChunk.CHUNKSIZEXZ];
        int[] eastAxis  = new int[SubChunk.CHUNKSIZEXZ * SubChunk.CHUNKSIZEXZ];
        int[] upAxis    = new int[SubChunk.CHUNKSIZEXZ * SubChunk.CHUNKSIZEXZ];
        int[] downAxis  = new int[SubChunk.CHUNKSIZEXZ * SubChunk.CHUNKSIZEXZ];

        int[] northMask = new int[SubChunk.CHUNKSIZEXZ * SubChunk.CHUNKSIZEXZ];
        int[] southMask = new int[SubChunk.CHUNKSIZEXZ * SubChunk.CHUNKSIZEXZ];
        int[] westMask  = new int[SubChunk.CHUNKSIZEXZ * SubChunk.CHUNKSIZEXZ];
        int[] eastMask  = new int[SubChunk.CHUNKSIZEXZ * SubChunk.CHUNKSIZEXZ];
        int[] upMask    = new int[SubChunk.CHUNKSIZEXZ * SubChunk.CHUNKSIZEXZ];
        int[] downMask  = new int[SubChunk.CHUNKSIZEXZ * SubChunk.CHUNKSIZEXZ];

        byte[] northAoMask = new byte[SubChunk.CHUNKSIZEXZ * SubChunk.CHUNKSIZEHEIGHT];
        byte[] southAoMask = new byte[SubChunk.CHUNKSIZEXZ * SubChunk.CHUNKSIZEHEIGHT];
        byte[] westAoMask  = new byte[SubChunk.CHUNKSIZEXZ * SubChunk.CHUNKSIZEHEIGHT];
        byte[] eastAoMask  = new byte[SubChunk.CHUNKSIZEXZ * SubChunk.CHUNKSIZEHEIGHT];
        byte[] upAoMask    = new byte[SubChunk.CHUNKSIZEXZ * SubChunk.CHUNKSIZEHEIGHT];
        byte[] downAoMask  = new byte[SubChunk.CHUNKSIZEXZ * SubChunk.CHUNKSIZEHEIGHT];

        byte[] northSkyLightMask = new byte[SubChunk.CHUNKSIZEXZ * SubChunk.CHUNKSIZEHEIGHT];
        byte[] southSkyLightMask = new byte[SubChunk.CHUNKSIZEXZ * SubChunk.CHUNKSIZEHEIGHT];
        byte[] westSkyLightMask  = new byte[SubChunk.CHUNKSIZEXZ * SubChunk.CHUNKSIZEHEIGHT];
        byte[] eastSkyLightMask  = new byte[SubChunk.CHUNKSIZEXZ * SubChunk.CHUNKSIZEHEIGHT];
        byte[] upSkyLightMask    = new byte[SubChunk.CHUNKSIZEXZ * SubChunk.CHUNKSIZEHEIGHT];
        byte[] downSkyLightMask  = new byte[SubChunk.CHUNKSIZEXZ * SubChunk.CHUNKSIZEHEIGHT];

        byte[] northBlockLightMask = new byte[SubChunk.CHUNKSIZEXZ * SubChunk.CHUNKSIZEHEIGHT];
        byte[] southBlockLightMask = new byte[SubChunk.CHUNKSIZEXZ * SubChunk.CHUNKSIZEHEIGHT];
        byte[] westBlockLightMask  = new byte[SubChunk.CHUNKSIZEXZ * SubChunk.CHUNKSIZEHEIGHT];
        byte[] eastBlockLightMask  = new byte[SubChunk.CHUNKSIZEXZ * SubChunk.CHUNKSIZEHEIGHT];
        byte[] upBlockLightMask    = new byte[SubChunk.CHUNKSIZEXZ * SubChunk.CHUNKSIZEHEIGHT];
        byte[] downBlockLightMask  = new byte[SubChunk.CHUNKSIZEXZ * SubChunk.CHUNKSIZEHEIGHT];
    }

    // obligé sinon les threads qui construisent les chunks pour modifié les infos
    // Thread A écrit dans northAxis[0], Thread B écrit dans northAxis[0] au même moment -> les données se corrompent mutuellement
    // c'est pour cela qu'un threadlocal, va créer une instance de FACE_DATA_LOCAL sur chaque thread
    private static final ThreadLocal<FaceData> FACE_DATA_LOCAL = ThreadLocal.withInitial(() -> new FaceData());

    @ChunkThread
    public SubChunk(Chunk parent){
        this.parent = parent;
        this.quads = new ArrayList<Quad>();
        this.blocks = new byte[SubChunk.MAXBLOCK];   
        this.lightMap = new byte[SubChunk.MAXBLOCK];   
        // définit la taille du buffer et du tableau de vertex  
        this.mesh.setupSubChunkBuffers(this.bitSet.cardinality());   
    }

    /**
     * Retourne la lumière du ciel sur le block.
     * @param x Coordonnées en x (locale).
     * @param y Coordonnées en y (locale).
     * @param z Coordonnées en z (locale).
     * @return la lumière (0 - 15)
     */
    public int getBlockSkyLight(int x, int y, int z){         
        int index = x + y * SubChunk.CHUNKSIZEXZ + z * SubChunk.CHUNKSIZEXZ * SubChunk.CHUNKSIZEXZ;

        if (x >= 0 && x < CHUNKSIZEXZ && y >= 0 && y < CHUNKSIZEXZ && z >= 0 && z < CHUNKSIZEXZ)
            { return (lightMap[index] & 0xF0) >>> 4; }
        if (y >= CHUNKSIZEXZ && this.upNeighbor != null)
            { return this.upNeighbor.getBlockSkyLight(x, y - CHUNKSIZEXZ, z); }
        if (y < 0 && this.downNeighbor != null)
            { return this.downNeighbor.getBlockSkyLight(x, y + CHUNKSIZEXZ, z); }
        if (x >= CHUNKSIZEXZ && this.westNeighbor != null)
            { return this.westNeighbor.getBlockSkyLight(x - CHUNKSIZEXZ, y, z); }
        if (x < 0 && this.eastNeighbor != null)
            { return this.eastNeighbor.getBlockSkyLight(x + CHUNKSIZEXZ, y, z); }
        if (z >= CHUNKSIZEXZ && this.northNeighbor != null)
            { return this.northNeighbor.getBlockSkyLight(x, y, z - CHUNKSIZEXZ); }
        if (z < 0 && this.southNeighbor != null)
            { return this.southNeighbor.getBlockSkyLight(x, y, z + CHUNKSIZEXZ); }
        return 0;
    }

    /**
     * Retourne la lumière du block.
     * @param x Coordonnées en x (locale)
     * @param y Coordonnées en y (locale)
     * @param z Coordonnées en z (locale)
     * @return la lumière (0 - 15)
     */
    public int getBlockLight(int x, int y, int z){
        int index = x + y * SubChunk.CHUNKSIZEXZ + z * SubChunk.CHUNKSIZEXZ * SubChunk.CHUNKSIZEXZ;

        if (x >= 0 && x < CHUNKSIZEXZ && y >= 0 && y < CHUNKSIZEXZ && z >= 0 && z < CHUNKSIZEXZ)
            { return this.lightMap[index] & 0x0F; }
        if (y >= CHUNKSIZEXZ && this.upNeighbor != null)
            { return this.upNeighbor.getBlockLight(x, y - CHUNKSIZEXZ, z); }
        if (y < 0 && this.downNeighbor != null)
            { return this.downNeighbor.getBlockLight(x, y + CHUNKSIZEXZ, z); }
        if (x >= CHUNKSIZEXZ && this.westNeighbor != null)
            { return this.westNeighbor.getBlockLight(x - CHUNKSIZEXZ, y, z); }
        if (x < 0 && this.eastNeighbor != null)
            { return this.eastNeighbor.getBlockLight(x + CHUNKSIZEXZ, y, z); }
        if (z >= CHUNKSIZEXZ && this.northNeighbor != null)
            { return this.northNeighbor.getBlockLight(x, y, z - CHUNKSIZEXZ); }
        if (z < 0 && this.southNeighbor != null)
            { return this.southNeighbor.getBlockLight(x, y, z + CHUNKSIZEXZ); }
        return 0;
    }

    /**
     * Définis la nouvelle lumière du ciel sur le block.
     * @param x Coordonnées en x (locale)
     * @param y Coordonnées en y (locale)
     * @param z Coordonnées en z (locale)
     * @param newLight Nouvelle valeur de lumière
     */
    public void setBlockSkyLight(int x, int y, int z, int newLight){
        //System.out.println("WRITE " + newLight + " at " + x + "," + y + "," + z);
        if (newLight < 0 || newLight > 15) throw new RuntimeException("La lumière doit être comprise entre 0 et 15");
        int index = x + y * SubChunk.CHUNKSIZEXZ + z * SubChunk.CHUNKSIZEXZ * SubChunk.CHUNKSIZEXZ;

        if (x >= 0 && x < CHUNKSIZEXZ && y >= 0 && y < CHUNKSIZEXZ && z >= 0 && z < CHUNKSIZEXZ){
            byte oldLight = this.lightMap[index];
            this.lightMap[index] = (byte)((oldLight & 0x0F) | (((byte)newLight & 0x0F) << 4));
            return; 
        }
        if (y >= CHUNKSIZEXZ && this.upNeighbor != null)
            { this.upNeighbor.setBlockSkyLight(x, y - CHUNKSIZEXZ, z, newLight); return; }
        if (y < 0 && this.downNeighbor != null)
            { this.downNeighbor.setBlockSkyLight(x, y + CHUNKSIZEXZ, z, newLight); return; }
        if (x >= CHUNKSIZEXZ && this.westNeighbor != null)
            { this.westNeighbor.setBlockSkyLight(x - CHUNKSIZEXZ, y, z, newLight); return; }
        if (x < 0 && this.eastNeighbor != null)
            { this.eastNeighbor.setBlockSkyLight(x + CHUNKSIZEXZ, y, z, newLight); return; }
        if (z >= CHUNKSIZEXZ && this.northNeighbor != null)
            { this.northNeighbor.setBlockSkyLight(x, y, z - CHUNKSIZEXZ, newLight); return; }
        if (z < 0 && this.southNeighbor != null)
            { this.southNeighbor.setBlockSkyLight(x, y, z + CHUNKSIZEXZ, newLight); return; }
    }

    /**
     * Définis la nouvelle lumière du block.
     * @param x Coordonnées en x (locale)
     * @param y Coordonnées en y (locale)
     * @param z Coordonnées en z (locale)
     * @param newLight Nouvelle valeur de lumière
     */
    public void setBlockLight(int x, int y, int z, int newLight){
        if (newLight < 0 || newLight > 15) throw new RuntimeException("La lumière doit être comprise entre 0 et 15");
        int index = x + y * SubChunk.CHUNKSIZEXZ + z * SubChunk.CHUNKSIZEXZ * SubChunk.CHUNKSIZEXZ; 

        if (x >= 0 && x < CHUNKSIZEXZ && y >= 0 && y < CHUNKSIZEXZ && z >= 0 && z < CHUNKSIZEXZ){
            byte oldLight = this.lightMap[index];
            this.lightMap[index] = (byte)((oldLight & 0xF0) | ((byte)newLight & 0x0F));
            return; 
        } // garde les 4 bits à gauche et met les 4 bits à droite avec les nouvelles valeurs
        if (y >= CHUNKSIZEXZ && this.upNeighbor != null)
            { this.upNeighbor.setBlockLight(x, y - CHUNKSIZEXZ, z, newLight); return; }
        if (y < 0 && this.downNeighbor != null)
            { this.downNeighbor.setBlockLight(x, y + CHUNKSIZEXZ, z, newLight); return; }
        if (x >= CHUNKSIZEXZ && this.westNeighbor != null)
            { this.westNeighbor.setBlockLight(x - CHUNKSIZEXZ, y, z, newLight); return; }
        if (x < 0 && this.eastNeighbor != null)
            { this.eastNeighbor.setBlockLight(x + CHUNKSIZEXZ, y, z, newLight); return; }
        if (z >= CHUNKSIZEXZ && this.northNeighbor != null)
            { this.northNeighbor.setBlockLight(x, y, z - CHUNKSIZEXZ, newLight); return; }
        if (z < 0 && this.southNeighbor != null)
            { this.southNeighbor.setBlockLight(x, y, z + CHUNKSIZEXZ, newLight); return; }
    }

    public void clearSubChunkLight(){
        for (int i = 0; i < this.lightMap.length; i++){
            this.lightMap[i] = 0;
        }
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

        FaceData faceData = FACE_DATA_LOCAL.get();
        if (this.quads == null) this.quads = new ArrayList<>(); // réinitialise si null après un précédent mesh

        // calcule le face culling
        this.calculateSubChunkFaceCulling(faceData);

        // collecte les types uniques
        // le Set n'accepte pas les doublons donc si j'ai 500 GRASS dans le set il n'y aura que 1 GRASS
        Set<Byte> types = new HashSet<>();
        for(byte b : this.blocks){
            if(b != (byte)Blocks.AIR.id) types.add(b);
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
     * Retourne le block à la position.
     * @param x Coordonnée en x (locale)
     * @param y Coordonnée en x (locale)
     * @param z Coordonnée en x (locale)
     * @return Retourne le block
     */
    public BlockDefinition getBlock(int x, int y, int z){
        if (x < 0 || x >= SubChunk.CHUNKSIZEXZ ||
            y < 0 || y >= SubChunk.CHUNKSIZEXZ ||
            z < 0 || z >= SubChunk.CHUNKSIZEXZ
        ){throw new RuntimeException("Coordonées dans le subChunk invalide");}
        int index = x + y * SubChunk.CHUNKSIZEXZ + z * SubChunk.CHUNKSIZEXZ * SubChunk.CHUNKSIZEXZ;
        return BlocksManager.get(this.blocks[index]);
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
        this.clearSubChunkLight();
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
     */
    @ChunkThread
    public void addBlockToSubChunk(BlockDefinition type, int x, int y, int z){
        if (x < 0 || x >= SubChunk.CHUNKSIZEXZ ||
            y < 0 || y >= SubChunk.CHUNKSIZEXZ ||
            z < 0 || z >= SubChunk.CHUNKSIZEXZ
        ){throw new RuntimeException("Coordonées dans le subChunk invalide");}
        int index = x + y * SubChunk.CHUNKSIZEXZ + z * SubChunk.CHUNKSIZEXZ * SubChunk.CHUNKSIZEXZ;
        this.blocks[index] = (byte) type.id;
        this.dirty = true;

        this.parent.updateHeightMapZoneAt(x, z);   
        this.rebuildSubChunk();

        if (x == SubChunk.CHUNKSIZEXZ - 1 && this.getNeighborWest() != null) { 
            this.getNeighborWest().rebuildSubChunk(); 
        }
        if (x == 0 && this.getNeighborEast() != null) { 
            this.getNeighborEast().rebuildSubChunk(); 
        }
        if (z == SubChunk.CHUNKSIZEXZ - 1 && this.getNeighborNorth() != null) { 
            this.getNeighborNorth().rebuildSubChunk(); 
        }
        if (z == 0 && this.getNeighborSouth() != null) { 
            this.getNeighborSouth().rebuildSubChunk(); 
        }
        if (y == 0 && this.getNeighborDown() != null) { 
            this.getNeighborDown().rebuildSubChunk(); 
        }
        if (y == SubChunk.CHUNKSIZEXZ - 1 && this.getNeighborUp() != null) { 
            this.getNeighborUp().rebuildSubChunk(); 
        }        
    }
    
    /**
     * Retire un block dans le subChunk <br>
     * (coordonnées locales du subChunk)
     * @param x int
     * @param y int
     * @param z int
     */
    @ChunkThread
    public void removeBlockFromSubChunk(int x, int y, int z){
        if (x < 0 || x >= SubChunk.CHUNKSIZEXZ ||
            y < 0 || y >= SubChunk.CHUNKSIZEXZ ||
            z < 0 || z >= SubChunk.CHUNKSIZEXZ
        ){throw new RuntimeException("Coordonées dans le subChunk invalide");}
        int index = x + y * SubChunk.CHUNKSIZEXZ + z * SubChunk.CHUNKSIZEXZ * SubChunk.CHUNKSIZEXZ;
        this.blocks[index] = (byte) Blocks.AIR.id;
        this.dirty = true;
        
        this.parent.updateHeightMapZoneAt(x, z);
        this.rebuildSubChunk();

        if (x == SubChunk.CHUNKSIZEXZ - 1 && this.getNeighborWest() != null) { 
            this.getNeighborWest().rebuildSubChunk(); 
        }
        if (x == 0 && this.getNeighborEast() != null) { 
            this.getNeighborEast().rebuildSubChunk(); 
        }
        if (z == SubChunk.CHUNKSIZEXZ - 1 && this.getNeighborNorth() != null) { 
            this.getNeighborNorth().rebuildSubChunk(); 
        }
        if (z == 0 && this.getNeighborSouth() != null) { 
            this.getNeighborSouth().rebuildSubChunk(); 
        }
        if (y == 0 && this.getNeighborDown() != null) { 
            this.getNeighborDown().rebuildSubChunk(); 
        }
        if (y == SubChunk.CHUNKSIZEXZ - 1 && this.getNeighborUp() != null) { 
            this.getNeighborUp().rebuildSubChunk(); 
        }        
    }

    @ChunkThread
    public void rebuildSubChunk(){
        this.linkNeighbors();
        this.fillSubChunkBitSet();
        this.mesh.setupSubChunkBuffers(this.bitSet.cardinality());
        this.generateSubChunkMesh();
        
        // met le thread en pause si l'array est plein, le thread se relance automatiquement au moment ou OpenGLThread poll un chunk
        //Render.dirtySubChunks.offer(this);
        if (this.mesh.getVertexCount() > 0) {   // n'envoie pas les subchunks vides au GPU
            try { Render.dirtySubChunks.put(this); } catch(InterruptedException e) { Thread.currentThread().interrupt(); }
        }
    }

    /**
     * Envoie les quads à dessiné avec le face cullings
     */
    @ChunkThread
    private void uploadSubChunkMesh(byte blockType){

        for(Quad quad : this.quads){
            BlockDefinition type = BlocksManager.get(blockType);
            if(type == Blocks.NULL) continue;
            int textureId =  type.getTextureLayer(quad.direction);
            this.mesh.addQuadVertexToArray(quad, textureId);
        }        
    }

    /**
     * Comme son nom l'indique
     * Calcule aussi avec les faces des chunks voisins
     */
    @ChunkThread
    private void calculateSubChunkFaceCulling(FaceData faceData){
        Arrays.fill(faceData.northAxis, 0); Arrays.fill(faceData.southAxis, 0);        
        Arrays.fill(faceData.westAxis,  0); Arrays.fill(faceData.eastAxis,  0);
        Arrays.fill(faceData.upAxis,    0); Arrays.fill(faceData.downAxis,  0);

        // Récup nord sud
        for (int y = 0; y < SubChunk.CHUNKSIZEXZ; y++){
            for (int x = 0; x < SubChunk.CHUNKSIZEXZ; x++){
                int index = y + x * SubChunk.CHUNKSIZEXZ;
                for (int z = 0; z < SubChunk.CHUNKSIZEXZ; z++){
                    int bitSetIndex = x + y * SubChunk.CHUNKSIZEXZ + z * SubChunk.CHUNKSIZEXZ * SubChunk.CHUNKSIZEXZ;
                    faceData.northAxis[index] |= (this.bitSet.get(bitSetIndex) ? 1 : 0) << (31 - z);
                }
                int ligne = faceData.northAxis[index];

                int ligneNeighborSouth = 0;
                if (this.getNeighborSouth() != null){
                    int bitSetIndex = x + y * SubChunk.CHUNKSIZEXZ + 15 * SubChunk.CHUNKSIZEXZ * SubChunk.CHUNKSIZEXZ;
                    ligneNeighborSouth = (this.getNeighborSouth().bitSet.get(bitSetIndex) ? 1 : 0);
                }
                ligneNeighborSouth = ligneNeighborSouth << 31;     

                int ligneNeighborNorth = 0;
                if (this.getNeighborNorth() != null){
                    int bitSetIndex = x + y * SubChunk.CHUNKSIZEXZ + 0 * SubChunk.CHUNKSIZEXZ * SubChunk.CHUNKSIZEXZ;
                    ligneNeighborNorth = (this.getNeighborNorth().bitSet.get(bitSetIndex) ? 1 : 0);
                }
                ligneNeighborNorth = ligneNeighborNorth << 16;  

                faceData.southAxis[index] = ligne & ~((ligne >>> 1) | ligneNeighborSouth); // faces vers le sud  
                faceData.northAxis[index] = ligne & ~((ligne << 1) | ligneNeighborNorth); // faces vers le nord
            }
        }
        // Récup ouest est, on remplit l'int qui correspond a la ligne avec le BitSet du subChunk
        for (int y = 0; y < SubChunk.CHUNKSIZEXZ; y++){
            for (int z = 0; z < SubChunk.CHUNKSIZEXZ; z++){
                int index = y + z * SubChunk.CHUNKSIZEXZ;      
                for (int x = 0; x < SubChunk.CHUNKSIZEXZ; x++){
                    int bitSetIndex = x + y * SubChunk.CHUNKSIZEXZ + z * SubChunk.CHUNKSIZEXZ * SubChunk.CHUNKSIZEXZ;                    
                    faceData.eastAxis[index] |= (this.bitSet.get(bitSetIndex) ? 1 : 0) << (31 - x);                    
                }
                int ligne = faceData.eastAxis[index];
                
                // Est : récupère les bits les plus à droite du voisin est (x = 15)
                int ligneNeighborEast = 0;
                // si il y a un voisin
                if (this.getNeighborEast() != null){
                    int bitSetIndex = 15 + y * SubChunk.CHUNKSIZEXZ + z * SubChunk.CHUNKSIZEXZ * SubChunk.CHUNKSIZEXZ;
                    // on récupère la valeur du bitSet à l'index
                    // met le dernier bit a 1
                    ligneNeighborEast = (this.getNeighborEast().bitSet.get(bitSetIndex) ? 1 : 0);
                }                                 
                // si on récup le bit le plus a droite du voisin il faut le mettre le plus a gauche pour le subChunk actuel
                ligneNeighborEast = ligneNeighborEast << 31;

                int ligneNeighborWest = 0;
                if (this.getNeighborWest() != null){
                    int bitSetIndex = 0 + y * SubChunk.CHUNKSIZEXZ + z * SubChunk.CHUNKSIZEXZ * SubChunk.CHUNKSIZEXZ;
                    ligneNeighborWest = (this.getNeighborWest().bitSet.get(bitSetIndex) ? 1 : 0);
                }   
                // si on récup le bit le plus a droite du voisin il faut le mettre le plus a droite par rapport a la ligne donc au milieu de l'int pour le subChunk actuel
                ligneNeighborWest = ligneNeighborWest << 16;

                // le >> ajoute un 1 au début le >>> ajoute un 0
                faceData.eastAxis[index] = ligne & ~((ligne >>> 1) | ligneNeighborEast); // faces vers l'est + le bloc voisin
                faceData.westAxis[index] = ligne & ~((ligne << 1) | ligneNeighborWest); // faces vers l'ouest + le bloc voisin     
            }
        }
        // Récup haut bas
        for (int z = 0; z < SubChunk.CHUNKSIZEXZ; z++){
            for (int x = 0; x < SubChunk.CHUNKSIZEXZ; x++){
                int index = z + x * SubChunk.CHUNKSIZEXZ;
                for (int y = 0; y < SubChunk.CHUNKSIZEXZ; y++){
                    int bitSetIndex = x + y * SubChunk.CHUNKSIZEXZ + z * SubChunk.CHUNKSIZEXZ * SubChunk.CHUNKSIZEXZ;
                    faceData.upAxis[index] |= (this.bitSet.get(bitSetIndex) ? 1 : 0) << (31 - y);
                }
                int ligne = faceData.upAxis[index];
                int ligneNeighborUp = 0;
                if (this.getNeighborUp() != null){
                    int bitSetIndex = x + 0 * SubChunk.CHUNKSIZEXZ + z * SubChunk.CHUNKSIZEXZ * SubChunk.CHUNKSIZEXZ;
                    ligneNeighborUp = (this.getNeighborUp().bitSet.get(bitSetIndex) ? 1 : 0);
                }
                ligneNeighborUp = ligneNeighborUp << 16;
                int ligneNeighborDown = 0;
                if (this.getNeighborDown() != null){
                    int bitSetIndex = x + 15 * SubChunk.CHUNKSIZEXZ + z * SubChunk.CHUNKSIZEXZ * SubChunk.CHUNKSIZEXZ;
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
        Arrays.fill(faceData.northMask, 0); Arrays.fill(faceData.southMask, 0);
        Arrays.fill(faceData.westMask,  0); Arrays.fill(faceData.eastMask,  0);
        Arrays.fill(faceData.upMask,    0); Arrays.fill(faceData.downMask,  0);
        
        Arrays.fill(faceData.northAoMask, (byte)0); Arrays.fill(faceData.southAoMask, (byte)0);
        Arrays.fill(faceData.westAoMask,  (byte)0); Arrays.fill(faceData.eastAoMask,  (byte)0);
        Arrays.fill(faceData.upAoMask,    (byte)0); Arrays.fill(faceData.downAoMask,  (byte)0);

        Arrays.fill(faceData.northSkyLightMask, (byte)0); Arrays.fill(faceData.southSkyLightMask, (byte)0);
        Arrays.fill(faceData.westSkyLightMask,  (byte)0); Arrays.fill(faceData.eastSkyLightMask,  (byte)0);
        Arrays.fill(faceData.upSkyLightMask,    (byte)0); Arrays.fill(faceData.downSkyLightMask,  (byte)0);
        
        Arrays.fill(faceData.northBlockLightMask, (byte)0); Arrays.fill(faceData.southBlockLightMask, (byte)0);
        Arrays.fill(faceData.westBlockLightMask,  (byte)0); Arrays.fill(faceData.eastBlockLightMask,  (byte)0);
        Arrays.fill(faceData.upBlockLightMask,    (byte)0); Arrays.fill(faceData.downBlockLightMask,  (byte)0);
        
        // rempli les masques avec les infos du face culling
        for (int y = 0; y < SubChunk.CHUNKSIZEXZ; y++){
            for (int x = 0; x < SubChunk.CHUNKSIZEXZ; x++){
                for (int z = 0; z < SubChunk.CHUNKSIZEXZ; z++){                    
                    int ligne = y + z * SubChunk.CHUNKSIZEXZ;

                    int blockIndex = x + y * SubChunk.CHUNKSIZEXZ + z * SubChunk.CHUNKSIZEXZ * SubChunk.CHUNKSIZEXZ;
                    if(this.blocks[blockIndex] != blockType) continue;

                    // En gros je récup tous les bits d'un int d'Axis, que je met en 1er position des int de mask a chaque boucle
                    int northBit = (faceData.northAxis[y + x * SubChunk.CHUNKSIZEXZ] >>> (31 - z)) & 1; // décalle de 31 bits + z vers la droite puis récupère le 1er bit
                    faceData.northMask[ligne] |= (northBit << (31 - x));
                    // si c'est un block solide alors on calcule/applique la lumière dessus
                    if(northBit == 1){
                        byte ao0 = computeAO(x-1, y, z+1,   x, y-1, z+1,   x-1, y-1, z+1);
                        byte ao1 = computeAO(x+1, y, z+1,   x, y-1, z+1,   x+1, y-1, z+1);
                        byte ao2 = computeAO(x-1, y, z+1,   x, y+1, z+1,   x-1, y+1, z+1);
                        byte ao3 = computeAO(x+1, y, z+1,   x, y+1, z+1,   x+1, y+1, z+1);
                        faceData.northAoMask[ligne * SubChunk.CHUNKSIZEXZ + x] = (byte)(ao0|(ao1<<2)|(ao2<<4)|(ao3<<6));
                        faceData.northSkyLightMask[ligne * SubChunk.CHUNKSIZEXZ + x] = (byte)this.getBlockSkyLight(x, y, z+1);
                        faceData.northBlockLightMask[ligne * SubChunk.CHUNKSIZEXZ + x] = (byte)this.getBlockLight(x, y, z+1);                        
                    }
                    
                    int southBit = (faceData.southAxis[y + x * SubChunk.CHUNKSIZEXZ] >> (31 - z)) & 1; // décalle de 31 bits - z vers la droite puis récupère le 1er bit*
                    faceData.southMask[ligne] |= (southBit << (31 - x));
                    if(southBit == 1){
                        byte ao0 = computeAO(x-1, y, z-1,   x, y-1, z-1,   x-1, y-1, z-1);
                        byte ao1 = computeAO(x+1, y, z-1,   x, y-1, z-1,   x+1, y-1, z-1);
                        byte ao2 = computeAO(x-1, y, z-1,   x, y+1, z-1,   x-1, y+1, z-1);
                        byte ao3 = computeAO(x+1, y, z-1,   x, y+1, z-1,   x+1, y+1, z-1);
                        faceData.southAoMask[ligne * SubChunk.CHUNKSIZEXZ + x] = (byte)(ao0|(ao1<<2)|(ao2<<4)|(ao3<<6));

                        faceData.southSkyLightMask[ligne * SubChunk.CHUNKSIZEXZ + x] = (byte)this.getBlockSkyLight(x, y, z-1);
                        faceData.southBlockLightMask[ligne * SubChunk.CHUNKSIZEXZ + x] = (byte)this.getBlockLight(x, y, z-1);
                    }                    
                }                
            }            
        }
        for (int y = 0; y < SubChunk.CHUNKSIZEXZ; y++){
            for (int z = 0; z < SubChunk.CHUNKSIZEXZ; z++){                
                for (int x = 0; x < SubChunk.CHUNKSIZEXZ; x++){                    
                    int ligne = y + x * SubChunk.CHUNKSIZEXZ;

                    // filtre par type
                    int blockIndex = x + y * SubChunk.CHUNKSIZEXZ + z * SubChunk.CHUNKSIZEXZ * SubChunk.CHUNKSIZEXZ;
                    if(this.blocks[blockIndex] != blockType) continue;

                    int westBit = (faceData.westAxis[y + z * SubChunk.CHUNKSIZEXZ] >>> (31 - x)) & 1;
                    faceData.westMask[ligne] |= (westBit << (31 - z));  
                    if(westBit == 1){
                        byte ao0 = computeAO(x+1, y, z-1,   x+1, y-1, z,   x+1, y-1, z-1);
                        byte ao1 = computeAO(x+1, y, z+1,   x+1, y-1, z,   x+1, y-1, z+1);
                        byte ao2 = computeAO(x+1, y, z-1,   x+1, y+1, z,   x+1, y+1, z-1);
                        byte ao3 = computeAO(x+1, y, z+1,   x+1, y+1, z,   x+1, y+1, z+1);
                        faceData.westAoMask[ligne * SubChunk.CHUNKSIZEXZ + z] = (byte)(ao0|(ao1<<2)|(ao2<<4)|(ao3<<6));

                        faceData.westSkyLightMask[ligne * SubChunk.CHUNKSIZEXZ + z] = (byte)this.getBlockSkyLight(x+1, y, z);
                        faceData.westBlockLightMask[ligne * SubChunk.CHUNKSIZEXZ + z] = (byte)this.getBlockLight(x+1, y, z);
                    }

                    int eastBit = (faceData.eastAxis[y + z * SubChunk.CHUNKSIZEXZ] >> (31 - x)) & 1;
                    faceData.eastMask[ligne] |= (eastBit << (31 - z));
                    if(eastBit == 1){
                        byte ao0 = computeAO(x-1, y, z-1,   x-1, y-1, z,   x-1, y-1, z-1);
                        byte ao1 = computeAO(x-1, y, z+1,   x-1, y-1, z,   x-1, y-1, z+1);
                        byte ao2 = computeAO(x-1, y, z-1,   x-1, y+1, z,   x-1, y+1, z-1);
                        byte ao3 = computeAO(x-1, y, z+1,   x-1, y+1, z,   x-1, y+1, z+1);                        
                        faceData.eastAoMask[ligne * SubChunk.CHUNKSIZEXZ + z] = (byte)(ao0|(ao1<<2)|(ao2<<4)|(ao3<<6));

                        faceData.eastSkyLightMask[ligne * SubChunk.CHUNKSIZEXZ + z] = (byte)this.getBlockSkyLight(x-1, y, z);
                        faceData.eastBlockLightMask[ligne * SubChunk.CHUNKSIZEXZ + z] = (byte)this.getBlockLight(x-1, y, z);
                    }
                }
            }
        }    
                  
        for (int y = 0; y < SubChunk.CHUNKSIZEXZ; y++){
            for (int z = 0; z < SubChunk.CHUNKSIZEXZ; z++){
                for (int x = 0; x < SubChunk.CHUNKSIZEXZ; x++){
                    int ligne = z + y * SubChunk.CHUNKSIZEXZ;

                    int blockIndex = x + y * SubChunk.CHUNKSIZEXZ + z * SubChunk.CHUNKSIZEXZ * SubChunk.CHUNKSIZEXZ;
                    if(this.blocks[blockIndex] != blockType) continue;

                    int upBit = (faceData.upAxis[z + x * SubChunk.CHUNKSIZEXZ] >>> (31 - y)) & 1;
                    faceData.upMask[ligne] |= (upBit << (31 - x));
                    if(upBit == 1){
                        byte ao0 = computeAO(x-1, y+1, z,   x, y+1, z-1,   x-1, y+1, z-1);
                        byte ao1 = computeAO(x+1, y+1, z,   x, y+1, z-1,   x+1, y+1, z-1);
                        byte ao2 = computeAO(x-1, y+1, z,   x, y+1, z+1,   x-1, y+1, z+1);
                        byte ao3 = computeAO(x+1, y+1, z,   x, y+1, z+1,   x+1, y+1, z+1);
                        faceData.upAoMask[ligne * SubChunk.CHUNKSIZEXZ + x] = (byte)(ao0|(ao1<<2)|(ao2<<4)|(ao3<<6));

                        faceData.upSkyLightMask[ligne * SubChunk.CHUNKSIZEXZ + x] = (byte)this.getBlockSkyLight(x, y+1, z);
                        faceData.upBlockLightMask[ligne * SubChunk.CHUNKSIZEXZ + x] = (byte)this.getBlockLight(x, y+1, z);
                    }

                    int downBit = (faceData.downAxis[z + x * SubChunk.CHUNKSIZEXZ] >>> (31 - y)) & 1;
                    faceData.downMask[ligne] |= (downBit << (31 - x));
                    if(downBit == 1){
                        byte ao0 = computeAO(x-1, y-1, z,   x, y-1, z-1,   x-1, y-1, z-1);
                        byte ao1 = computeAO(x+1, y-1, z,   x, y-1, z-1,   x+1, y-1, z-1);
                        byte ao2 = computeAO(x-1, y-1, z,   x, y-1, z+1,   x-1, y-1, z+1);
                        byte ao3 = computeAO(x+1, y-1, z,   x, y-1, z+1,   x+1, y-1, z+1);
                        faceData.downAoMask[ligne * SubChunk.CHUNKSIZEXZ + x] = (byte)(ao0|(ao1<<2)|(ao2<<4)|(ao3<<6));

                        faceData.downSkyLightMask[ligne * SubChunk.CHUNKSIZEXZ + x] = (byte)this.getBlockSkyLight(x, y-1, z);
                        faceData.downBlockLightMask[ligne * SubChunk.CHUNKSIZEXZ + x] = (byte)this.getBlockLight(x, y-1, z);
                    }
                }
            }        
        }

        this.greedyMeshingPerFace(faceData.northMask, faceData.northAoMask, faceData.northSkyLightMask, faceData.northBlockLightMask, "north");
        this.greedyMeshingPerFace(faceData.southMask, faceData.southAoMask, faceData.southSkyLightMask, faceData.southBlockLightMask, "south");
        this.greedyMeshingPerFace(faceData.westMask,  faceData.westAoMask,  faceData.westSkyLightMask,  faceData.westBlockLightMask,  "west");
        this.greedyMeshingPerFace(faceData.eastMask,  faceData.eastAoMask,  faceData.eastSkyLightMask,  faceData.eastBlockLightMask,  "east");
        this.greedyMeshingPerFace(faceData.upMask,    faceData.upAoMask,    faceData.upSkyLightMask,    faceData.upBlockLightMask,    "up");
        this.greedyMeshingPerFace(faceData.downMask,  faceData.downAoMask,  faceData.downSkyLightMask,  faceData.downBlockLightMask,  "down");
    }

    // fait le greedy pour chaque face
    private void greedyMeshingPerFace(int[] faceMask, byte[] faceAoMask, byte[] faceSkyLightMask, byte[] faceBlockLightMask, String dir){        
        for(int ligne = 0; ligne < faceMask.length; ligne++){
            int i = 0; // indice en largeur byte car dépasse pas 15
            while(i < SubChunk.CHUNKSIZEXZ){              
                if (faceMask[ligne] == 0) break; // si il n'y a pas de face           
                int j = 1; // indice en longueur/hauteur

                int[] coord = new int[3]; // point de départ, fin quad en x, fin quad en y
                while((i < SubChunk.CHUNKSIZEXZ) && ((faceMask[ligne] >>> (31 - i)) & 1) != 1){ // tant que l'on a pas trouvé la 1er face pleine
                    i++;
                }

                // condition de fusion en largeur
                coord[0] = i;// x commence à i
                while((i < SubChunk.CHUNKSIZEXZ) && ((faceMask[ligne] >>> (31 - i)) & 1) != 0
                    && faceAoMask[ligne * SubChunk.CHUNKSIZEXZ + i]         == faceAoMask[ligne * SubChunk.CHUNKSIZEXZ + coord[0]]
                    && faceSkyLightMask[ligne * SubChunk.CHUNKSIZEXZ + i]   == faceSkyLightMask[ligne * SubChunk.CHUNKSIZEXZ + coord[0]]
                    && faceBlockLightMask[ligne * SubChunk.CHUNKSIZEXZ + i] == faceBlockLightMask[ligne * SubChunk.CHUNKSIZEXZ + coord[0]]){
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
                int sliceStart = (ligne / SubChunk.CHUNKSIZEXZ) * SubChunk.CHUNKSIZEXZ;
                while((ligne + j < sliceStart + SubChunk.CHUNKSIZEXZ)
                    && (ligne + j < faceMask.length) 
                    && (faceMask[ligne + j] & mask) == mask){

                    // condition de fusion en hauteur
                    boolean match = true;
                    for(int k = coord[0]; k < coord[1]; k++){
                        if(faceAoMask[(ligne+j) * SubChunk.CHUNKSIZEXZ + k]         != faceAoMask[ligne * SubChunk.CHUNKSIZEXZ + k] // même valeur d'AO
                        || faceSkyLightMask[(ligne+j) * SubChunk.CHUNKSIZEXZ + k]   != faceSkyLightMask[ligne * SubChunk.CHUNKSIZEXZ + k] // même valeur de lumière du ciel
                        || faceBlockLightMask[(ligne+j) * SubChunk.CHUNKSIZEXZ + k] != faceBlockLightMask[ligne * SubChunk.CHUNKSIZEXZ + k]){ // même valeur de lumière du block
                            match = false;
                            break;
                        }
                    }
                    if(!match) break;
                    faceMask[ligne + j] = faceMask[ligne + j] & ~mask;
                    j++;
                }
                // le quad descent de j
                coord[2] = j;       

                if (coord[0] < coord[1]){
                    int qx, qy, qz;
                    switch(dir){
                        case "north": case "south":
                            // masque index = y + z * CHUNKSIZEXZ, bit = X
                            qx = coord[0];
                            qy = ligne % SubChunk.CHUNKSIZEXZ;
                            qz = ligne / SubChunk.CHUNKSIZEXZ;
                            break;
                        case "west": case "east":
                            // masque index = y + x * CHUNKSIZEXZ, bit = Z
                            qx = ligne / SubChunk.CHUNKSIZEXZ;
                            qy = ligne % SubChunk.CHUNKSIZEXZ;
                            qz = coord[0];
                            break;
                        case "up": case "down":
                            // masque index = y + z * CHUNKSIZEXZ, bit = X
                            qx = coord[0];
                            qy = ligne / SubChunk.CHUNKSIZEXZ;  
                            qz = ligne % SubChunk.CHUNKSIZEXZ; 
                            break;
                        default:
                            qx = qy = qz = 0;
                    }

                    int w = coord[1] - coord[0];
                    int h = coord[2];
                    
                    int packedAoBottomFirst = faceAoMask[ligne                   * SubChunk.CHUNKSIZEXZ + coord[0]    ];
                    int packedAoBottomLast  = faceAoMask[ligne                   * SubChunk.CHUNKSIZEXZ + (coord[1]-1)];
                    int packedAoTopFirst    = faceAoMask[(ligne + h - 1)         * SubChunk.CHUNKSIZEXZ + coord[0]    ];
                    int packedAoTopLast     = faceAoMask[(ligne + h - 1)         * SubChunk.CHUNKSIZEXZ + (coord[1]-1)];

                    int ao0 = ( packedAoBottomFirst        & 0x3); // bas-gauche
                    int ao1 = ((packedAoBottomLast  >> 2)  & 0x3); // bas-droite  <- dernier bloc
                    int ao2 = ((packedAoTopFirst    >> 4)  & 0x3); // haut-gauche <- ligne du haut
                    int ao3 = ((packedAoTopLast     >> 6)  & 0x3); // haut-droite <- dernier bloc, ligne du haut    
                    
                    // On utilise & 0xFF pour transformer le byte signé (-128 à 127) en int non-signé (0 à 255)
                    int skyLight   = faceSkyLightMask  [ligne * SubChunk.CHUNKSIZEXZ + coord[0]];
                    int blockLight = faceBlockLightMask[ligne * SubChunk.CHUNKSIZEXZ + coord[0]];

                    this.quads.add(new Quad(qx, qy, qz, w, h, dir, ao0, ao1, ao2, ao3, skyLight, blockLight));
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
    public boolean isBlockSolid(int x, int y, int z){
        // dans le subchunk courant
        if (x >= 0 && x < CHUNKSIZEXZ && y >= 0 && y < CHUNKSIZEXZ && z >= 0 && z < CHUNKSIZEXZ){
            int index = x + y * SubChunk.CHUNKSIZEXZ + z * SubChunk.CHUNKSIZEXZ * SubChunk.CHUNKSIZEXZ;
            return this.blocks[index] != (byte)Blocks.AIR.id;
        }
        // en dehors délègue aux voisins, blocks en bordure
        if (y >= CHUNKSIZEXZ && this.upNeighbor != null){
            return this.upNeighbor.isBlockSolid(x, y - CHUNKSIZEXZ, z);
        }
        if (y < 0 && this.downNeighbor != null){
            return this.downNeighbor.isBlockSolid(x, y + CHUNKSIZEXZ, z);
        }
        if (x >= CHUNKSIZEXZ && this.westNeighbor != null){
            return this.westNeighbor.isBlockSolid(x - CHUNKSIZEXZ, y, z);
        }
        if (x < 0 && this.eastNeighbor != null){
            return this.eastNeighbor.isBlockSolid(x + CHUNKSIZEXZ, y, z);
        }
        if (z >= CHUNKSIZEXZ && this.northNeighbor != null){
            return this.northNeighbor.isBlockSolid(x, y, z - CHUNKSIZEXZ);
        }
        if (z < 0 && this.southNeighbor != null){
            return this.southNeighbor.isBlockSolid(x, y, z + CHUNKSIZEXZ);
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

        if (s1 && s2) {
            return 3;
        }
        return (byte)((s1 ? 1 : 0) + (s2 ? 1 : 0) + (c ? 1 : 0));
    }


    /**
     * Remplis le BitSet du subChunk (cube plein = 1, cube vide = 0)
     */
    @ChunkThread
    public void fillSubChunkBitSet(){
        this.bitSet.clear();
        for(int i = 0; i < SubChunk.MAXBLOCK; i++){      
            // les blocks transparent ne doivent pas être face cull, logique   
            // BlocksManager.get(this.blocks[i]).transparent == false
            if (this.blocks[i] != Blocks.AIR.id){
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
            Chunk chunk = ChunksManager.getChunkAt(
                this.x + SubChunk.CHUNKSIZEXZ,
                this.z);
            this.westNeighbor = chunk.getSubChunkAt(this.y);
        }catch(Exception e){
            this.westNeighbor = null;
        }  
        // est
        try{
            Chunk chunk = ChunksManager.getChunkAt(
                this.x - SubChunk.CHUNKSIZEXZ,
                this.z);
            this.eastNeighbor = chunk.getSubChunkAt(this.y);
        }catch(Exception e){
            this.eastNeighbor = null;
        }   
        // nord
        try{
            Chunk chunk = ChunksManager.getChunkAt(
                this.x,
                this.z + SubChunk.CHUNKSIZEXZ);
            this.northNeighbor = chunk.getSubChunkAt(this.y);
        }catch(Exception e){
            this.northNeighbor = null;
        }   
        // sud
        try{
            Chunk chunk = ChunksManager.getChunkAt(
                this.x,
                this.z - SubChunk.CHUNKSIZEXZ);
            this.southNeighbor = chunk.getSubChunkAt(this.y);
        }catch(Exception e){
            this.southNeighbor = null;
        } 
        // haut
        try{
            this.upNeighbor = parent.getSubChunkAt(this.y + SubChunk.CHUNKSIZEXZ);
        }catch(Exception e){
            this.upNeighbor = null;
        }  
        // bas
        try{
            this.downNeighbor = parent.getSubChunkAt(this.y - SubChunk.CHUNKSIZEXZ);
        }catch(Exception e){
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
