package demo.Game;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

import demo.Managers.ChunksManager;
import demo.Managers.PlayersManager;
import demo.Others.BlockDefinition;
import demo.Others.Blocks;
import demo.Rendering.BlockBorderRenderer;
import demo.Rendering.Camera;
import demo.Rendering.Render;
import demo.Annotations.OpenGLThread;
import demo.Events.KeyboardInput;
import demo.Events.MouseInput;
import demo.Geometry.Chunk;
import demo.Geometry.SubChunk;

/**
 * Une joueur 
 */
public class Player {

    /**
     * Vitesse de déplacement de la caméra
     */
    public static final float MOVEMENTSPEED = 0.005f;
    /**
     * Sensibilité de la souris
     */
    public static final float MOUSESENS = 0.1f;
    
    private Camera camera = new Camera();

    public Vector3f position;
    private float playerHeight = 1.5f; // si le joueur est grand ou pas
    private float playerRadius = 0.3f; // si le joueur est gros ou pas
    
    private boolean isFalling = false; 
    private float velocityY = 0f; // Vitesse de chute/saut
    private final float gravity = -9.81f; // constante de gravité

    private volatile int oldBlockBorderX;
    private volatile int oldBlockBorderY;
    private volatile int oldBlockBorderZ;
    private volatile boolean hasBlockBorder = false;

    private float breakCooldown = 0f;
    private float placeCooldown = 0f;
    private float breakPlaceDelay = 200f; // temps en millisecondes pour casser/poser un bloc

    public BlockDefinition blockInHand = Blocks.COBBLESTONE; // pour test

    // snapshots thread-safe lues par le render thread
    public volatile float prevX, prevY, prevZ;
    public volatile float curX,  curY,  curZ;

    public Player(){
        this.position = new Vector3f(); // indépendant de la caméra
    }

    public void moveForward(float inc) {
        move(this.camera.getForwardDirection(inc));
    }

    public void moveBackwards(float inc) {
        // L'arrière, c'est l'avant inversé
        Vector3f dir = this.camera.getForwardDirection(inc).negate();
        move(dir);
    }

    public void moveLeft(float inc) {
        // La gauche, c'est la droite inversée
        Vector3f dir = this.camera.getRightDirection(inc).negate();
        move(dir);
    }

    public void moveRight(float inc) {
        move(this.camera.getRightDirection(inc));
    }

    private void move(Vector3f dir) {
        float nextX = this.position.x + dir.x;
        // On vérifie si on peut bouger en X en gardant le Z et Y actuels
        if (canMoveToWithBuffer(nextX, this.position.z, this.playerRadius)) {
            this.position.x = nextX;
        }

        float nextZ = this.position.z + dir.z;
        // On vérifie si on peut bouger en Z en gardant le X (éventuellement mis à jour) et Y
        if (canMoveToWithBuffer(this.position.x, nextZ, this.playerRadius)) {
            this.position.z = nextZ;
        }
        
        this.setPosition(this.position.x, this.position.y, this.position.z);
    }

    private boolean canMoveToWithBuffer(float x, float z, float radius) {
        // On vérifie les 4 coins/côtés pour simuler une épaisseur
        return canMoveTo(x + radius, z + radius) && // Coin haut-droite
            canMoveTo(x - radius, z + radius) && // Coin haut-gauche
            canMoveTo(x + radius, z - radius) && // Coin bas-droite
            canMoveTo(x - radius, z - radius);   // Coin bas-gauche
    }
    
    private boolean canMoveTo(float newX, float newZ){
        int bx = newX < 0 ? (int) Math.floor(newX) : (int) newX;
        int bz = newZ < 0 ? (int) Math.floor(newZ) : (int) newZ;
        int by = (int)(this.position.y - this.playerHeight);
        try {
            BlockDefinition blockFeet = ChunksManager.getBlock(bx, by, bz);
            BlockDefinition blockHead = ChunksManager.getBlock(bx, by + 1, bz);
            return blockFeet == Blocks.AIR && blockHead == Blocks.AIR;
        } catch (Exception e) {
            return false;
        }
    }

    public void jump(){
        // Si le joueur ne tombe pas
        if (!this.isFalling  && !isBlockCeiling()){
            this.velocityY = 4.95f; // force de saut comme dans minecraft :)
            this.isFalling = true;
        }
    }

    public void updateMovement(float delta){
        // Si le joueur tombe on applique la gravité sur la vitesse de chute
        this.velocityY += this.gravity * delta;
        // Chute par rapport à la vitesse de chute
        this.position.y += velocityY * delta;

        // garde-fou au cas où les chunks ne sont pas encore chargés
        if (this.position.y < 0) {
            this.velocityY = 0f;
            this.isFalling = false;
            this.setPosition(0, 100, 0);
            return;
        }

        // si le joueur monte et touche un plafond
        if (this.velocityY > 0 && this.isBlockCeiling()){
            this.velocityY = 0f; // stoppe la montée
        }

        // si il y a un bloc et que l'on tombe, on arrête de tombé
        if (this.isOnGround() && this.velocityY <= 0) {
            this.velocityY = 0f;
            this.isFalling = false;
            this.position.y = (int)Math.floor(this.position.y - this.playerHeight - 0.01f) + 1 + this.playerHeight;
        } else {
            // sinon on tombe
            this.isFalling = true;
        }
        this.setPosition(this.position.x, this.position.y, this.position.z);
    }

    private boolean isInsidePlayer(int px, int py, int pz){
        // vérifie si le bloc chevauche la boite de collision du joueur
        float minX = this.position.x - this.playerRadius;
        float maxX = this.position.x + this.playerRadius;
        float minZ = this.position.z - this.playerRadius;
        float maxZ = this.position.z + this.playerRadius;
        float minY = this.position.y - this.playerHeight;
        float maxY = this.position.y;
        
        // le bloc occupe de px à px+1, py à py+1, pz à pz+1
        return px + 1 > minX && px < maxX &&
            py + 1 > minY && py < maxY &&
            pz + 1 > minZ && pz < maxZ;
    }

    private boolean isOnGround() {
        // On vérifie sous les pieds à la position Y actuelle (légèrement décalée vers le bas)
        float footY = this.position.y - this.playerHeight - 0.01f; 

        // On teste les 4 coins de la boîte de collision
        return isBlockAt(this.position.x + this.playerRadius, footY, this.position.z + this.playerRadius) ||
            isBlockAt(this.position.x - this.playerRadius, footY, this.position.z + this.playerRadius) ||
            isBlockAt(this.position.x + this.playerRadius, footY, this.position.z - this.playerRadius) ||
            isBlockAt(this.position.x - this.playerRadius, footY, this.position.z - this.playerRadius);
    }

    private boolean isBlockCeiling(){
        float headY = this.position.y + 0.1f;
        return isBlockAt(this.position.x + this.playerRadius, headY, this.position.z + this.playerRadius) ||
            isBlockAt(this.position.x - this.playerRadius, headY, this.position.z + this.playerRadius) ||
            isBlockAt(this.position.x + this.playerRadius, headY, this.position.z - this.playerRadius) ||
            isBlockAt(this.position.x - this.playerRadius, headY, this.position.z - this.playerRadius);
    }

    private boolean isBlockAt(float x, float y, float z) {
        try {
            int bx = (int) Math.floor(x);
            int by = (int) Math.floor(y);
            int bz = (int) Math.floor(z);
            return ChunksManager.getBlock(bx, by, bz) != Blocks.AIR;
        } catch (Exception e) {
            return false;
        }
    }

    // sauve la position au début du tick, avant tout mouvement
    public void savePrevPosition() {
        this.prevX = this.position.x;
        this.prevY = this.position.y;
        this.prevZ = this.position.z;
    }

    // Appelé depuis le GameLoop thread
    public void playerLoopLogic(float delta) {
        this.savePrevPosition(); // sauve prev AVANT tout mouvement

        float move = delta * Player.MOVEMENTSPEED;
        if (KeyboardInput.keyForward)     PlayersManager.player.moveForward(move);
        else if (KeyboardInput.keyBack)   PlayersManager.player.moveBackwards(move);
        if (KeyboardInput.keyLeft)        PlayersManager.player.moveLeft(move);
        else if (KeyboardInput.keyRight)  PlayersManager.player.moveRight(move);

        if (this.breakCooldown > 0) this.breakCooldown -= delta;
        if (this.placeCooldown > 0) this.placeCooldown -= delta;

        Vector3f[] look = this.camera.getLookAt(5);

        if (look != null) {
            Vector3f hit   = look[0];
            Vector3f place = look[1];
            try {
                int bx = (int) Math.floor(hit.x);
                int by = (int) Math.floor(hit.y);
                int bz = (int) Math.floor(hit.z);

                BlockDefinition block = ChunksManager.getBlock(bx, by, bz);
                Chunk chunk = ChunksManager.getChunkAt(bx, bz);
                SubChunk sub = chunk.getSubChunkAt(by);

                int lx = ChunksManager.floorMod(bx, SubChunk.CHUNKSIZEXZ);
                int ly = by % SubChunk.CHUNKSIZEXZ;
                int lz = ChunksManager.floorMod(bz, SubChunk.CHUNKSIZEXZ);
                Render.monitor.setBlockLightRayCast(sub.getBlockLight(lx, ly + 1, lz));
                Render.monitor.setSkyLightRayCast(sub.getBlockSkyLight(lx, ly + 1, lz));

                // casser un bloc
                if (MouseInput.isLeftButtonPressed() && KeyboardInput.isMouseLock) {
                    if (block != Blocks.AIR && this.breakCooldown <= 0f && block != Blocks.BEDROCK) {
                        final SubChunk subToModify = sub;
                        final int fbx = bx, fby = by, fbz = bz;
                        chunkModifyThread.submit(() -> {
                            int localX = ChunksManager.floorMod(fbx, SubChunk.CHUNKSIZEXZ);
                            int localY = ChunksManager.floorMod(fby, SubChunk.CHUNKSIZEXZ);
                            int localZ = ChunksManager.floorMod(fbz, SubChunk.CHUNKSIZEXZ);
                            subToModify.removeBlockFromSubChunk(localX, localY, localZ);
                        });
                        this.breakCooldown = this.breakPlaceDelay;
                    }
                } else {
                    this.breakCooldown = 0;
                }

                // poser un bloc
                if (MouseInput.isRightButtonPressed() && KeyboardInput.isMouseLock) {
                    int px = (int) Math.floor(place.x);
                    int py = (int) Math.floor(place.y);
                    int pz = (int) Math.floor(place.z);
                    if (ChunksManager.getBlock(px, py, pz) == Blocks.AIR
                            && this.placeCooldown <= 0f
                            && !this.isInsidePlayer(px, py, pz)) {
                        try {
                            Chunk chunk2 = ChunksManager.getChunkAt(px, pz);
                            final SubChunk sub2 = chunk2.getSubChunkAt(py);
                            chunkModifyThread.submit(() -> {
                                int localX = ChunksManager.floorMod(px, SubChunk.CHUNKSIZEXZ);
                                int localY = ChunksManager.floorMod(py, SubChunk.CHUNKSIZEXZ);
                                int localZ = ChunksManager.floorMod(pz, SubChunk.CHUNKSIZEXZ);
                                sub2.addBlockToSubChunk(blockInHand, localX, localY, localZ);
                            });
                            this.placeCooldown = this.breakPlaceDelay;
                        } catch (Exception e) {}
                    }
                } else {
                    this.placeCooldown = 0;
                }

                this.hasBlockBorder = (block != Blocks.AIR);
                if (this.hasBlockBorder){
                    this.oldBlockBorderX = bx;
                    this.oldBlockBorderY = by;
                    this.oldBlockBorderZ = bz;
                }

            } catch (Exception e) {}
        } else {
            this.hasBlockBorder = false;
        }

        this.updateMovement(delta / 1000f);

        // snapshot courante après physique, lue par le render thread
        this.curX = this.position.x;
        this.curY = this.position.y;
        this.curZ = this.position.z;
    }

    // Appelé depuis le thread OpenGL (Render)
    @OpenGLThread
    public void playerLoopRender() {
        float alpha = (float)(System.nanoTime() - GameLoop.lastTickTime) / (float) GameLoop.NS_PER_TICK;
        if (alpha > 1f) alpha = 1f;

        float ix = this.prevX + (this.curX - this.prevX) * alpha;
        float iy = this.prevY + (this.curY - this.prevY) * alpha;
        float iz = this.prevZ + (this.curZ - this.prevZ) * alpha;
        this.camera.setPosition(ix, iy, iz);

        if (this.hasBlockBorder) { 
            GL11.glLineWidth(4.0f);
            BlockBorderRenderer.draw(
                this.oldBlockBorderX,
                this.oldBlockBorderY,
                this.oldBlockBorderZ
            );
            GL11.glLineWidth(1.0f);
        }
    }

    // un seul thread dédié
    private static ExecutorService chunkModifyThread = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        return t;
    });

    /**
     * Retourne la camera de la scène
     * @return Camera
     */
    public Camera getCamera(){
        return this.camera;
    }

    /**
     * Retourne la position du joueur 
     * @return
     */
    public Vector3f getPosition(){
        return this.position;
    }

    /**
     * Définis la position du joueur
     * @param x float
     * @param y float
     * @param z float
     */
    public void setPosition(float x, float y, float z){
        this.position.x = x;
        this.position.y = y;
        this.position.z = z;
    }
}
