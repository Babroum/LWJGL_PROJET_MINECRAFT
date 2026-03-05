package demo.Rendering;

import java.nio.FloatBuffer;
import java.lang.Math;

import org.joml.*;
import org.lwjgl.BufferUtils;

import demo.Enum.BlocksType;
import demo.Managers.ChunksManager;

/**
 * Une camera
 */
public class Camera {
    
    private Vector3f position;
    private Vector3f direction;
    private Matrix4f viewMatrix;
    private FloatBuffer matrixBuffer = BufferUtils.createFloatBuffer(16);
    private float yaw = 0;   // Rotation gauche/droite
    private float pitch = 0; // Rotation haut/bas

    public Camera() {
        this.direction = new Vector3f();
        this.position = new Vector3f();
        this.viewMatrix = new Matrix4f();
    }

    /**
     * Ajoute une rotation au vecteur de rotation
     * @param rx float
     * @param ry float
     */
    public void addRotation(float rx, float ry) {
        // rx correspond souvent au mouvement horizontal de la souris (Yaw)
        // ry au mouvement vertical (Pitch)
        this.yaw += Math.toDegrees(rx);
        this.pitch += Math.toDegrees(ry);

        // bloque la vue verticale pour éviter de se retourner
        if (pitch > 89f) pitch = 89f;
        if (pitch < -89f) pitch = -89f;
        
        this.recalculate();
    }

    /**
     * Retourne le vecteur de position
     * @return Vector3f
     */
    public Vector3f getPosition(){
        return this.position;
    }

    /**
     * Retourne la viewMatrix
     * @return Matrix4f
     */
    public Matrix4f getViewMatrix(){
        return this.viewMatrix;
    }

    /**
     * Fonctionne comme un raycast <br>
     * Raycast DDA (Digital Differential Analysis)
     * @param distance float
     * @return Vector3f[], [0] bloc visé et [1] où poser
     */
    public Vector3f[] getLookAt(float distance){
        Vector3f pos = new Vector3f(this.position);
        Vector3f dir = new Vector3f(this.direction);
        
        // bloc de départ : la case dans laquelle se trouve la caméra
        int bx = (int) Math.floor(pos.x);
        int by = (int) Math.floor(pos.y);
        int bz = (int) Math.floor(pos.z);
        
        // direction du pas : +1 ou -1 selon le sens du regard sur chaque axe
        int stepX = dir.x > 0 ? 1 : -1;
        int stepY = dir.y > 0 ? 1 : -1;
        int stepZ = dir.z > 0 ? 1 : -1;
        
        // tMaxX/Y/Z : distance le long du rayon jusqu'à la PROCHAINE frontière de bloc sur chaque axe
        // si on regarde vers +X depuis x=2.3, la prochaine frontière X est à x=3.0
        // si on regarde vers -X depuis x=2.3, la prochaine frontière X est à x=2.0
        // (stepX > 0 ? 1 : 0) choisit la bonne frontière selon le sens
        float tMaxX = (dir.x != 0) ? Math.abs((bx + (stepX > 0 ? 1 : 0) - pos.x) / dir.x) : Float.MAX_VALUE;
        float tMaxY = (dir.y != 0) ? Math.abs((by + (stepY > 0 ? 1 : 0) - pos.y) / dir.y) : Float.MAX_VALUE;
        float tMaxZ = (dir.z != 0) ? Math.abs((bz + (stepZ > 0 ? 1 : 0) - pos.z) / dir.z) : Float.MAX_VALUE;
        
        // tDelta : distance à parcourir sur le rayon pour traverser UN bloc complet sur chaque axe
        // si dir.x = 0.5, il faut parcourir 1/0.5 = 2 unités du rayon pour avancer d'1 bloc en X
        float tDeltaX = (dir.x != 0) ? Math.abs(1.0f / dir.x) : Float.MAX_VALUE;
        float tDeltaY = (dir.y != 0) ? Math.abs(1.0f / dir.y) : Float.MAX_VALUE;
        float tDeltaZ = (dir.z != 0) ? Math.abs(1.0f / dir.z) : Float.MAX_VALUE;

        int lastAxis = 0;
        
        // on continue tant que la prochaine frontière est dans la distance max
        while (Math.min(tMaxX, Math.min(tMaxY, tMaxZ)) < distance){
            
            // on avance vers la frontière la plus proche
            if (tMaxX < tMaxY && tMaxX < tMaxZ){
                // la frontière X est la plus proche : on passe dans le bloc voisin en X
                bx += stepX;
                // la prochaine frontière X est maintenant à tDeltaX plus loin
                tMaxX += tDeltaX;
                lastAxis = 0;
            } else if (tMaxY < tMaxZ){
                // la frontière Y est la plus proche
                by += stepY;
                tMaxY += tDeltaY;
                lastAxis = 1;
            } else {
                // la frontière Z est la plus proche
                bz += stepZ;
                tMaxZ += tDeltaZ;
                lastAxis = 2;
            }
            
            try {
                BlocksType block = ChunksManager.getBlock(bx, by, bz);
                // dès qu'on trouve un bloc on retourne sa position entière exacte
                if (block != BlocksType.NULL){
                    int placeX = bx - (lastAxis == 0 ? stepX : 0);
                    int placeY = by - (lastAxis == 1 ? stepY : 0);
                    int placeZ = bz - (lastAxis == 2 ? stepZ : 0);
                    return new Vector3f[]{
                        new Vector3f(bx, by, bz),      // [0] bloc visé
                        new Vector3f(placeX, placeY, placeZ) // [1] où poser
                    };
                }
            } catch (Exception e) {
                // hors du monde chargé
            }
        }
        return null; // aucun bloc trouvé dans la distance
    }

    /**
     * Retourne la matrice sous forme de FloatBuffer pour OpenGL
     * @return FloatBuffer
     */
    public FloatBuffer getViewMatrixBuffer(){
        this.matrixBuffer.clear();
        this.getViewMatrix().get(matrixBuffer);
        return matrixBuffer;
    }

    /**
     * Permet d'obtenir la direction en avant<br>
     * Ne déplace pas
     * @param inc float
     * @return Vector3f
     */
    public Vector3f getForwardDirection(float inc) {
        // On utilise le Yaw pour calculer X et Z
        float rad = (float) Math.toRadians(yaw);
        
        // Formule standard : 
        // sin(yaw) donne le mouvement latéral
        // cos(yaw) donne le mouvement avant/arrière
        float dx = (float) Math.sin(rad);
        float dz = (float) -Math.cos(rad);
        
        return new Vector3f(dx * inc, 0, dz * inc);
    }

    /**
     * Permet d'obtenir la direction sur le côté<br>
     * Ne déplace pas
     * @param inc float
     * @return Vector3f
     */
    public Vector3f getRightDirection(float inc) {
        // Pour la droite, on décale de 90 degrés
        float rad = (float) Math.toRadians(yaw + 90);
        
        float dx = (float) Math.sin(rad);
        float dz = (float) -Math.cos(rad);
        
        return new Vector3f(dx * inc, 0, dz * inc);
    }

    /**
     * Permet d'obtenir la direction sur le dessus<br>
     * Ne déplace pas
     * @param inc float
     * @return Vector3f
     */
    public Vector3f getUpDirection(float inc){
        return new Vector3f(0, inc, 0);
    }

    public float getYaw() {
        return yaw;
    }

    /**
     * Recalcule la viewMatrix
     */
    private void recalculate(){
        this.viewMatrix.identity().rotateX((float) Math.toRadians(pitch)).rotateY((float) Math.toRadians(yaw)).translate(-position.x, -position.y, -position.z);
        this.viewMatrix.positiveZ(this.direction).negate(); // met à jour la direction
    }

    /**
     * Définis une position pour le vecteur de position
     * @param x float
     * @param y float 
     * @param z float
     */
    public void setPosition(float x, float y, float z){
        this.position.set(x, y, z);
        this.recalculate();
    }

    /**
     * Définis une rotation pour le vecteur de rotation
     * @param x float
     * @param y float 
     */
    public void setRotation(float x, float y) {
        // Si x et y sont passés en radians, on les convertit
        this.pitch = (float) Math.toDegrees(x);
        this.yaw = (float) Math.toDegrees(y);
        this.recalculate();
    }
}
