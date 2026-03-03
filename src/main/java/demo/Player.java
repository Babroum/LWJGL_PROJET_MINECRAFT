package demo;

import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

import demo.Managers.BuffersManager;
import demo.Managers.ChunksManager;
import demo.Managers.PlayersManager;
import demo.Rendering.Camera;
import demo.Enum.BlocksType;
import demo.Events.KeyboardInput;
import demo.Events.MouseInput;
import demo.Geometry.Object3D;
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
    private Object3D oldBlockBorder;
    private float breakCooldown = 0f;
    private float placeCooldown = 0f;
    private float breakPlaceDelay = 200f; // temps en millisecondes pour casser/poser un bloc

    public Player(){
        this.position = this.camera.getPosition();
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
        
        this.setPosition(new Vector3f(this.position.x, this.position.y, this.position.z));
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
            Object3D blockFeet = ChunksManager.getBlock(bx, by, bz);
            Object3D blockHead = ChunksManager.getBlock(bx, by + 1, bz);
            return blockFeet == null && blockHead == null;
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
            this.setPosition(new Vector3f(0, 10, 0));
            return;
        }

        // si le joueur monte et touche un plafond
        if (this.velocityY > 0 && this.isBlockCeiling()){
            this.velocityY = 0f; // stoppe la montée
        }

        // si il y a un block et que l'on tombe, on arrête de tombé
        if (this.isOnGround() && this.velocityY <= 0) {
            this.velocityY = 0f;
            this.isFalling = false;
            this.position.y = (int)Math.floor(this.position.y - this.playerHeight - 0.01f) + 1 + this.playerHeight;
        } else {
            // sinon on tombe
            this.isFalling = true;
        }
        this.setPosition(new Vector3f(this.position.x, this.position.y, this.position.z));
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
            return ChunksManager.getBlock(bx, by, bz) != null;
        } catch (Exception e) {
            return false;
        }
    }

    public void playerLoop(float delta){
        if (this.breakCooldown > 0) {
            this.breakCooldown -= delta;
        }
        if (this.placeCooldown > 0){
            this.placeCooldown -= delta;
        }

        // récupère les coordonées de ce que regarde le joueur (raycast)
        Vector3f[] look = PlayersManager.player.getCamera().getLookAt(5);
        Object3D block = null;
        SubChunk sub = null;

        // si on regarde quelque chose
        if (look != null){   
            Vector3f hit = look[0];
            Vector3f place = look[1];
            try {
                // on floor pour évité les arrondis, 0.8 = bloc 0 et pas 1
                int bx = (int) Math.floor(hit.x);
                int by = (int) Math.floor(hit.y);
                int bz = (int) Math.floor(hit.z);

                // récupère le block à tel position
                block = ChunksManager.getBlock(bx, by, bz);
                // pareil pour le chunk
                sub = ChunksManager.getSubChunk(bx, by, bz);

                // si on clique gauche et que la souris n'est pas débloqué, on casse le block si il existe
                if (MouseInput.isLeftButtonPressed() && KeyboardInput.isMouseLock) {
                    // Si on a un bloc et que le cooldown est prêt
                    if (block != null && this.breakCooldown <= 0f) {
                        // le thread ne prend que des constantes, donc on copie 
                        final SubChunk subToModify = sub;
                        final Object3D blockToRemove = block;
                        block = null; // l'original a null
                        Thread t = new Thread(() -> {
                            subToModify.removeBlockFromSubChunk(blockToRemove);
                        });
                        t.setDaemon(true);
                        t.start();
                        this.breakCooldown = this.breakPlaceDelay;
                        block = null; 
                    }
                } else {
                    this.breakCooldown = 0;
                }  
                
                // si on clique droit et que la souris n'est pas débloqué, on pose le block
                if (MouseInput.isRightButtonPressed() && KeyboardInput.isMouseLock) {
                    int px = (int) Math.floor(place.x);
                    int py = (int) Math.floor(place.y);
                    int pz = (int) Math.floor(place.z);
                    // on vérifie pour etre sur qu'il n'y ait pas de bloc
                    if (ChunksManager.getBlock(px, py, pz) == null && this.placeCooldown <= 0f && !this.isInsidePlayer(px, py, pz)){
                        try {
                            final SubChunk sub2 = ChunksManager.getSubChunk(px, py, pz);
                            // convertir en coordonnées locales du subchunk
                            int localX = ChunksManager.floorMod(px, SubChunk.CHUNKSIZEX);
                            int localY = ChunksManager.floorMod(py, SubChunk.CHUNKSIZEY);
                            int localZ = ChunksManager.floorMod(pz, SubChunk.CHUNKSIZEZ);
                            Thread t = new Thread(() -> {
                                sub2.addBlockToSubChunk(BlocksType.STONE, localX, localY, localZ);
                            });
                            t.setDaemon(true);
                            t.start();
                            this.placeCooldown = this.breakPlaceDelay;
                        } catch (Exception e) {
                            // hors du monde
                        }
                    } 
                } else {
                    this.placeCooldown = 0;
                }     

                // si le block regarder n'est pas le même que le précédent on créer les buffers
                if (block != null && block != this.oldBlockBorder){
                    if (this.oldBlockBorder != null){ BuffersManager.deleteVBOVAO(oldBlockBorder.ids); }
                    this.oldBlockBorder = block;
                    block.createSubChunkBorderVBOVAO();
                }

                // dessine à chaque frame si un bloc est regardé
                if (this.oldBlockBorder != null){
                    GL11.glLineWidth(4.0f); // épaisseur en pixels
                    this.oldBlockBorder.drawBlockBorder(bx, by, bz);
                    GL11.glLineWidth(1.0f); // reset après
                }
            } catch(Exception e){
                // le chunk n'existe pas
            }
        } else {
            // Si on ne regarde rien 
            if (this.oldBlockBorder != null) {
                BuffersManager.deleteVBOVAO(oldBlockBorder.ids);
                this.oldBlockBorder = null;
            }
        }  

        this.updateMovement(delta / 1000f); // en seconde
    }

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
     * Définis la postition du joueur
     * @param pos
     */
    public void setPosition(Vector3f pos){
        this.position = pos;
        this.camera.setPosition(pos.x, pos.y, pos.z);
    }
}
