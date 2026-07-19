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
 * Le joueur : position, déplacement, collisions, actions (casser/poser un bloc).
 */
public class Player {

    /**
     * Vitesse de déplacement de base.
     */
    public static final float MOVEMENTSPEED = 0.005f;

    /**
     * Sensibilité de la souris.
     */
    public static final float MOUSESENS = 0.1f;

    /**
     * Hauteur maximale d'un obstacle que le joueur peut monter automatiquement (auto-step).
     * 8 sub-unités sur 16 = un demi-bloc.
     */
    private static final float STEP_HEIGHT = 8f / 16f;

    /**
     * Petite marge utilisée dans les comparaisons de position pour éviter
     * les erreurs d'arrondi flottant sur les frontières de bloc.
     */
    private static final float EPSILON = 0.001f;

    private Camera camera = new Camera();

    public Vector3f position;
    private float playerHeight = 1.5f; // hauteur du joueur
    private float playerRadius = 0.3f; // largeur du joueur

    private boolean isFalling = false;
    private float velocityY = 0f; // vitesse de chute/saut
    private final float gravity = -9.81f; // constante de gravité

    /**
     * Hauteur (en coordonnées monde) du sommet du bloc/objet sous les pieds du joueur.
     * Mise à jour à chaque fois que isOnGround détecte un contact.
     */
    private float lastGroundSurfaceY = 0f;

    private volatile int oldBlockBorderX;
    private volatile int oldBlockBorderY;
    private volatile int oldBlockBorderZ;
    private volatile boolean hasBlockBorder = false;

    private float breakCooldown = 0f;
    private float placeCooldown = 0f;
    private float breakPlaceDelay = 200f; // délai en millisecondes pour casser/poser un bloc

    public BlockDefinition blockInHand = Blocks.COBBLESTONE; // pour test

    // snapshots thread-safe lues par le render thread
    public volatile float prevX, prevY, prevZ;
    public volatile float curX,  curY,  curZ;

    public Player(){
        this.position = new Vector3f(); // indépendant de la caméra
    }

    /**
     * Déplace le joueur vers l'avant.
     * @param inc vitesse du déplacement
     */
    public void moveForward(float inc) {
        move(this.camera.getForwardDirection(inc));
    }

    /**
     * Déplace le joueur vers l'arrière.
     * @param inc vitesse du déplacement
     */
    public void moveBackwards(float inc) {
        Vector3f dir = this.camera.getForwardDirection(inc).negate();
        move(dir);
    }

    /**
     * Déplace le joueur vers la gauche.
     * @param inc vitesse du déplacement
     */
    public void moveLeft(float inc) {
        Vector3f dir = this.camera.getRightDirection(inc).negate();
        move(dir);
    }

    /**
     * Déplace le joueur vers la droite.
     * @param inc vitesse du déplacement
     */
    public void moveRight(float inc) {
        move(this.camera.getRightDirection(inc));
    }

    /**
     * Déplace le joueur en X puis en Z, avec auto-step si un petit obstacle bloque le passage.
     * @param dir direction du déplacement
     */
    private void move(Vector3f dir) {
        float nextX = this.position.x + dir.x;
        if (tryMoveTo(nextX, this.position.z)) {
            this.position.x = nextX;
        }

        float nextZ = this.position.z + dir.z;
        if (tryMoveTo(this.position.x, nextZ)) {
            this.position.z = nextZ;
        }

        this.setPosition(this.position.x, this.position.y, this.position.z);
    }

    /**
     * Tente un déplacement horizontal. Si le passage est libre, autorise le mouvement.
     * Si bloqué par un obstacle bas (hauteur <= STEP_HEIGHT), relève le joueur automatiquement.
     * @param newX nouvelle position X
     * @param newZ nouvelle position Z
     * @return true si le déplacement est possible (avec ou sans auto-step)
     */
    private boolean tryMoveTo(float newX, float newZ) {
        float currentFootY = this.position.y - this.playerHeight;

        // passage libre à la hauteur actuelle
        if (canMoveToAtFootY(newX, newZ, currentFootY)) {
            return true;
        }

        // bloqué : on regarde si l'obstacle est assez bas pour être monté automatiquement
        float obstacleTop = getObstacleTopHeight(newX, newZ, currentFootY);
        if (obstacleTop < 0f) {
            return false; // aucun obstacle bas identifié, mur normal
        }

        float stepDelta = obstacleTop - currentFootY;
        if (stepDelta <= 0f || stepDelta > STEP_HEIGHT + EPSILON) {
            return false; // pas un step valide (trop haut, ou rien à monter)
        }

        // on vérifie qu'il y a la place en hauteur une fois monté (pas de plafond)
        if (canMoveToAtFootY(newX, newZ, obstacleTop)) {
            this.position.y = obstacleTop + this.playerHeight;
            return true;
        }

        return false;
    }

    /**
     * Vérifie si le joueur peut occuper la position (newX, newZ) avec les pieds à footY,
     * sans collisionner avec un bloc. On ajoute un epsilon au-dessus de footY pour ne pas
     * compter comme collision le simple contact avec la surface sur laquelle on se tient.
     * @param newX position X testée
     * @param newZ position Z testée
     * @param footY hauteur des pieds testée
     * @return true si la position est libre
     */
    private boolean canMoveToAtFootY(float newX, float newZ, float footY) {
        float minX = newX - this.playerRadius;
        float maxX = newX + this.playerRadius;
        float minZ = newZ - this.playerRadius;
        float maxZ = newZ + this.playerRadius;
        float minY = footY + EPSILON; // ignore le contact avec la surface, ne teste que le volume au-dessus
        float maxY = footY + this.playerHeight;
        return !isBlockAt(minX, maxX, minY, maxY, minZ, maxZ);
    }

    /**
     * Cherche la hauteur du sommet du bloc/objet qui bloque le passage à (x, z),
     * en scannant uniquement la tranche entre les pieds actuels et pieds + STEP_HEIGHT.
     * @param x position X testée
     * @param z position Z testée
     * @param currentFootY hauteur actuelle des pieds
     * @return hauteur du sommet le plus haut trouvé, ou -1 si aucun obstacle bas
     */
    private float getObstacleTopHeight(float x, float z, float currentFootY) {
        float minX = x - this.playerRadius;
        float maxX = x + this.playerRadius;
        float minZ = z - this.playerRadius;
        float maxZ = z + this.playerRadius;
        float minY = currentFootY - EPSILON;
        float maxY = currentFootY + STEP_HEIGHT + EPSILON;

        int bxMin = (int) Math.floor(minX + EPSILON), bxMax = (int) Math.floor(maxX - EPSILON);
        int byMin = (int) Math.floor(minY),           byMax = (int) Math.floor(maxY);
        int bzMin = (int) Math.floor(minZ + EPSILON), bzMax = (int) Math.floor(maxZ - EPSILON);

        float highest = -1f;

        for (int bx = bxMin; bx <= bxMax; bx++) {
            for (int by = byMin; by <= byMax; by++) {
                for (int bz = bzMin; bz <= bzMax; bz++) {
                    try {
                        BlockDefinition block = ChunksManager.getBlock(bx, by, bz);
                        if (block == Blocks.AIR || block == Blocks.WATER) continue;

                        float lMinX = (Math.max(minX, bx)     - bx) * 16;
                        float lMaxX = (Math.min(maxX, bx + 1) - bx) * 16;
                        float lMinY = (Math.max(minY, by)     - by) * 16;
                        float lMaxY = (Math.min(maxY, by + 1) - by) * 16;
                        float lMinZ = (Math.max(minZ, bz)     - bz) * 16;
                        float lMaxZ = (Math.min(maxZ, bz + 1) - bz) * 16;

                        if (ChunksManager.isCollidingBlockAt(block, lMinX, lMaxX, lMinY, lMaxY, lMinZ, lMaxZ)) {
                            float surfaceY = by + block.height / 16f;
                            if (surfaceY > highest) highest = surfaceY;
                        }
                    } catch (Exception e) {
                        // chunk pas chargé, on ignore cette cellule
                    }
                }
            }
        }

        return highest;
    }

    /**
     * Fait sauter le joueur si un saut est possible (pas déjà en chute, pas de plafond).
     */
    public void jump(){
        if (!this.isFalling && !isBlockCeiling()){
            this.velocityY = 4.95f; // force de saut comme dans minecraft
            this.isFalling = true;
        }
    }

    /**
     * Met à jour la physique du joueur : gravité, chute, snap au sol.
     * @param delta temps écoulé depuis le dernier appel
     */
    public void updateMovement(float delta){
        this.velocityY += this.gravity * delta;
        this.position.y += velocityY * delta;

        // garde-fou si les chunks ne sont pas encore chargés
        if (this.position.y < 0) {
            this.velocityY = 0f;
            this.isFalling = false;
            this.setPosition(0, 100, 0);
            return;
        }

        // si le joueur monte et touche un plafond
        if (this.velocityY > 0 && this.isBlockCeiling()){
            this.velocityY = 0f;
        }

        // si le joueur touche le sol en tombant, on le pose exactement sur le vrai sommet du bloc
        if (this.isOnGround() && this.velocityY <= 0) {
            this.velocityY = 0f;
            this.isFalling = false;
            this.position.y = this.lastGroundSurfaceY + this.playerHeight;
        } else {
            this.isFalling = true;
        }

        this.setPosition(this.position.x, this.position.y, this.position.z);
    }

    /**
     * Vérifie si le bloc qu'on s'apprête à poser (px, py, pz) chevauche le joueur.
     * Utilise la vraie bounding box de l'objet posé, pas un cube plein.
     * @param px position X du bloc à poser
     * @param py position Y du bloc à poser
     * @param pz position Z du bloc à poser
     * @return true si l'objet posé chevaucherait le joueur
     */
    private boolean isInsidePlayer(int px, int py, int pz){
        BlockDefinition placing = this.blockInHand;

        float objMinX = px + placing.x / 16f;
        float objMaxX = px + (placing.x + placing.width)  / 16f;
        float objMinY = py + placing.y / 16f;
        float objMaxY = py + (placing.y + placing.height) / 16f;
        float objMinZ = pz + placing.z / 16f;
        float objMaxZ = pz + (placing.z + placing.depth)  / 16f;

        float minX = this.position.x - this.playerRadius;
        float maxX = this.position.x + this.playerRadius;
        float minZ = this.position.z - this.playerRadius;
        float maxZ = this.position.z + this.playerRadius;
        float minY = this.position.y - this.playerHeight;
        float maxY = this.position.y;

        return objMaxX > minX && objMinX < maxX &&
               objMaxY > minY && objMinY < maxY &&
               objMaxZ > minZ && objMinZ < maxZ;
    }

    /**
     * Vérifie si le joueur est sur le sol. Teste une fine tranche Y sous les pieds
     * et retient la hauteur du sommet touché dans lastGroundSurfaceY.
     * @return true si un support est détecté sous les pieds
     */
    private boolean isOnGround() {
        float minX = this.position.x - this.playerRadius;
        float maxX = this.position.x + this.playerRadius;
        float minZ = this.position.z - this.playerRadius;
        float maxZ = this.position.z + this.playerRadius;
        float footY = this.position.y - this.playerHeight - EPSILON;
        float footYTop = footY + 0.05f;

        return findGroundSurface(minX, maxX, footY, footYTop, minZ, maxZ);
    }

    /**
     * Vérifie s'il y a un bloc au-dessus de la tête du joueur (plafond).
     * @return true si un obstacle est détecté au-dessus de la tête
     */
    private boolean isBlockCeiling(){
        float minX = this.position.x - this.playerRadius;
        float maxX = this.position.x + this.playerRadius;
        float minZ = this.position.z - this.playerRadius;
        float maxZ = this.position.z + this.playerRadius;
        float headY = this.position.y + 0.1f;
        float headYTop = headY + 0.02f;

        return isBlockAt(minX, maxX, headY, headYTop, minZ, maxZ);
    }

    /**
     * Recherche un support sous la tranche Y donnée et retient la hauteur du sommet
     * le plus haut trouvé dans lastGroundSurfaceY.
     * @return true si un support est trouvé
     */
    private boolean findGroundSurface(float minX, float maxX, float minY, float maxY, float minZ, float maxZ) {
        int bxMin = (int) Math.floor(minX), bxMax = (int) Math.floor(maxX);
        int byMin = (int) Math.floor(minY), byMax = (int) Math.floor(maxY);
        int bzMin = (int) Math.floor(minZ), bzMax = (int) Math.floor(maxZ);

        boolean found = false;
        float highest = Float.NEGATIVE_INFINITY;

        for (int bx = bxMin; bx <= bxMax; bx++) {
            for (int by = byMin; by <= byMax; by++) {
                for (int bz = bzMin; bz <= bzMax; bz++) {
                    try {
                        BlockDefinition block = ChunksManager.getBlock(bx, by, bz);
                        if (block == Blocks.AIR || block == Blocks.WATER) continue;

                        float lMinX = (Math.max(minX, bx)     - bx) * 16;
                        float lMaxX = (Math.min(maxX, bx + 1) - bx) * 16;
                        float lMinY = (Math.max(minY, by)     - by) * 16;
                        float lMaxY = (Math.min(maxY, by + 1) - by) * 16;
                        float lMinZ = (Math.max(minZ, bz)     - bz) * 16;
                        float lMaxZ = (Math.min(maxZ, bz + 1) - bz) * 16;

                        if (ChunksManager.isCollidingBlockAt(block, lMinX, lMaxX, lMinY, lMaxY, lMinZ, lMaxZ)) {
                            float surfaceY = by + block.height / 16f;
                            if (surfaceY > highest) highest = surfaceY;
                            found = true;
                        }
                    } catch (Exception e) {
                        // chunk pas chargé, on ignore cette cellule
                    }
                }
            }
        }

        if (found) this.lastGroundSurfaceY = highest;
        return found;
    }

    /**
     * Vérifie si une bounding box (coordonnées monde) chevauche un bloc solide.
     * Parcourt toutes les cellules touchées par la boîte, pas seulement une.
     * @return true si la boîte chevauche au moins un bloc solide
     */
    private boolean isBlockAt(float minX, float maxX, float minY, float maxY, float minZ, float maxZ) {
        int bxMin = (int) Math.floor(minX), bxMax = (int) Math.floor(maxX);
        int byMin = (int) Math.floor(minY), byMax = (int) Math.floor(maxY);
        int bzMin = (int) Math.floor(minZ), bzMax = (int) Math.floor(maxZ);

        for (int bx = bxMin; bx <= bxMax; bx++) {
            for (int by = byMin; by <= byMax; by++) {
                for (int bz = bzMin; bz <= bzMax; bz++) {
                    try {
                        BlockDefinition block = ChunksManager.getBlock(bx, by, bz);
                        if (block == Blocks.AIR || block == Blocks.WATER) continue;

                        float lMinX = (Math.max(minX, bx)     - bx) * 16;
                        float lMaxX = (Math.min(maxX, bx + 1) - bx) * 16;
                        float lMinY = (Math.max(minY, by)     - by) * 16;
                        float lMaxY = (Math.min(maxY, by + 1) - by) * 16;
                        float lMinZ = (Math.max(minZ, bz)     - bz) * 16;
                        float lMaxZ = (Math.min(maxZ, bz + 1) - bz) * 16;

                        if (ChunksManager.isCollidingBlockAt(block, lMinX, lMaxX, lMinY, lMaxY, lMinZ, lMaxZ)) {
                            return true;
                        }
                    } catch (Exception e) {
                        // chunk pas chargé, on ignore cette cellule
                    }
                }
            }
        }

        return false;
    }

    /**
     * Sauve la position au début du tick, avant tout mouvement.
     */
    public void savePrevPosition() {
        this.prevX = this.position.x;
        this.prevY = this.position.y;
        this.prevZ = this.position.z;
    }

    /**
     * Logique du joueur appelée depuis le GameLoop thread : déplacements, actions.
     * @param delta temps écoulé depuis le dernier tick
     */
    public void playerLoopLogic(float delta) {
        this.savePrevPosition();

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
                    if (this.placeCooldown <= 0f && !this.isInsidePlayer(px, py, pz)) {
                        try {
                            Chunk chunk2 = ChunksManager.getChunkAt(px, pz);
                            final SubChunk sub2 = chunk2.getSubChunkAt(py);
                            lx = ChunksManager.floorMod(px, SubChunk.CHUNKSIZEXZ);
                            ly = ChunksManager.floorMod(py, SubChunk.CHUNKSIZEXZ);
                            lz = ChunksManager.floorMod(pz, SubChunk.CHUNKSIZEXZ);
                            int liquidLevel = sub2.getLiquidLevel(lx, ly, lz);
                            block = ChunksManager.getBlock(px, py, pz);
                            if (block == Blocks.AIR || liquidLevel != block.liquidLevel){
                                chunkModifyThread.submit(() -> {
                                    int localX = ChunksManager.floorMod(px, SubChunk.CHUNKSIZEXZ);
                                    int localY = ChunksManager.floorMod(py, SubChunk.CHUNKSIZEXZ);
                                    int localZ = ChunksManager.floorMod(pz, SubChunk.CHUNKSIZEXZ);
                                    sub2.addBlockToSubChunk(blockInHand, localX, localY, localZ);
                                });
                                this.placeCooldown = this.breakPlaceDelay;
                            }
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

    /**
     * Rendu appelé depuis le thread OpenGL : interpolation de la position, affichage du contour du bloc visé.
     */
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

    // un seul thread dédié aux actions du joueur (casser/poser)
    private static ExecutorService chunkModifyThread = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Player Action Thread");
        t.setDaemon(true);
        return t;
    });

    /**
     * Retourne la caméra du joueur.
     */
    public Camera getCamera(){
        return this.camera;
    }

    /**
     * Retourne la position du joueur.
     */
    public Vector3f getPosition(){
        return this.position;
    }

    /**
     * Définit la position du joueur.
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