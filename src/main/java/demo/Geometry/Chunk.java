package demo.Geometry;

import org.joml.Matrix4f;

import demo.Annotations.ChunkThread;
import demo.Annotations.OpenGLThread;
import demo.Rendering.FrustumCulling;
import demo.Rendering.Render;
import demo.Managers.LightManager;
import demo.Others.BlockDefinition;

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

    private int[] colY      = new int[256];
    private byte[] colLight = new byte[256];

    /**
     * Height map qui contient la hauteur du bloc (x, z) le plus proche du ciel
     */
    public int[] heightMap;
    
    /**
     * Initialise les subChunks du chunks.
     * @param chunkX Coordonnées en x (locale) 0-15
     * @param chunkZ Coordonnées en Z  (locale)
     */
    @ChunkThread
    public Chunk(int chunkX, int chunkZ){
        this.subChunks = new SubChunk[Chunk.SUBCHUNKCOUNT];
        for (int i = 0; i < this.subChunks.length; i++){
            this.subChunks[i] = new SubChunk(this);
            this.subChunks[i].setSubChunkPosition(0, i * SubChunk.CHUNKSIZEXZ, 0);
        }
        this.setChunkPosition(chunkX, chunkZ);
        
        this.minX = this.x;
        this.minY = this.y;
        this.minZ = this.z;

        this.maxX = this.minX + SubChunk.CHUNKSIZEXZ;
        this.maxY = this.minY + SubChunk.CHUNKSIZEHEIGHT;
        this.maxZ = this.minZ + SubChunk.CHUNKSIZEXZ;

        this.heightMap = new int[SubChunk.CHUNKSIZEXZ * SubChunk.CHUNKSIZEXZ];
        
        Render.monitor.addChunkCount();
    }

    /**
     * Initialise la Height map du chunk.
     */
    @ChunkThread
    public void initHeightMap(){        
        for(int x = 0; x < SubChunk.CHUNKSIZEXZ; x++){
            for(int z = 0; z < SubChunk.CHUNKSIZEXZ; z++){
                int y = SubChunk.CHUNKSIZEHEIGHT - 1;
                int lowestSkyY = -1; // le bloc d'air le plus bas de la colonne ciel

                while (y >= 0) {           
                    if (this.getBlock(x, y, z).transparent == false) {
                        // On a touché le sol, on enregistre la hauteur
                        this.heightMap[z + x * SubChunk.CHUNKSIZEXZ] = y;
                        this.getSubChunkAt(y).setBlockSkyLight(x, y % SubChunk.CHUNKSIZEXZ, z, (byte)0);
                        break; 
                    }
                    
                    // On donne 15 à l'air directement, SANS ajouter à la queue BFS
                    this.getSubChunkAt(y).setBlockSkyLight(x, y % SubChunk.CHUNKSIZEXZ, z, (byte)15);
                    lowestSkyY = y; // on descend donc c'est toujours le plus bas trouvé jusqu'ici
                    
                    y--;
                }

                if (y == 0 && this.getBlock(x, y, z).transparent == true){
                    this.heightMap[z + x * SubChunk.CHUNKSIZEXZ] = 0;
                }

                // Une seule entrée BFS par colonne : le bloc du bas de la colonne ciel.
                // C'est lui qui est adjacent à la surface -> c'est le seul seed utile
                // pour la propagation latérale vers les zones d'ombre.
                if (lowestSkyY >= 0) {
                    LightManager.addSkyLight(this, x, lowestSkyY, z, (byte)15);
                }
            }
        }        
    }

    /**
     * Met à jour la height map quand un bloc est poser si besoin, et propage de l'obscurité
     * @param x Coordonnées en x (locale) 0-15
     * @param x Coordonnées en y (locale) 0-255
     * @param z Coordonnées en z (locale) 0-15
     */
    @ChunkThread
    public void updateHeightMapAtWhenAdd(int x, int y, int z){
        SubChunk sub = this.getSubChunkAt(y);
        int localY = y % SubChunk.CHUNKSIZEXZ;       

        // bloc posé est la nouvelle heightmap
        // les blocs en dessous sont placé dans la queue d'obscurité 
        if (this.heightMap[z + x * SubChunk.CHUNKSIZEXZ] < y){
            this.heightMap[z + x * SubChunk.CHUNKSIZEXZ] = y;

            // Phase 1 : mettre toute la colonne à 0 avant tout BFS
            // (évite que le BFS d'un bloc voie les blocs inférieurs encore éclairés
            //  et les traite comme sources externes -> re-propagation incorrecte)
            byte oldLight = (byte) sub.getBlockSkyLight(x, y % SubChunk.CHUNKSIZEXZ, z);
            sub.setBlockSkyLight(x, localY, z, 0);
            LightManager.removeSkyLight(this, x, y, z, oldLight);

            int cy = y - 1;
            int count = 0;
            SubChunk bottomSub = null;

            while (cy >= 0) {
                sub = this.getSubChunkAt(cy);
                oldLight = (byte) sub.getBlockSkyLight(x, cy % SubChunk.CHUNKSIZEXZ, z);
                sub.setBlockSkyLight(x, cy % SubChunk.CHUNKSIZEXZ, z, 0);
                if (sub.getBlock(x, cy % SubChunk.CHUNKSIZEXZ, z).transparent == false) {
                    bottomSub = sub;
                    break;
                }
                colY[count]   = cy;
                colLight[count] = oldLight;
                count++;
                cy--;
            }

            if (bottomSub != null && this.getSubChunkAt(y) != bottomSub) {
                bottomSub.rebuildSubChunk();
            }

            // Phase 2 : propager l'obscurité maintenant que toute la colonne est à 0
            // Le BFS ne verra plus les blocs inférieurs comme sources lumineuses
            for (int i = 0; i < count; i++) {
                LightManager.removeSkyLight(this, x, colY[i], z, colLight[i]);
            }
        }else{
            // bloc posé sous la heightmap, pas de changement de heightmap mais il faut quand même éteindre sa lumière
            byte oldLight = (byte) sub.getBlockSkyLight(x, localY, z);
            sub.setBlockSkyLight(x, localY, z, 0);
            LightManager.removeSkyLight(this, x, y, z, oldLight);
        }
    }

    /**
     * Met à jour la height map quand un bloc est casser si besoin, et ajoute les 6 voisins à la propagation de lumière
     * @param x Coordonnées en x (locale) 0-15
     * @param x Coordonnées en y (locale) 0-255
     * @param z Coordonnées en z (locale) 0-15
     */
    public void updateHeightMapAtWhenBreak(int x, int y, int z){
        // si le bloc cassé est la height map
        if (this.heightMap[z + x * SubChunk.CHUNKSIZEXZ] == y){
            int cy = y;
            while (cy >= 0) {
                if (this.getBlock(x, cy, z).transparent == false) {
                    // On a touché le sol, on enregistre la hauteur
                    this.heightMap[z + x * SubChunk.CHUNKSIZEXZ] = cy;
                    this.getSubChunkAt(cy).setBlockSkyLight(x, cy % SubChunk.CHUNKSIZEXZ, z, (byte)0);
                    SubChunk sub = this.getSubChunkAt(cy);
                    if (this.getSubChunkAt(y) != sub){
                        sub.rebuildSubChunk();
                    }
                    break; 
                }
                // On donne 15 à l'air
                this.getSubChunkAt(cy).setBlockSkyLight(x, cy % SubChunk.CHUNKSIZEXZ, z, (byte)15);
                LightManager.addSkyLight(this, x, cy, z, (byte)15);
                cy--;
            }
            if (cy == 0 && this.getBlock(x, cy, z).transparent == true){
                this.heightMap[z + x * SubChunk.CHUNKSIZEXZ] = 0;
            }
        }else{
            final int[][] LIGHT_NEIGHBORS = {{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}};
            for (int[] delta : LIGHT_NEIGHBORS) {                
                int nx = x + delta[0];
                int ny = y + delta[1];
                int nz = z + delta[2];

                Chunk targetChunk = this;
                int localNx = nx;
                int localNz = nz;

                if (nx >= SubChunk.CHUNKSIZEXZ) {
                    SubChunk neighbor = this.getSubChunkAt(ny).getNeighborWest();
                    if (neighbor == null) continue;
                    targetChunk = neighbor.parent;
                    localNx = 0;
                } else if (nx < 0) {
                    SubChunk neighbor = this.getSubChunkAt(ny).getNeighborEast();
                    if (neighbor == null) continue;
                    targetChunk = neighbor.parent;
                    localNx = 15;
                } else if (nz >= SubChunk.CHUNKSIZEXZ) {
                    SubChunk neighbor = this.getSubChunkAt(ny).getNeighborNorth();
                    if (neighbor == null) continue;
                    targetChunk = neighbor.parent;
                    localNz = 0;
                } else if (nz < 0) {
                    SubChunk neighbor = this.getSubChunkAt(ny).getNeighborSouth();
                    if (neighbor == null) continue;
                    targetChunk = neighbor.parent;
                    localNz = 15;
                }

                SubChunk targetSub = targetChunk.getSubChunkAt(ny);
                int light = targetSub.getBlockSkyLight(localNx, ny % SubChunk.CHUNKSIZEXZ, localNz);
                LightManager.addSkyLight(targetChunk, localNx, ny, localNz, (byte) light);
            }
        }          
    }

    /**
     * Retourne un bloc dans le chunk.
     * @param x Coordonnées en x (locale) 0-15
     * @param y Coordonnées en y (global)
     * @param z Coordonnées en z (locale) 0-15
     * @return Retourne le bloc trouvé
     */
    public BlockDefinition getBlock(int x, int y, int z){
        if (y < 0 || y >= SubChunk.CHUNKSIZEHEIGHT){throw new RuntimeException("Valeur en Y incorrect");}
        SubChunk sub = this.getSubChunkAt(y);

        return sub.getBlock(x, y % SubChunk.CHUNKSIZEXZ, z);
    }

    /**
     * Définis la position du chunk
     * @param chunkX position du chunk en X coordonnées locales
     * @param chunkZ position du chunk en Z coordonnées locales
     */
    @ChunkThread
    public void setChunkPosition(int chunkX, int chunkZ){
        this.x = chunkX * SubChunk.CHUNKSIZEXZ;
        this.z = chunkZ * SubChunk.CHUNKSIZEXZ;
        for (int i = 0; i < this.subChunks.length; i++){
            this.subChunks[i].setSubChunkPosition(this.x, i * SubChunk.CHUNKSIZEXZ, this.z);
        }
    }
    
    /**
     * Ajoute un bloc dans le chunk <br> 
     * (coordonnées locales du chunk)
     * @param type BlockType
     * @param x int
     * @param y int
     * @param z int
     * @throws IncorrectCoordinates
     */
    @ChunkThread
    public void addBlockToChunk(BlockDefinition type, int x, int y, int z){
        if (x < 0 || x >= SubChunk.CHUNKSIZEXZ ||
            y < 0 || y >= SubChunk.CHUNKSIZEXZ * Chunk.SUBCHUNKCOUNT ||
            z < 0 || z >= SubChunk.CHUNKSIZEXZ
        ){throw new RuntimeException("Coordonées dans le chunk invalide");}
        this.subChunks[y / SubChunk.CHUNKSIZEXZ].addBlockToSubChunk(type, x, y % SubChunk.CHUNKSIZEXZ, z);
    }

    /**
     * Retire un bloc dans le chunk <br>
     * (coordonnées locales du subChunk)
     * @param x int
     * @param y int
     * @param z int
     * @throws IncorrectCoordinates
     */
    @ChunkThread
    public void removeBlockFromChunk(int x, int y, int z){
        if (x < 0 || x >= SubChunk.CHUNKSIZEXZ ||
            y < 0 || y >= SubChunk.CHUNKSIZEXZ * Chunk.SUBCHUNKCOUNT ||
            z < 0 || z >= SubChunk.CHUNKSIZEXZ
        ){throw new RuntimeException("Coordonées dans le chunk invalide");}
        this.subChunks[y / SubChunk.CHUNKSIZEXZ].removeBlockFromSubChunk(x, y % SubChunk.CHUNKSIZEXZ, z);
    }

    /**
     * Retourne le subChunk à la position donné (y).
     * @param y Coordonnée en y (global)
     * @return Le subChunk retourné
     */
    public SubChunk getSubChunkAt(int y){ 
        if (y < 0 || y >= SubChunk.CHUNKSIZEHEIGHT) {
            throw new RuntimeException("Les coordonées sont incorrects (0-15 pour le subChunk n°0, 16-31 ... n°1, etc...");
        }
        return this.subChunks[y / SubChunk.CHUNKSIZEXZ];
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
        for (SubChunk sub : subChunks){
            if (sub.mesh.getVAO() == 0 && sub.transparentMesh.getVAO() == 0) continue;
            if (!FrustumCulling.testAABB(this.minX, sub.y, this.minZ, this.maxX, sub.y + 16, this.maxZ)) continue;
            sub.drawNode(parentModel);
        }
    }

    public void drawNodeTransparent(Matrix4f parentModel){
        if (FrustumCulling.testAABB(this.minX, this.minY, this.minZ, this.maxX, this.maxY, this.maxZ)){
            for (SubChunk sub : subChunks){
                if (sub.transparentMesh.getVAO() == 0) continue;
                sub.drawNodeTransparent(parentModel);
            }
        }
    }
}
