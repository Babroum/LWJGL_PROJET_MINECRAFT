package demo.Rendering;

import java.nio.FloatBuffer;
import java.lang.Math;

import org.joml.*;
import org.lwjgl.BufferUtils;

import demo.Geometry.Chunk;
import demo.Geometry.SubChunk;
import demo.Managers.ChunksManager;
import demo.Others.BlockDefinition;
import demo.Others.Blocks;

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

    // Champs réutilisés à chaque frame
    private final Vector3f rayPos = new Vector3f();
    private final Vector3f rayDir = new Vector3f();
    private final Vector3f forwardDir = new Vector3f();
    private final Vector3f rightDir = new Vector3f();
    private final Vector3f[] lookAtResult = new Vector3f[]{new Vector3f(), new Vector3f()};

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
        this.rayPos.set(this.position);
        this.rayDir.set(this.direction);
        
        // bloc de départ : la case dans laquelle se trouve la caméra
        int bx = (int) Math.floor(this.rayPos.x);
        int by = (int) Math.floor(this.rayPos.y);
        int bz = (int) Math.floor(this.rayPos.z);
        
        // direction du pas : +1 ou -1 selon le sens du regard sur chaque axe
        int stepX = this.rayDir.x > 0 ? 1 : -1;
        int stepY = this.rayDir.y > 0 ? 1 : -1;
        int stepZ = this.rayDir.z > 0 ? 1 : -1;
        
        // tMaxX/Y/Z : distance le long du rayon jusqu'à la PROCHAINE frontière de bloc sur chaque axe
        // si on regarde vers +X depuis x=2.3, la prochaine frontière X est à x=3.0
        // si on regarde vers -X depuis x=2.3, la prochaine frontière X est à x=2.0
        // (stepX > 0 ? 1 : 0) choisit la bonne frontière selon le sens
        float tMaxX = (this.rayDir.x != 0) ? Math.abs((bx + (stepX > 0 ? 1 : 0) - this.rayPos.x) / this.rayDir.x) : Float.MAX_VALUE;
        float tMaxY = (this.rayDir.y != 0) ? Math.abs((by + (stepY > 0 ? 1 : 0) - this.rayPos.y) / this.rayDir.y) : Float.MAX_VALUE;
        float tMaxZ = (this.rayDir.z != 0) ? Math.abs((bz + (stepZ > 0 ? 1 : 0) - this.rayPos.z) / this.rayDir.z) : Float.MAX_VALUE;
        
        // tDelta : distance à parcourir sur le rayon pour traverser UN bloc complet sur chaque axe
        // si this.rayDir.x = 0.5, il faut parcourir 1/0.5 = 2 unités du rayon pour avancer d'1 bloc en X
        float tDeltaX = (this.rayDir.x != 0) ? Math.abs(1.0f / this.rayDir.x) : Float.MAX_VALUE;
        float tDeltaY = (this.rayDir.y != 0) ? Math.abs(1.0f / this.rayDir.y) : Float.MAX_VALUE;
        float tDeltaZ = (this.rayDir.z != 0) ? Math.abs(1.0f / this.rayDir.z) : Float.MAX_VALUE;

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
                BlockDefinition block = ChunksManager.getBlock(bx, by, bz);
                Chunk chunk = ChunksManager.getChunkAt(bx, bz);
                SubChunk sub = chunk.getSubChunkAt(by);
                int liquidLevel = 0;
                if (block == Blocks.WATER){
                    int lx = ChunksManager.floorMod(bx, SubChunk.CHUNKSIZEXZ);
                    int ly = ChunksManager.floorMod(by, SubChunk.CHUNKSIZEXZ);
                    int lz = ChunksManager.floorMod(bz, SubChunk.CHUNKSIZEXZ);
                    liquidLevel = sub.getLiquidLevel(lx, ly, lz);
                }

                // dès qu'on trouve un bloc/une source d'eau on retourne sa position entière exacte
                if (block != Blocks.AIR && liquidLevel == block.liquidLevel){
                    int placeX = bx - (lastAxis == 0 ? stepX : 0);
                    int placeY = by - (lastAxis == 1 ? stepY : 0);
                    int placeZ = bz - (lastAxis == 2 ? stepZ : 0);
                    this.lookAtResult[0].set(bx, by, bz); // [0] bloc visé
                    this.lookAtResult[1].set(placeX, placeY, placeZ); // [1] où poser
                    return this.lookAtResult;
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
        this.recalculate();
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
        
        return this.forwardDir.set(dx * inc, 0, dz * inc);
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
        
        return this.rightDir.set(dx * inc, 0, dz * inc);
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
