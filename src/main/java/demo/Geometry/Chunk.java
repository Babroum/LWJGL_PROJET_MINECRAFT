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

    /**
     * Height map qui contient la hauteur du block (x, z) le plus proche du ciel
     */
    public int[] heightMap;
    
    /**
     * Initialise les subChunks du chunks.
     * @param chunkX Coordonnées en X (locale)
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
                while (y >= 0) {           
                    if (this.getBlock(x, y, z).transparent == false) {
                        // On a touché le sol, on enregistre la hauteur
                        this.heightMap[z + x * SubChunk.CHUNKSIZEXZ] = y + 1;
                        break; 
                    }
                    
                    // On donne 15 à l'air ET au premier bloc solide
                    this.getSubChunkAt(y).setBlockSkyLight(x, y % SubChunk.CHUNKSIZEXZ, z, (byte)15);
                    LightManager.addSkyLight(this, x, y, z, (byte)15);
                    
                    y--;
                }
            }
        }
    }

    /**
     * Met à jour une colonne de la height map
     * @param x Coordonnées en x (local)
     * @param z Coordonnées en z (local)
     */
    @ChunkThread
    private void updateHeightMapAt(int x, int z){
        // Recalcule la hauteur de cette colonne
        int y = SubChunk.CHUNKSIZEHEIGHT - 1;
        while (y >= 0) {           
            if (this.getBlock(x, y, z).transparent == false) {
                this.heightMap[z + x * SubChunk.CHUNKSIZEXZ] = y + 1;
                break; 
            }                                   
            y--;
        }
        
        // Remet à 0 seulement cette colonne
        for(int cy = 0; cy < SubChunk.CHUNKSIZEHEIGHT; cy++){
            SubChunk sub = this.getSubChunkAt(cy);
            sub.setBlockSkyLight(x, cy % SubChunk.CHUNKSIZEXZ, z, (byte)0);
        }
        
        // Ajoute toute la colonne au-dessus de la surface à la queue
        int surfaceY = this.heightMap[z + x * SubChunk.CHUNKSIZEXZ];
        for(int cy = surfaceY; cy < SubChunk.CHUNKSIZEHEIGHT; cy++){
            if (this.getBlock(x, cy, z).transparent == true){
                SubChunk sub = this.getSubChunkAt(cy);
                int yInSub = cy % SubChunk.CHUNKSIZEXZ;                  
                sub.setBlockSkyLight(x, yInSub, z, (byte)15);
                LightManager.addSkyLight(this, x, cy, z, (byte)15);                
            }
        }
    }

    /**
     * Met à jour la colonne x, z et ajoute ses voisins dans la queue du bfs
     * @param x Coordonnées en x (local)
     * @param z Coordonnées en z (local)
     */
    @ChunkThread
    public void updateHeightMapZoneAt(int x, int z){
        this.updateHeightMapAt(x, z);        
        
        // update les 4 voisins en forme de spirale montante pour appliquer les niveaux de light les plus élevés en dernier

        // peut ne pas fonctionner dans certains cas mais jsp
        // faire un plafond de 3x3, le centre ne passe pas à 13 si les voisins sont à 14 car les voisins ne sont pas reset donc il voit donc 14 > 14 donc faux et skip 
        // et le fait de reset les voisins va régler le problème mais en créer un autre, où, avec un plafond de 3x3, le centre du plafond est bouché en dernier
        // les 4 voisins adjacents passent à 0 et personne n'est là pour les mettre à 14, car on limite aux voisins adjacents.
        int[][] neighbors = {{-1,0}, {1,0}, {0,-1}, {0,1}};
        for(int ny = 0; ny < SubChunk.CHUNKSIZEHEIGHT; ny++){
            for(int[] dir : neighbors){
                int nx = x + dir[0];
                int nz = z + dir[1];

                Chunk chunk = this;
                SubChunk sub = chunk.getSubChunkAt(ny);

                //if (nx < 0 || nx >= SubChunk.CHUNKSIZEXZ || nz < 0 || nz >= SubChunk.CHUNKSIZEXZ) continue;

                // si voisin en dehors alors on va voir dans le chunk à coté
                if (nx >= SubChunk.CHUNKSIZEXZ) { 
                    if (this.getSubChunkAt(ny).getNeighborWest() != null){
                        sub = this.getSubChunkAt(ny).getNeighborWest();
                        chunk = sub.parent;
                        nx = 0;
                    }else{continue;}
                }
                if (nx < 0) { 
                    if (this.getSubChunkAt(ny).getNeighborEast() != null){
                        sub = this.getSubChunkAt(ny).getNeighborEast();
                        chunk = sub.parent;
                        nx = 15;
                    }else{continue;}
                }
                if (nz >= SubChunk.CHUNKSIZEXZ) { 
                    if (this.getSubChunkAt(ny).getNeighborNorth() != null){
                        sub = this.getSubChunkAt(ny).getNeighborNorth();
                        chunk = sub.parent;
                        nz = 0;
                    }else{continue;}                   
                }
                if (nz < 0) { 
                    if (this.getSubChunkAt(ny).getNeighborSouth() != null){
                        sub = this.getSubChunkAt(ny).getNeighborSouth();
                        chunk = sub.parent;
                        nz = 15;
                    }else{continue;}                  
                }

                if (chunk.getBlock(nx, ny, nz).transparent == true){
                    int surfaceY = chunk.heightMap[nz + nx * SubChunk.CHUNKSIZEXZ];
                    sub = chunk.getSubChunkAt(ny);
                    int yInSub = ny % SubChunk.CHUNKSIZEXZ;  
                    // si au dessus de la height map alors tous à 15 car soleil
                    if (ny >= surfaceY){
                        sub.setBlockSkyLight(nx, yInSub, nz, (byte)15);
                        LightManager.addSkyLight(chunk, nx, ny, nz, (byte)15);
                        continue;                    
                    }

                    // sinon si le dessous est éclairé par qlq chose et que > 1 il doit aussi propager ça lumière sans la changer
                    int light = sub.getBlockSkyLight(nx, yInSub, nz);
                    if (light > 1){
                        LightManager.addSkyLight(chunk, nx, ny, nz, (byte)light);
                    }
                }
            }
        }
        LightManager.updateLight();
    }

    /**
     * Retourne un block dans le chunk.
     * @param x Coordonnées en x (local)
     * @param y Coordonnées en y (global)
     * @param z Coordonnées en z (local)
     * @return Retourne le block trouvé
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
     * Ajoute un block dans le chunk <br> 
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
     * Retire un block dans le chunk <br>
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
        // si le chunk est visible on l'affiche
        if (FrustumCulling.testAABB(this.minX, this.minY, this.minZ, this.maxX, this.maxY, this.maxZ)){
            for (SubChunk sub : subChunks){
                if (sub.mesh.getVAO() == 0 && sub.transparentMesh.getVAO() == 0) continue; // subchunk vide, rien à dessiner
                sub.drawNode(parentModel);
            } 
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
