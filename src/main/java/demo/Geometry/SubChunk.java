package demo.Geometry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.HashSet;

import org.joml.Matrix4f;

import demo.Annotations.ChunkThread;
import demo.Annotations.OpenGLThread;
import demo.Events.KeyboardInput;
import demo.Managers.BlocksManager;
import demo.Managers.ChunksManager;
import demo.Managers.LightManager;
import demo.Managers.LiquidManager;
import demo.Managers.ShadersManager;
import demo.Meshing.SubChunkMesh;
import demo.Others.BlockDefinition;
import demo.Others.Blocks;
import demo.Meshing.Quad;
import demo.Rendering.Render;

/**
 * Un sous-chunk (portion de 16x16x16 blocs d'un {@link Chunk}).
 * Contient les blocs, la lumière, le liquide et les meshes (opaque, transparent, verre, liquide)
 * de cette portion du monde.
 */
public class SubChunk extends Node3D{

    /**
     * Taille horizontale (X et Z) d'un sous-chunk, en blocs.
     */
    public static final int CHUNKSIZEXZ = 16;
    /**
     * Hauteur totale (en blocs) d'un chunk complet, cumulant tous ses sous-chunks.
     */
    public static final int CHUNKSIZEHEIGHT = 256;
    /**
     * Nombre total de blocs contenus dans un sous-chunk (16 x 16 x 16).
     */
    public static final int MAXBLOCK = 4096;    

    /**
     * Matrice de transformation réutilisée pour le rendu du sous-chunk.
     */
    private final Matrix4f model = new Matrix4f();

    /**
     * Sous-chunk voisin à l'ouest (x décroissant), ou null s'il n'est pas chargé.
     */
    private SubChunk westNeighbor;
    /**
     * Sous-chunk voisin à l'est (x croissant), ou null s'il n'est pas chargé.
     */
    private SubChunk eastNeighbor;
    /**
     * Sous-chunk voisin au nord (z croissant), ou null s'il n'est pas chargé.
     */
    private SubChunk northNeighbor;
    /**
     * Sous-chunk voisin au sud (z décroissant), ou null s'il n'est pas chargé.
     */
    private SubChunk southNeighbor;
    /**
     * Sous-chunk voisin au dessus (y croissant), ou null s'il n'existe pas.
     */
    private SubChunk upNeighbor;
    /**
     * Sous-chunk voisin en dessous (y décroissant), ou null s'il n'existe pas.
     */
    private SubChunk downNeighbor;

    /**
     * Mesh des blocs opaques du sous-chunk.
     */
    // donne au mesh le subChunk au quel il appartient
    public SubChunkMesh mesh = new SubChunkMesh();
    /**
     * Mesh des blocs transparents non-verre (ex: feuilles) du sous-chunk.
     */
    public SubChunkMesh transparentMesh = new SubChunkMesh();    
    /**
     * Mesh des blocs de verre du sous-chunk.
     */
    public SubChunkMesh glassMesh = new SubChunkMesh();
    /**
     * Mesh des blocs liquides du sous-chunk.
     */
    public SubChunkMesh liquidMesh = new SubChunkMesh();

    /**
     * Id des blocs du sous-chunk (256 types de blocs différents maximum car stocké sur 8 bits).
     */
    public byte[] blocks; // id des blocs (MAX 256 blocs différent car 8bits)

    /**
     * Données compactées par bloc (lumière du ciel, lumière de bloc, niveau de liquide, source de liquide) :
     * <pre>
     * [ 0000  0000  0000  0     000  0000  0000  0000 ]
     *   sky   block liqud liqud
     *   light light level src ?
     * </pre>
     */
    public AtomicIntegerArray blocksData;
    
    /**
     * Indique si le sous-chunk a été modifié et doit être reconstruit (mesh à régénérer).
     * AtomicBoolean garantit que le lire + écrire se fait en une seule instruction CPU indivisible.
     */
    public AtomicBoolean dirty = new AtomicBoolean(false);

    /**
     * Chunk parent auquel appartient ce sous-chunk.
     */
    public Chunk parent;

    /**
     * Bit à 1 si le bloc à l'index correspondant est plein (non-air), 0 si vide (air).
     */
    public BitSet solidBitSet = new BitSet(SubChunk.MAXBLOCK); 
    /**
     * Bit à 1 si le bloc à l'index correspondant est transparent (ex: vitre), 0 sinon.
     */
    public BitSet transparentBitSet = new BitSet(SubChunk.MAXBLOCK);

    /**
     * Mode de face culling : blocs opaques.
     */
    private static final int MODE_OPAQUE      = 0;
    /**
     * Mode de face culling : blocs de verre.
     */
    private static final int MODE_GLASS       = 1;
    /**
     * Mode de face culling : blocs transparents non-verre.
     */
    private static final int MODE_TRANSPARENT = 2;

    /**
     * Ensemble des types de blocs uniques présents dans le sous-chunk, recalculé à chaque génération de mesh.
     */
    private final Set<Byte> blockTypes = new HashSet<>();

    /**
     * Regroupe, pour chacune des 6 directions, le masque de bits des faces visibles du sous-chunk.
     * Utilisé comme tampon de calcul pendant le face culling.
     */
    private static class FaceData {
        /** Masque des faces visibles orientées nord. */
        int[] northAxis = new int[SubChunk.CHUNKSIZEXZ * SubChunk.CHUNKSIZEXZ];
        /** Masque des faces visibles orientées sud. */
        int[] southAxis = new int[SubChunk.CHUNKSIZEXZ * SubChunk.CHUNKSIZEXZ];
        /** Masque des faces visibles orientées ouest. */
        int[] westAxis  = new int[SubChunk.CHUNKSIZEXZ * SubChunk.CHUNKSIZEXZ];
        /** Masque des faces visibles orientées est. */
        int[] eastAxis  = new int[SubChunk.CHUNKSIZEXZ * SubChunk.CHUNKSIZEXZ];
        /** Masque des faces visibles orientées haut. */
        int[] upAxis    = new int[SubChunk.CHUNKSIZEXZ * SubChunk.CHUNKSIZEXZ];
        /** Masque des faces visibles orientées bas. */
        int[] downAxis  = new int[SubChunk.CHUNKSIZEXZ * SubChunk.CHUNKSIZEXZ];
    }

    // obligé sinon les threads qui construisent les chunks pour modifié les infos
    // Thread A écrit dans northAxis[0], Thread B écrit dans northAxis[0] au même moment -> les données se corrompent mutuellement
    // c'est pour cela qu'un threadlocal, va créer une instance de FACE_DATA_LOCAL sur chaque thread
    /**
     * FaceData local au thread courant pour le calcul de face culling des blocs opaques (thread-safe, zéro allocation).
     */
    private static final ThreadLocal<FaceData> OPAQUE_FACE_DATA_LOCAL      = ThreadLocal.withInitial(() -> new FaceData());
    /**
     * FaceData local au thread courant pour le calcul de face culling des blocs transparents (thread-safe, zéro allocation).
     */
    private static final ThreadLocal<FaceData> TRANSPARENT_FACE_DATA_LOCAL = ThreadLocal.withInitial(() -> new FaceData());
    /**
     * FaceData local au thread courant pour le calcul de face culling du verre (thread-safe, zéro allocation).
     */
    private static final ThreadLocal<FaceData> GLASS_FACE_DATA_LOCAL       = ThreadLocal.withInitial(() -> new FaceData());
    /**
     * FaceData local au thread courant pour le calcul de face culling des liquides (thread-safe, zéro allocation).
     */
    private static final ThreadLocal<FaceData> LIQUID_FACE_DATA_LOCAL      = ThreadLocal.withInitial(() -> new FaceData());

    /**
     * Crée un sous-chunk vide rattaché à un chunk parent.
     * @param parent chunk parent auquel appartient ce sous-chunk
     */
    @ChunkThread
    public SubChunk(Chunk parent){
        this.parent = parent;
        this.blocksData = new AtomicIntegerArray(SubChunk.MAXBLOCK); // thread safe
        this.blocks = new byte[SubChunk.MAXBLOCK];   

        // définit la taille du buffer et du tableau de vertex  
        this.mesh.setupSubChunkBuffers(this.solidBitSet.cardinality());   
    }

    /**
     * Retourne le niveau de liquide dans le bloc.
     * @param x Coordonnées en x (locale) 0-15.
     * @param y Coordonnées en y (locale) 0-15.
     * @param z Coordonnées en z (locale) 0-15.
     * @return le liquide (0-15)
     */
    public int getLiquidLevel(int x, int y, int z){         
        int index = x + y * SubChunk.CHUNKSIZEXZ + z * SubChunk.CHUNKSIZEXZ * SubChunk.CHUNKSIZEXZ;

        if (x >= 0 && x < CHUNKSIZEXZ && y >= 0 && y < CHUNKSIZEXZ && z >= 0 && z < CHUNKSIZEXZ)
            { return (this.blocksData.get(index) >>> 20) & 0xF; }
        if (y >= CHUNKSIZEXZ && this.upNeighbor != null)
            { return this.upNeighbor.getLiquidLevel(x, y - CHUNKSIZEXZ, z); }
        if (y < 0 && this.downNeighbor != null)
            { return this.downNeighbor.getLiquidLevel(x, y + CHUNKSIZEXZ, z); }
        if (x >= CHUNKSIZEXZ && this.westNeighbor != null)
            { return this.westNeighbor.getLiquidLevel(x - CHUNKSIZEXZ, y, z); }
        if (x < 0 && this.eastNeighbor != null)
            { return this.eastNeighbor.getLiquidLevel(x + CHUNKSIZEXZ, y, z); }
        if (z >= CHUNKSIZEXZ && this.northNeighbor != null)
            { return this.northNeighbor.getLiquidLevel(x, y, z - CHUNKSIZEXZ); }
        if (z < 0 && this.southNeighbor != null)
            { return this.southNeighbor.getLiquidLevel(x, y, z + CHUNKSIZEXZ); }
        return 0;
    }

    /**
     * Définis le nouveau niveau de liquide dans le bloc.
     * @param x Coordonnées en x (locale) 0-15
     * @param y Coordonnées en y (locale) 0-15
     * @param z Coordonnées en z (locale) 0-15
     * @param newLiquidLevel Nouvelle valeur de liquide (0-15)
     * @throws RuntimeException si newLiquidLevel n'est pas compris entre 0 et 15
     */
    public void setLiquidLevel(int x, int y, int z, int newLiquidLevel){
        if (newLiquidLevel < 0 || newLiquidLevel > 15) throw new RuntimeException("Le liquide doit être comprise entre 0 et 255");
        int index = x + y * SubChunk.CHUNKSIZEXZ + z * SubChunk.CHUNKSIZEXZ * SubChunk.CHUNKSIZEXZ;

        if (x >= 0 && x < CHUNKSIZEXZ && y >= 0 && y < CHUNKSIZEXZ && z >= 0 && z < CHUNKSIZEXZ){
            this.blocksData.getAndUpdate(index, old -> (old & 0xFF0FFFFF) | ((newLiquidLevel & 0xF) << 20));
            return; 
        }
        if (y >= CHUNKSIZEXZ && this.upNeighbor != null)
            { this.upNeighbor.setLiquidLevel(x, y - CHUNKSIZEXZ, z, newLiquidLevel); return; }
        if (y < 0 && this.downNeighbor != null)
            { this.downNeighbor.setLiquidLevel(x, y + CHUNKSIZEXZ, z, newLiquidLevel); return; }
        if (x >= CHUNKSIZEXZ && this.westNeighbor != null)
            { this.westNeighbor.setLiquidLevel(x - CHUNKSIZEXZ, y, z, newLiquidLevel); return; }
        if (x < 0 && this.eastNeighbor != null)
            { this.eastNeighbor.setLiquidLevel(x + CHUNKSIZEXZ, y, z, newLiquidLevel); return; }
        if (z >= CHUNKSIZEXZ && this.northNeighbor != null)
            { this.northNeighbor.setLiquidLevel(x, y, z - CHUNKSIZEXZ, newLiquidLevel); return; }
        if (z < 0 && this.southNeighbor != null)
            { this.southNeighbor.setLiquidLevel(x, y, z + CHUNKSIZEXZ, newLiquidLevel); return; }
    }

    /**
     * Retourne la lumière du ciel sur le bloc.
     * @param x Coordonnées en x (locale) 0-15.
     * @param y Coordonnées en y (locale) 0-15.
     * @param z Coordonnées en z (locale) 0-15.
     * @return la lumière (0 - 15)
     */
    public int getBlockSkyLight(int x, int y, int z){         
        int index = x + y * SubChunk.CHUNKSIZEXZ + z * SubChunk.CHUNKSIZEXZ * SubChunk.CHUNKSIZEXZ;

        if (x >= 0 && x < CHUNKSIZEXZ && y >= 0 && y < CHUNKSIZEXZ && z >= 0 && z < CHUNKSIZEXZ)
            { return (this.blocksData.get(index) >>> 28) & 0xF; } // 32-F(4) = 28
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
     * Retourne la lumière du bloc.
     * @param x Coordonnées en x (locale) 0-15
     * @param y Coordonnées en y (locale) 0-15
     * @param z Coordonnées en z (locale) 0-15
     * @return la lumière (0 - 15)
     */
    public int getBlockLight(int x, int y, int z){
        int index = x + y * SubChunk.CHUNKSIZEXZ + z * SubChunk.CHUNKSIZEXZ * SubChunk.CHUNKSIZEXZ;

        if (x >= 0 && x < CHUNKSIZEXZ && y >= 0 && y < CHUNKSIZEXZ && z >= 0 && z < CHUNKSIZEXZ)
            { return (this.blocksData.get(index) >>> 24) & 0xF; }
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
     * Définis la nouvelle lumière du ciel sur le bloc.
     * @param x Coordonnées en x (locale) 0-15
     * @param y Coordonnées en y (locale) 0-15
     * @param z Coordonnées en z (locale) 0-15
     * @param newLight Nouvelle valeur de lumière
     * @throws RuntimeException si newLight n'est pas compris entre 0 et 15
     */
    public void setBlockSkyLight(int x, int y, int z, int newLight){
        if (newLight < 0 || newLight > 15) throw new RuntimeException("La lumière doit être comprise entre 0 et 15");
        int index = x + y * SubChunk.CHUNKSIZEXZ + z * SubChunk.CHUNKSIZEXZ * SubChunk.CHUNKSIZEXZ;

        if (x >= 0 && x < CHUNKSIZEXZ && y >= 0 && y < CHUNKSIZEXZ && z >= 0 && z < CHUNKSIZEXZ){
            this.blocksData.getAndUpdate(index, old -> (old & 0x0FFFFFFF) | ((newLight & 0xF) << 28));
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
     * Définis la nouvelle lumière du bloc.
     * @param x Coordonnées en x (locale) 0-15
     * @param y Coordonnées en y (locale) 0-15
     * @param z Coordonnées en z (locale) 0-15
     * @param newLight Nouvelle valeur de lumière
     * @throws RuntimeException si newLight n'est pas compris entre 0 et 15
     */
    public void setBlockLight(int x, int y, int z, int newLight){
        if (newLight < 0 || newLight > 15) throw new RuntimeException("La lumière doit être comprise entre 0 et 15");
        int index = x + y * SubChunk.CHUNKSIZEXZ + z * SubChunk.CHUNKSIZEXZ * SubChunk.CHUNKSIZEXZ; 

        if (x >= 0 && x < CHUNKSIZEXZ && y >= 0 && y < CHUNKSIZEXZ && z >= 0 && z < CHUNKSIZEXZ){
            this.blocksData.getAndUpdate(index, old -> (old & 0xF0FFFFFF) | ((newLight & 0xF) << 24));
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

    /**
     * Vide la light map du subChunk
     */
    public void clearSubChunkLight(){
        for (int i = 0; i < this.blocksData.length(); i++){
            this.blocksData.getAndUpdate(i, data -> data & 0x00FFFFFF);
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
     * Génère la maille (mesh) d'un subChunk : calcule le face culling et construit
     * les meshes opaque, verre, transparent et liquide.
     */
    // sync, comme ça peu importe par où le mesh est généré, deux threads ne peuvent jamais être dedans en même temps pour le même subchunk.
    @ChunkThread
    public synchronized void generateSubChunkMesh(){
        List<Quad> quads = new ArrayList<>();

        FaceData opaqueFaceData      = OPAQUE_FACE_DATA_LOCAL.get();
        FaceData glassFaceData       = GLASS_FACE_DATA_LOCAL.get();
        FaceData transparentFaceData = TRANSPARENT_FACE_DATA_LOCAL.get();
        FaceData liquidFaceData      = LIQUID_FACE_DATA_LOCAL.get();

        // Collecte les types uniques
        this.blockTypes.clear();
        for (byte b : this.blocks){
            if (b != (byte)Blocks.AIR.id) this.blockTypes.add(b);
        }

        // Vérifie si on a du verre et/ou des blocs transparents non-verre

        boolean hasGlass = this.blockTypes.contains((byte)Blocks.GLASS.id);  
        boolean hasLiquid = this.blockTypes.contains((byte)Blocks.WATER.id);
        boolean hasTransparent = this.blockTypes.stream()
            .anyMatch(b -> BlocksManager.get(b).transparent && b != (byte)Blocks.GLASS.id && b != (byte)Blocks.WATER.id);

        // Face culling opaque (toujours calculé)
        this.calculateSubChunkFaceCulling(opaqueFaceData, MODE_OPAQUE, quads);
        this.uploadSubChunkMesh(quads);

        
        // Face culling verre : culling verre-verre
        if (hasGlass){
            this.calculateSubChunkFaceCulling(glassFaceData, MODE_GLASS, quads);
            this.uploadSubChunkGlassMesh(quads);
        }

        // Face culling transparent (feuilles etc.) : pas de culling entre eux
        if (hasTransparent){
            this.calculateSubChunkFaceCulling(transparentFaceData, MODE_TRANSPARENT, quads);            
            this.uploadSubChunkTransparentMesh(quads);            
        }

        if (hasLiquid){
            this.calculateSubChunkFaceCulling(liquidFaceData, MODE_TRANSPARENT, quads);
            this.uploadSubChunkLiquidMesh(quads);
        }

        this.mesh.fillSubChunkVertexBufferWithArray();
        this.transparentMesh.fillSubChunkVertexBufferWithArray();
        this.glassMesh.fillSubChunkVertexBufferWithArray();
        this.liquidMesh.fillSubChunkVertexBufferWithArray();
        this.blockTypes.clear();
    }

    /**
     * Retourne le bloc à la position.
     * @param x Coordonnée en x (locale)
     * @param y Coordonnée en y (locale)
     * @param z Coordonnée en z (locale)
     * @return Retourne le bloc
     * @throws RuntimeException si les coordonnées sont en dehors du subChunk
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
     * Retourne tout les bloc du sous chunk
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
        if (this.mesh.getVertexCount() > 0) {
            this.mesh.createSubChunkVBOVAO();
            if (KeyboardInput.isChunkBorderShow && this.mesh.getBorderVAO() == 0){
                this.mesh.createSubChunkBorderVBOVAO();
            }
        }
        if (this.transparentMesh.getVertexCount() > 0) {
            this.transparentMesh.createSubChunkVBOVAO();
        }
        if (this.glassMesh.getVertexCount() > 0) {
            this.glassMesh.createSubChunkVBOVAO();
        }
        if (this.liquidMesh.getVertexCount() > 0) {
            this.liquidMesh.createSubChunkVBOVAO();
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
        this.transparentMesh.cleanup();
        this.glassMesh.cleanup();
        this.liquidMesh.cleanup();

        this.mesh.deleteSubChunkVBOVAO();
        this.transparentMesh.deleteSubChunkVBOVAO();
        this.glassMesh.deleteSubChunkVBOVAO();
        this.liquidMesh.deleteSubChunkVBOVAO();
        this.cleanup();
    }

    /**
     * Ajoute un bloc dans le subChunk <br> 
     * (coordonnées locales du subChunk)
     * @param type BlockType
     * @param x int
     * @param y int
     * @param z int
     * @throws RuntimeException si les coordonnées sont en dehors du subChunk
     */
    @ChunkThread
    public void addBlockToSubChunk(BlockDefinition type, int x, int y, int z){
        if (x < 0 || x >= SubChunk.CHUNKSIZEXZ ||
            y < 0 || y >= SubChunk.CHUNKSIZEXZ ||
            z < 0 || z >= SubChunk.CHUNKSIZEXZ
        ){throw new RuntimeException("Coordonées dans le subChunk invalide");}
        int index = x + y * SubChunk.CHUNKSIZEXZ + z * SubChunk.CHUNKSIZEXZ * SubChunk.CHUNKSIZEXZ;

        // si l'ancien block était de l'eau on supprime cette eau et ça propagation
        if (BlocksManager.get(this.blocks[index]) == Blocks.WATER){
            this.setLiquidLevel(x, y, z,0);
            this.blocksData.getAndUpdate(index, data -> (data & ~(0x1 << 12)) | ((0 & 0x1) << 12));
            LiquidManager.removeLiquid(this.parent, x, this.y + y, z, (byte)type.liquidLevel);
        }   
        this.blocks[index] = (byte) type.id;

        if (type.luminance > 0){
            // set le bloc source à luminance
            this.setBlockLight(x, y, z, type.luminance);
            
            int[][] neighbors = {{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}};
            for (int[] d : neighbors){
                int nx = x + d[0];
                int ny = this.y + y + d[1];
                int nz = z + d[2];
                
                int gnx = this.parent.x + nx; // coordonnée globale X
                int gnz = this.parent.z + nz; // coordonnée globale Z
                
                Chunk targetChunk = ChunksManager.getChunkAt(gnx, gnz);
                if (targetChunk == null) continue;
                SubChunk targetSub = targetChunk.getSubChunkAt(ny);
                
                int lnx = ChunksManager.floorMod(gnx, SubChunk.CHUNKSIZEXZ);
                int lny = ny % SubChunk.CHUNKSIZEXZ;
                int lnz = ChunksManager.floorMod(gnz, SubChunk.CHUNKSIZEXZ);
                
                if (!targetSub.isBlockSolid(lnx, lny, lnz)){
                    targetSub.setBlockLight(lnx, lny, lnz, type.luminance);
                    LightManager.addBlockLight(this.parent, nx, ny, nz, (byte)type.luminance);
                }
            }
        }      
        if (type.liquidLevel > 0){
            this.setLiquidLevel(x, y, z, type.liquidLevel);
            this.blocksData.getAndUpdate(index, data -> (data & ~(0x1 << 12)) | ((1 & 0x1) << 12)); // le bloc est une source donc on l'indique dans la data
            LiquidManager.addLiquid(this.parent, x, this.y + y, z, (byte)type.liquidLevel);
        }   
        this.parent.updateHeightMapAtWhenAdd(x, this.y + y, z);           

        markDirty(this);

        if (x == SubChunk.CHUNKSIZEXZ - 1) markDirty(this.getNeighborWest());
        if (x == 0)                         markDirty(this.getNeighborEast());
        if (z == SubChunk.CHUNKSIZEXZ - 1) markDirty(this.getNeighborNorth());
        if (z == 0)                         markDirty(this.getNeighborSouth());
        if (y == SubChunk.CHUNKSIZEXZ - 1) markDirty(this.getNeighborUp());
        if (y == 0)                         markDirty(this.getNeighborDown());
    }
    
    /**
     * Retire un bloc dans le subChunk <br>
     * (coordonnées locales du subChunk)
     * @param x int
     * @param y int
     * @param z int
     * @throws RuntimeException si les coordonnées sont en dehors du subChunk
     */
    @ChunkThread
    public void removeBlockFromSubChunk(int x, int y, int z){
        if (x < 0 || x >= SubChunk.CHUNKSIZEXZ ||
            y < 0 || y >= SubChunk.CHUNKSIZEXZ ||
            z < 0 || z >= SubChunk.CHUNKSIZEXZ
        ){throw new RuntimeException("Coordonées dans le subChunk invalide");}
        int index = x + y * SubChunk.CHUNKSIZEXZ + z * SubChunk.CHUNKSIZEXZ * SubChunk.CHUNKSIZEXZ;    

        BlockDefinition type = BlocksManager.get(this.blocks[index]);

        if (type.luminance > 0){
            this.setBlockLight(x, y, z, 0);
            
            int[][] neighbors = {{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}};
            for (int[] d : neighbors){
                int nx = x + d[0];
                int ny = this.y + y + d[1];
                int nz = z + d[2];
                
                int gnx = this.parent.x + nx;
                int gnz = this.parent.z + nz;
                
                Chunk targetChunk = ChunksManager.getChunkAt(gnx, gnz);
                if (targetChunk == null) continue;
                SubChunk targetSub = targetChunk.getSubChunkAt(ny);
                
                int lnx = ChunksManager.floorMod(gnx, SubChunk.CHUNKSIZEXZ);
                int lny = ny % SubChunk.CHUNKSIZEXZ;
                int lnz = ChunksManager.floorMod(gnz, SubChunk.CHUNKSIZEXZ);
                
                if (!targetSub.isBlockSolid(lnx, lny, lnz)){
                    targetSub.setBlockLight(lnx, lny, lnz, 0);
                    LightManager.removeBlockLight(this.parent, nx, ny, nz, (byte)type.luminance);
                }
            }
        }    

        // si c'est une source de liquide, nettoyer son état liquide en plus
        if (((this.blocksData.get(index) >>> 12) & 0x1) == 1){
            this.setLiquidLevel(x, y, z, 0);
            this.blocksData.getAndUpdate(index, data -> (data & ~(0x1 << 12)) | ((0 & 0x1) << 12));
            LiquidManager.removeLiquid(this.parent, x, this.y + y, z, (byte)type.liquidLevel);
        }

        this.blocks[index] = (byte) Blocks.AIR.id;  
        // relance la propagation du liquide voisin qui pourrait maintenant couler dans le trou créé
        int[][] liquidTriggerNeighbors = {{0,1,0},{1,0,0},{-1,0,0},{0,0,1},{0,0,-1}};
        for (int[] d : liquidTriggerNeighbors) {
            int nx = x + d[0];
            int ny = this.y + y + d[1];
            int nz = z + d[2];

            int gnx = this.parent.x + nx;
            int gnz = this.parent.z + nz;

            Chunk targetChunk = ChunksManager.getChunkAt(gnx, gnz);
            if (targetChunk == null) continue;
            SubChunk targetSub = targetChunk.getSubChunkAt(ny);
            if (targetSub == null) continue;

            int lnx = ChunksManager.floorMod(gnx, SubChunk.CHUNKSIZEXZ);
            int lny = ChunksManager.floorMod(ny, SubChunk.CHUNKSIZEXZ);
            int lnz = ChunksManager.floorMod(gnz, SubChunk.CHUNKSIZEXZ);

            int neighborLiquidLevel = targetSub.getLiquidLevel(lnx, lny, lnz);
            if (neighborLiquidLevel > 0) {
                LiquidManager.addLiquid(this.parent, nx, ny, nz, (byte) neighborLiquidLevel);
            }
        }          
        this.parent.updateHeightMapAtWhenBreak(x, this.y + y, z);

        markDirty(this);

        if (x == SubChunk.CHUNKSIZEXZ - 1) markDirty(this.getNeighborWest());
        if (x == 0)                         markDirty(this.getNeighborEast());
        if (z == SubChunk.CHUNKSIZEXZ - 1) markDirty(this.getNeighborNorth());
        if (z == 0)                         markDirty(this.getNeighborSouth());
        if (y == SubChunk.CHUNKSIZEXZ - 1) markDirty(this.getNeighborUp());
        if (y == 0)                         markDirty(this.getNeighborDown());
    }

    /**
     * Marque un sous-chunk comme "dirty" (à reconstruire) s'il ne l'est pas déjà,
     * et l'ajoute à la file du LightManager.
     * @param sub sous-chunk à marquer, ignoré si null
     */
    private void markDirty(SubChunk sub){
        if (sub == null) return;
        if (sub.dirty.compareAndSet(false, true)){
            LightManager.addDirtySubChunk(sub);
        }
    }

    /**
     * Reconstruit le subChunk (mesh)
     */
    @ChunkThread
    public void rebuildSubChunk(){

        this.linkNeighbors();        
        // synchronized(this) fait attendre le second thread au lieu de skipper, donc aucun rebuild n'est perdu
        synchronized(this) {
            this.fillSubChunkBitSet();
            this.mesh.setupSubChunkBuffers(this.solidBitSet.cardinality());
            this.transparentMesh.setupSubChunkBuffers(this.transparentBitSet.cardinality());
            this.glassMesh.setupSubChunkBuffers(this.transparentBitSet.cardinality());
            this.liquidMesh.setupSubChunkBuffers(this.transparentBitSet.cardinality());
            this.generateSubChunkMesh();
        }
        
        // met le thread en pause si l'array est plein, le thread se relance automatiquement au moment où OpenGLThread poll un chunk
        if (this.mesh.getVertexCount() > 0 || this.transparentMesh.getVertexCount() > 0 || this.glassMesh.getVertexCount() > 0 || this.liquidMesh.getVertexCount() > 0) {
            try { Render.dirtySubChunks.put(this); } catch(InterruptedException e) { Thread.currentThread().interrupt(); }
        }
    }

    /**
     * Envoie le mesh à dessiné
     * @param quads liste des quads opaques à ajouter au mesh
     */
    @ChunkThread
    private void uploadSubChunkMesh(List<Quad> quads){
        for(Quad quad : quads){            
            this.mesh.addQuadVertexToArray(quad, quad.textureId);
        }        
    }

    /**
     * Envoie les quads transparents (non-verre) au mesh transparent.
     * @param quads liste des quads transparents à ajouter au mesh
     */
    @ChunkThread
    private void uploadSubChunkTransparentMesh(List<Quad> quads){
        for(Quad quad : quads){
            this.transparentMesh.addQuadVertexToArray(quad, quad.textureId);
        }        
    }

    /**
     * Envoie les quads de verre au mesh de verre.
     * @param quads liste des quads de verre à ajouter au mesh
     */
    @ChunkThread
    private void uploadSubChunkGlassMesh(List<Quad> quads){
        for(Quad quad : quads){
            this.glassMesh.addQuadVertexToArray(quad, quad.textureId);
        }        
    }

    /**
     * Envoie les quads de liquide au mesh de liquide.
     * @param quads liste des quads de liquide à ajouter au mesh
     */
    @ChunkThread
    private void uploadSubChunkLiquidMesh(List<Quad> quads){
        for(Quad quad : quads){
            this.liquidMesh.addQuadVertexToArray(quad, quad.textureId);
        }        
    }
    /**
     * Calcule le face culling du subChunk pour un mode donné (opaque, verre ou transparent)
     * et génère les quads correspondants pour les 6 directions. <br>
     * Comme son nom l'indique, calcule aussi avec les faces des chunks voisins.
     * @param faceData tampon thread-local dans lequel écrire les masques de faces visibles
     * @param mode mode de culling (MODE_OPAQUE, MODE_GLASS ou MODE_TRANSPARENT)
     * @param quads liste dans laquelle les quads générés sont ajoutés
     */
    @ChunkThread
    private void calculateSubChunkFaceCulling(FaceData faceData, int mode, List<Quad> quads){        
        quads.clear(); 
        Arrays.fill(faceData.northAxis, 0); Arrays.fill(faceData.southAxis, 0);        
        Arrays.fill(faceData.westAxis,  0); Arrays.fill(faceData.eastAxis,  0);
        Arrays.fill(faceData.upAxis,    0); Arrays.fill(faceData.downAxis,  0);

        // Récup nord sud
        for (int y = 0; y < SubChunk.CHUNKSIZEXZ; y++){
            for (int x = 0; x < SubChunk.CHUNKSIZEXZ; x++){
                int index = y + x * SubChunk.CHUNKSIZEXZ;
                for (int z = 0; z < SubChunk.CHUNKSIZEXZ; z++){
                    int bitSetIndex = x + y * SubChunk.CHUNKSIZEXZ + z * SubChunk.CHUNKSIZEXZ * SubChunk.CHUNKSIZEXZ;
                    boolean value = computeValue(bitSetIndex, mode);
                    faceData.northAxis[index] |= (value ? 1 : 0) << (31 - z);
                }
                int ligne = faceData.northAxis[index];

                int ligneNeighborSouth = 0;
                if (this.getNeighborSouth() != null){
                    int bitSetIndex = x + y * SubChunk.CHUNKSIZEXZ + 15 * SubChunk.CHUNKSIZEXZ * SubChunk.CHUNKSIZEXZ;
                    SubChunk neighbor = this.getNeighborSouth();
                    ligneNeighborSouth = computeNeighborValue(neighbor, bitSetIndex, mode) ? 1 : 0;
                }
                ligneNeighborSouth = ligneNeighborSouth << 31;

                int ligneNeighborNorth = 0;
                if (this.getNeighborNorth() != null){
                    int bitSetIndex = x + y * SubChunk.CHUNKSIZEXZ + 0 * SubChunk.CHUNKSIZEXZ * SubChunk.CHUNKSIZEXZ;
                    SubChunk neighbor = this.getNeighborNorth();
                    ligneNeighborNorth = computeNeighborValue(neighbor, bitSetIndex, mode) ? 1 : 0;
                }
                ligneNeighborNorth = ligneNeighborNorth << 16;

                if (mode == MODE_TRANSPARENT) {
                    faceData.southAxis[index] = ligne & ~ligneNeighborSouth;
                    faceData.northAxis[index] = ligne & ~ligneNeighborNorth;
                } else {
                    faceData.southAxis[index] = ligne & ~((ligne >>> 1) | ligneNeighborSouth);
                    faceData.northAxis[index] = ligne & ~((ligne << 1)  | ligneNeighborNorth);
                }
            }
        }

        // Récup ouest est
        for (int y = 0; y < SubChunk.CHUNKSIZEXZ; y++){
            for (int z = 0; z < SubChunk.CHUNKSIZEXZ; z++){
                int index = y + z * SubChunk.CHUNKSIZEXZ;
                for (int x = 0; x < SubChunk.CHUNKSIZEXZ; x++){
                    int bitSetIndex = x + y * SubChunk.CHUNKSIZEXZ + z * SubChunk.CHUNKSIZEXZ * SubChunk.CHUNKSIZEXZ;
                    boolean value = computeValue(bitSetIndex, mode);
                    faceData.eastAxis[index] |= (value ? 1 : 0) << (31 - x);
                }
                int ligne = faceData.eastAxis[index];

                int ligneNeighborEast = 0;
                if (this.getNeighborEast() != null){
                    int bitSetIndex = 15 + y * SubChunk.CHUNKSIZEXZ + z * SubChunk.CHUNKSIZEXZ * SubChunk.CHUNKSIZEXZ;
                    SubChunk neighbor = this.getNeighborEast();
                    ligneNeighborEast = computeNeighborValue(neighbor, bitSetIndex, mode) ? 1 : 0;
                }
                ligneNeighborEast = ligneNeighborEast << 31;

                int ligneNeighborWest = 0;
                if (this.getNeighborWest() != null){
                    int bitSetIndex = 0 + y * SubChunk.CHUNKSIZEXZ + z * SubChunk.CHUNKSIZEXZ * SubChunk.CHUNKSIZEXZ;
                    SubChunk neighbor = this.getNeighborWest();
                    ligneNeighborWest = computeNeighborValue(neighbor, bitSetIndex, mode) ? 1 : 0;
                }
                ligneNeighborWest = ligneNeighborWest << 16;

                if (mode == MODE_TRANSPARENT) {
                    faceData.eastAxis[index] = ligne & ~ligneNeighborEast;
                    faceData.westAxis[index] = ligne & ~ligneNeighborWest;
                } else {
                    faceData.eastAxis[index] = ligne & ~((ligne >>> 1) | ligneNeighborEast);
                    faceData.westAxis[index] = ligne & ~((ligne << 1)  | ligneNeighborWest);
                }
            }
        }

        // Récup haut bas
        for (int z = 0; z < SubChunk.CHUNKSIZEXZ; z++){
            for (int x = 0; x < SubChunk.CHUNKSIZEXZ; x++){
                int index = z + x * SubChunk.CHUNKSIZEXZ;
                for (int y = 0; y < SubChunk.CHUNKSIZEXZ; y++){
                    int bitSetIndex = x + y * SubChunk.CHUNKSIZEXZ + z * SubChunk.CHUNKSIZEXZ * SubChunk.CHUNKSIZEXZ;
                    boolean value = computeValue(bitSetIndex, mode);
                    faceData.upAxis[index] |= (value ? 1 : 0) << (31 - y);
                }
                int ligne = faceData.upAxis[index];

                int ligneNeighborUp = 0;
                if (this.getNeighborUp() != null){
                    int bitSetIndex = x + 0 * SubChunk.CHUNKSIZEXZ + z * SubChunk.CHUNKSIZEXZ * SubChunk.CHUNKSIZEXZ;
                    SubChunk neighbor = this.getNeighborUp();
                    ligneNeighborUp = computeNeighborValue(neighbor, bitSetIndex, mode) ? 1 : 0;
                }
                ligneNeighborUp = ligneNeighborUp << 16;

                int ligneNeighborDown = 0;
                if (this.getNeighborDown() != null){
                    int bitSetIndex = x + 15 * SubChunk.CHUNKSIZEXZ + z * SubChunk.CHUNKSIZEXZ * SubChunk.CHUNKSIZEXZ;
                    SubChunk neighbor = this.getNeighborDown();
                    ligneNeighborDown = computeNeighborValue(neighbor, bitSetIndex, mode) ? 1 : 0;
                }
                ligneNeighborDown = ligneNeighborDown << 31;

                if (mode == MODE_TRANSPARENT) {
                    faceData.upAxis[index]   = ligne & ~ligneNeighborUp;
                    faceData.downAxis[index] = ligne & ~ligneNeighborDown;
                } else {
                    faceData.upAxis[index]   = ligne & ~((ligne << 1)  | ligneNeighborUp);
                    faceData.downAxis[index] = ligne & ~((ligne >>> 1) | ligneNeighborDown);
                }
            }
        }
        
        this.createQuad(faceData.northAxis, 0, quads);
        this.createQuad(faceData.southAxis, 1, quads);
        this.createQuad(faceData.westAxis,  2, quads);
        this.createQuad(faceData.eastAxis,  3, quads);
        this.createQuad(faceData.upAxis,    4, quads);
        this.createQuad(faceData.downAxis,  5, quads);
    }

    /**
     * Calcule si le bloc courant doit être inclus dans la ligne selon le mode.
     * @param bitSetIndex index du bloc dans solidBitSet / transparentBitSet
     * @param mode mode de culling (MODE_OPAQUE, MODE_GLASS ou MODE_TRANSPARENT)
     * @return true si le bloc doit être compté comme "plein" pour ce mode
     */
    // "->" nouvelle syntaxe qui permet d'évité le break ici il est implicite
    private boolean computeValue(int bitSetIndex, int mode) {
        boolean solid  = solidBitSet.get(bitSetIndex);
        boolean transp = transparentBitSet.get(bitSetIndex);
        return switch (mode) {
            case MODE_OPAQUE -> solid && !transp;
            case MODE_GLASS  -> solid && transp && (this.blocks[bitSetIndex] == Blocks.GLASS.id);
            default          -> solid && transp && (this.blocks[bitSetIndex] != Blocks.GLASS.id);
        };
    }

    /**
     * Calcule si le bloc voisin cache la face selon le mode.
     * @param neighbor sous-chunk voisin à interroger
     * @param bitSetIndex index du bloc voisin dans solidBitSet / transparentBitSet
     * @param mode mode de culling (MODE_OPAQUE, MODE_GLASS ou MODE_TRANSPARENT)
     * @return true si le bloc voisin cache la face du bloc courant
     */
    private boolean computeNeighborValue(SubChunk neighbor, int bitSetIndex, int mode) {
        boolean nSolid  = neighbor.solidBitSet.get(bitSetIndex);
        boolean nTransp = neighbor.transparentBitSet.get(bitSetIndex);
        return switch (mode) {
            // opaque : caché par un voisin opaque seulement
            case MODE_OPAQUE -> nSolid && !nTransp;
            // verre : caché par n'importe quel voisin solide (verre/verre ou opaque/verre)
            case MODE_GLASS  -> nSolid;
            // feuilles etc. : caché uniquement par un voisin opaque
            default          -> nSolid && !nTransp;
        };
    }

    /**
     * Crée les quads de chaque face pour une direction donnée à partir du masque de faces visibles.
     * @param faceMask masque de bits des faces visibles pour cette direction
     * @param dir direction de la face (0=nord, 1=sud, 2=ouest, 3=est, 4=haut, 5=bas)
     * @param quads liste dans laquelle les quads générés sont ajoutés
     */
    // créer les quads de chaque face
    private void createQuad(int[] faceMask, int dir, List<Quad> quads){
        switch (dir) {
            case 0: case 1:
                for (int y = 0; y < SubChunk.CHUNKSIZEXZ; y++){
                    for (int x = 0; x < SubChunk.CHUNKSIZEXZ; x++){
                        int index = y + x * SubChunk.CHUNKSIZEXZ;
                        for (int z = 0; z < SubChunk.CHUNKSIZEXZ; z++){
                            int bit;
                            if (dir == 0) bit = faceMask[index] >>> (31 - z) & 1;
                            else          bit = faceMask[index] >>  (31 - z) & 1;                          
                            
                            if (bit == 1){
                                BlockDefinition block = this.getBlock(x, y, z);
                                int liquidLevel = this.getLiquidLevel(x, y, z); 
                                if (liquidLevel == 15) liquidLevel = 16; // triche si c'est une colonne d'eau alors le bloc est plein car sinon avec 15 il manque 1 tranche d'eau 
                                int textureId = BlocksManager.get(block.id).getTextureLayer(dir);
                                byte ao0, ao1, ao2, ao3;
                                int skyLight, blockLight;
                                if (dir == 0){
                                    ao0 = computeAO(x-1, y, z+1,   x, y-1, z+1,   x-1, y-1, z+1);
                                    ao1 = computeAO(x+1, y, z+1,   x, y-1, z+1,   x+1, y-1, z+1);
                                    ao2 = computeAO(x-1, y, z+1,   x, y+1, z+1,   x-1, y+1, z+1);
                                    ao3 = computeAO(x+1, y, z+1,   x, y+1, z+1,   x+1, y+1, z+1);
                                    skyLight = (byte)this.getBlockSkyLight(x, y, z+1);
                                    blockLight = (byte)this.getBlockLight(x, y, z+1);     
                                }else{
                                    ao0 = computeAO(x-1, y, z-1,   x, y-1, z-1,   x-1, y-1, z-1);
                                    ao1 = computeAO(x+1, y, z-1,   x, y-1, z-1,   x+1, y-1, z-1);
                                    ao2 = computeAO(x-1, y, z-1,   x, y+1, z-1,   x-1, y+1, z-1);
                                    ao3 = computeAO(x+1, y, z-1,   x, y+1, z-1,   x+1, y+1, z-1);                                    
                                    skyLight = (byte)this.getBlockSkyLight(x, y, z-1);
                                    blockLight = (byte)this.getBlockLight(x, y, z-1);
                                }   
                                // si il y a du liquide alors on prend en priorité la hauteur du liquide
                                quads.add(new Quad((x * 16) + block.x, (y * 16) + block.y, (z * 16) + block.z,
                                                   block.width, (liquidLevel > 0 ? liquidLevel : block.height), block.depth,
                                                   dir, ao0, ao1, ao2, ao3, skyLight, blockLight, textureId));
                            }
                        }
                    }
                }
                break;
            case 2: case 3:
                for (int y = 0; y < SubChunk.CHUNKSIZEXZ; y++){
                    for (int z = 0; z < SubChunk.CHUNKSIZEXZ; z++){
                        int index = y + z * SubChunk.CHUNKSIZEXZ;
                        for (int x = 0; x < SubChunk.CHUNKSIZEXZ; x++){
                            int bit;
                            if (dir == 2) bit = faceMask[index] >>> (31 - x) & 1;
                            else          bit = faceMask[index] >>  (31 - x) & 1;     
                            if (bit == 1){
                                BlockDefinition block = this.getBlock(x, y, z);
                                int liquidLevel = this.getLiquidLevel(x, y, z); 
                                int textureId = BlocksManager.get(block.id).getTextureLayer(dir);
                                byte ao0, ao1, ao2, ao3;
                                int skyLight, blockLight;
                                if (dir == 2){
                                    ao0 = computeAO(x+1, y, z-1,   x+1, y-1, z,   x+1, y-1, z-1);
                                    ao1 = computeAO(x+1, y, z+1,   x+1, y-1, z,   x+1, y-1, z+1);
                                    ao2 = computeAO(x+1, y, z-1,   x+1, y+1, z,   x+1, y+1, z-1);
                                    ao3 = computeAO(x+1, y, z+1,   x+1, y+1, z,   x+1, y+1, z+1);                       
                                    skyLight = (byte)this.getBlockSkyLight(x+1, y, z);
                                    blockLight = (byte)this.getBlockLight(x+1, y, z);
                                }else{
                                    ao0 = computeAO(x-1, y, z-1,   x-1, y-1, z,   x-1, y-1, z-1);
                                    ao1 = computeAO(x-1, y, z+1,   x-1, y-1, z,   x-1, y-1, z+1);
                                    ao2 = computeAO(x-1, y, z-1,   x-1, y+1, z,   x-1, y+1, z-1);
                                    ao3 = computeAO(x-1, y, z+1,   x-1, y+1, z,   x-1, y+1, z+1);
                                    skyLight = (byte)this.getBlockSkyLight(x-1, y, z);
                                    blockLight = (byte)this.getBlockLight(x-1, y, z);
                                }
                                quads.add(new Quad((x * 16) + block.x, (y * 16) + block.y, (z * 16) + block.z,
                                                   block.width, (liquidLevel > 0 ? liquidLevel : block.height), block.depth,
                                                   dir, ao0, ao1, ao2, ao3, skyLight, blockLight, textureId));
                            }
                        }
                    }
                }              
                break;
            case 4: case 5:
                for (int z = 0; z < SubChunk.CHUNKSIZEXZ; z++){
                    for (int x = 0; x < SubChunk.CHUNKSIZEXZ; x++){
                        int index = z + x * SubChunk.CHUNKSIZEXZ;
                        for (int y = 0; y < SubChunk.CHUNKSIZEXZ; y++){
                            // récupère le premier bit de l'int en avançant à chaque boucle
                            int bit = faceMask[index] >>> (31 - y) & 1;
                            // si bloc solide
                            if (bit == 1){
                                BlockDefinition block = this.getBlock(x, y, z);
                                int liquidLevel = this.getLiquidLevel(x, y, z); 
                                int textureId = BlocksManager.get(block.id).getTextureLayer(dir);
                                byte ao0, ao1, ao2, ao3;
                                int skyLight, blockLight;
                                if (dir == 4){
                                    ao0 = computeAO(x-1, y+1, z,   x, y+1, z-1,   x-1, y+1, z-1);
                                    ao1 = computeAO(x+1, y+1, z,   x, y+1, z-1,   x+1, y+1, z-1);
                                    ao2 = computeAO(x-1, y+1, z,   x, y+1, z+1,   x-1, y+1, z+1);
                                    ao3 = computeAO(x+1, y+1, z,   x, y+1, z+1,   x+1, y+1, z+1);
                                    skyLight = this.getBlockSkyLight(x, y+1, z);
                                    blockLight = this.getBlockLight(x, y+1, z);
                                }else{
                                    ao0 = computeAO(x-1, y-1, z,   x, y-1, z-1,   x-1, y-1, z-1);
                                    ao1 = computeAO(x+1, y-1, z,   x, y-1, z-1,   x+1, y-1, z-1);
                                    ao2 = computeAO(x-1, y-1, z,   x, y-1, z+1,   x-1, y-1, z+1);
                                    ao3 = computeAO(x+1, y-1, z,   x, y-1, z+1,   x+1, y-1, z+1);
                                    skyLight = this.getBlockSkyLight(x, y-1, z);
                                    blockLight = this.getBlockLight(x, y-1, z);
                                }                               
                                quads.add(new Quad((x * 16) + block.x, (y * 16) + block.y, (z * 16) + block.z,
                                                   block.width, (liquidLevel > 0 ? liquidLevel : block.height), block.depth,
                                                   dir, ao0, ao1, ao2, ao3, skyLight, blockLight, textureId));
                            }
                        }
                    }
                }
                break;
        }
    }

    /**
     * Détermine si un bloc est solide <br>
     * Si le bloc n'est pas dans le chunk (voisin d'un chunk) <br>
     * On demande au voisin
     * @param x Coordonnées en x (locale), peut être hors des bornes du subChunk (délégué au voisin)
     * @param y Coordonnées en y (locale), peut être hors des bornes du subChunk (délégué au voisin)
     * @param z Coordonnées en z (locale), peut être hors des bornes du subChunk (délégué au voisin)
     * @return true si le bloc est solide, false s'il est vide ou si aucun voisin n'est chargé
     */
    public boolean isBlockSolid(int x, int y, int z){
        // dans le subchunk courant
        if (x >= 0 && x < CHUNKSIZEXZ && y >= 0 && y < CHUNKSIZEXZ && z >= 0 && z < CHUNKSIZEXZ){
            int index = x + y * SubChunk.CHUNKSIZEXZ + z * SubChunk.CHUNKSIZEXZ * SubChunk.CHUNKSIZEXZ;
            return !BlocksManager.get(this.blocks[index]).transparent;
        }
        // en dehors délègue aux voisins, blocs en bordure
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
     * Calcule l'ambient occlusion à appliquer sur le bloc
     * @param side1X Coordonnée x du premier bloc adjacent au coin
     * @param side1Y Coordonnée y du premier bloc adjacent au coin
     * @param side1Z Coordonnée z du premier bloc adjacent au coin
     * @param side2X Coordonnée x du second bloc adjacent au coin
     * @param side2Y Coordonnée y du second bloc adjacent au coin
     * @param side2Z Coordonnée z du second bloc adjacent au coin
     * @param cornerX Coordonnée x du bloc en diagonale du coin
     * @param cornerY Coordonnée y du bloc en diagonale du coin
     * @param cornerZ Coordonnée z du bloc en diagonale du coin
     * @return le niveau d'occlusion ambiante du coin (0 = aucune occlusion, 3 = occlusion maximale)
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
        this.solidBitSet.clear();
        this.transparentBitSet.clear();
        for(int i = 0; i < SubChunk.MAXBLOCK; i++){      
            // les blocs solide ne doivent pas être face cull, logique   
            // blocsManager.get(this.blocs[i]).transparent == false
            if (this.blocks[i] != Blocks.AIR.id){
                this.solidBitSet.set(i); // met a 1 l'indice i
            }
            // les blocs transparent ne doivent pas être face cull, mais rendu
            if (BlocksManager.get(this.blocks[i]).transparent == true){
                this.transparentBitSet.set(i);
            }
        }
    }
    
    /**
     * Récupère tout les voisins du subChunk
     */
    @ChunkThread
    public void linkNeighbors(){
        // évite les try catch qui sont couteux en java
        // ouest          
        Chunk chunk = ChunksManager.getChunkAt(
            this.x + SubChunk.CHUNKSIZEXZ,
            this.z);
        this.westNeighbor = (chunk != null) ? chunk.getSubChunkAt(this.y) : null;

        // est
        chunk = ChunksManager.getChunkAt(
            this.x - SubChunk.CHUNKSIZEXZ,
            this.z);
        this.eastNeighbor = (chunk != null) ? chunk.getSubChunkAt(this.y) : null;

        // nord
        chunk = ChunksManager.getChunkAt(
            this.x,
            this.z + SubChunk.CHUNKSIZEXZ);
        this.northNeighbor = (chunk != null) ? chunk.getSubChunkAt(this.y) : null;

        // sud
        chunk = ChunksManager.getChunkAt(
            this.x,
            this.z - SubChunk.CHUNKSIZEXZ);
        this.southNeighbor = (chunk != null) ? chunk.getSubChunkAt(this.y) : null;

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

    /**
     * Dessine le mesh opaque et transparent du subChunk (même passe, sans blend).
     * @param parentModel matrice modèle du nœud parent (chunk)
     */
    @OpenGLThread
    @Override
    public void drawNode(Matrix4f parentModel){
        if (this.buffer == null) return;
        parentModel.translate(this.x, this.y, this.z, this.model);
        this.model.get(this.buffer);
        ShadersManager.runModel(this.buffer);
        this.mesh.drawSubChunkMesh();
        this.transparentMesh.drawSubChunkMesh(); // même passe, pas de blend              
    }

    /**
     * Dessine le mesh de verre du subChunk.
     * @param parentModel matrice modèle du nœud parent (chunk)
     */
    @OpenGLThread
    public void drawNodeGlass(Matrix4f parentModel){
        if (this.buffer == null) return;
        parentModel.translate(this.x, this.y, this.z, this.model);
        this.model.get(this.buffer);
        ShadersManager.runModel(this.buffer);
        this.glassMesh.drawSubChunkMesh();       
    }

    /**
     * Dessine le mesh des liquides du subChunk.
     * @param parentModel matrice modèle du nœud parent (chunk)
     */
    @OpenGLThread
    public void drawNodeLiquid(Matrix4f parentModel){
        if (this.buffer == null) return;
        parentModel.translate(this.x, this.y, this.z, this.model);
        this.model.get(this.buffer);
        ShadersManager.runModelLiquid(this.buffer);   
        this.liquidMesh.drawSubChunkMeshLiquid();  
    }
}