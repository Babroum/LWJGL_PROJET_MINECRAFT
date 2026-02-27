package demo;

import org.joml.Vector3f;

import demo.Managers.ChunksManager;
import demo.Rendering.Camera;
import demo.Geometry.Object3D;

/**
 * Une joueur 
 */
public class Player {
    
    private Camera camera = new Camera();

    public Vector3f position;
    private float playerHeight = 2f;
    private boolean isFalling = false; 
    private float velocityY = 0f; // Vitesse de chute/saut
    private final float gravity = -9.81f; // constante de gravité

    public Player(){
        this.position = this.camera.getPosition();
    }

    public void moveForward(float move){
        camera.moveForward(move);
    }
    public void moveBackwards(float move){
        camera.moveBackwards(move);
    }
    public void moveLeft(float move){
        camera.moveLeft(move);
    }
    public void moveRight(float move){
        camera.moveRight(move);
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
            this.position.y = this.playerHeight;
            this.velocityY = 0f;
            this.isFalling = false;
            this.camera.setPosition(0, 5, 0);
            return;
        }

        try {
            int blockX = (int)this.position.x;
            int blockY = (int)(this.position.y - this.playerHeight);
            int blockZ = (int)this.position.z;
            
            Object3D block = ChunksManager.getBlock(blockX, blockY, blockZ);
            if (block != null && this.velocityY <= 0) {
                this.position.y = (int)(this.position.y - this.playerHeight) + 1 + this.playerHeight;
                this.velocityY = 0f;
                this.isFalling = false;
            } else {
                this.isFalling = true;
            }
        } catch (Exception e) {
            System.out.println("[updateMovement] exception: " + e.getMessage());
            this.isFalling = false; // chunk pas encore chargé
        }
        this.camera.setPosition(this.position.x, this.position.y, this.position.z);
    }

    /**
     * Retourne la camera de la scène
     * @return Camera
     */
    public Camera getCamera(){
        return this.camera;
    }
}
