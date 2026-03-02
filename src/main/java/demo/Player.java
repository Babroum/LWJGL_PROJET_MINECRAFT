package demo;

import org.joml.Vector3f;

import demo.Managers.ChunksManager;
import demo.Rendering.Camera;
import demo.Geometry.Object3D;

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
    private float playerHeight = 1.5f;
    private float playerRadius = 0.3f; // si le joueur est gros ou pas
    private boolean isFalling = false; 
    private float velocityY = 0f; // Vitesse de chute/saut
    private final float gravity = -9.81f; // constante de gravité

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
        
        // On met à jour la caméra après les deux tests
        this.camera.setPosition(this.position.x, this.position.y, this.position.z);
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
        if (!this.isFalling){
            this.velocityY = 5f; // valeur arbitraire, force du saut
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

        // si il y a un block et que l'on tombe, on arrête de tombé
        if (this.isOnGround() && this.velocityY <= 0) {
            this.position.y = (int)(this.position.y - this.playerHeight) + 1 + this.playerHeight;
            this.velocityY = 0f;
            this.isFalling = false;
        } else {
            // sinon on tombe
            this.isFalling = true;
        }

        this.camera.setPosition(this.position.x, this.position.y, this.position.z);
    }

    public boolean isOnGround() {
        float radius = 0.3f; // Même rayon que pour les murs
        // On vérifie sous les pieds à la position Y actuelle (légèrement décalée vers le bas)
        float footY = this.position.y - this.playerHeight - 0.01f; 

        // On teste les 4 coins de la boîte de collision
        return isBlockAt(this.position.x + radius, footY, this.position.z + radius) ||
            isBlockAt(this.position.x - radius, footY, this.position.z + radius) ||
            isBlockAt(this.position.x + radius, footY, this.position.z - radius) ||
            isBlockAt(this.position.x - radius, footY, this.position.z - radius);
    }

    // Petite méthode utilitaire pour simplifier ton try/catch
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
        this.camera.setPosition(this.position.x, this.position.y, this.position.z);
    }
}
