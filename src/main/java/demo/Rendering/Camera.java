package demo.Rendering;

import java.nio.FloatBuffer;

import org.joml.*;
import org.lwjgl.BufferUtils;

import demo.Geometry.Object3D;
import demo.Managers.ChunksManager;

// source : https://ahbejarano.gitbook.io/lwjglgamedev/chapter-08
/**
 * Une camera
 */
public class Camera {
    
    private Vector3f direction;
    private Vector3f position;
    private Vector3f right;
    private Vector2f rotation;
    private Vector3f up;
    private Matrix4f viewMatrix;
    private FloatBuffer matrixBuffer = BufferUtils.createFloatBuffer(16);

    public Camera() {
        this.direction = new Vector3f();
        this.right = new Vector3f();
        this.up = new Vector3f();
        this.position = new Vector3f();
        this.viewMatrix = new Matrix4f();
        this.rotation = new Vector2f();
    }

    /**
     * Ajoute une rotation au vecteur de rotation
     * @param x float
     * @param y float
     */
    public void addRotation(float x, float y){
        this.rotation.add(x, y);
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
     * Fonctionne comme un raycast
     * @param distance
     * @return
     */
    public Vector3f getLookAt(float distance){
        Vector3f pos = new Vector3f(this.position); // copie de la position de la caméra
        Vector3f step = new Vector3f(this.direction).mul(0.2f); // vecteur de 0.2 dans la direction du regard

        // à chaque itération on avance pos d'un pas jusqu'a distance
        for (float i = 0; i < distance; i += 0.5f) {
            pos.add(step);
            try {
                // récupère le block en poss
                Object3D block = ChunksManager.getBlock((int)pos.x, (int)pos.y, (int)pos.z);
                // si il existe
                if (block != null) {
                    return new Vector3f(pos); // on retourne la position flottante du point de contact
                }
            } catch (Exception e) {
                // ray cast en dehors du monde
            }
        }
        return null; // on a parcouru toute la distance sans trouver de bloc

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
     * Permet de reculer
     * Modifie le vecteur de position qui modifie la viewMatrice
     * @param inc float
     */
    public void moveBackwards(float inc){
        this.viewMatrix.positiveZ(this.direction).negate().mul(inc);
        this.direction.y = 0; // pour bouger comme un joueur 
        this.position.sub(this.direction);
        this.recalculate();
    }

    /**
     * Permet de descendre
     * Modifie le vecteur up qui modifie la viewMatrice
     * @param inc float
     */
    public void moveDown(float inc){
        this.viewMatrix.positiveY(this.up).mul(inc);
        this.position.sub(this.up);
        this.recalculate();
    }

    /**
     * Permet d'avancer
     * Modifie le vecteur de position qui modifie la viewMatrice
     * @param inc float
     */
    public void moveForward(float inc){
        this.viewMatrix.positiveZ(this.direction).negate().mul(inc);
        this.direction.y = 0; // pour bouger comme un joueur 
        this.position.add(this.direction);
        this.recalculate();
    }

    /**
     * Permet d'aller à gauche
     * Modifie le vecteur right qui modifie la viewMatrice
     * @param inc float
     */
    public void moveLeft(float inc){
        this.viewMatrix.positiveX(this.right).mul(inc);
        this.position.sub(this.right);
        this.recalculate();
    }

    /**
     * Permet d'aller à droite
     * Modifie le vecteur right qui modifie la viewMatrice
     * @param inc float
     */
    public void moveRight(float inc){
        this.viewMatrix.positiveX(this.right).mul(inc);
        this.position.add(this.right);
        this.recalculate();
    }

    /**
     * Permet de monter
     * Modifie le vecteur up qui modifie la viewMatrice
     * @param inc float
     */
    public void moveUp(float inc){
        this.viewMatrix.positiveY(this.up).mul(inc);
        this.position.add(this.up);
        this.recalculate();
    }

    /**
     * Recalcule la viewMatrix
     * @param inc float
     */
    private void recalculate(){
        this.viewMatrix.identity().rotateX(this.rotation.x).rotateY(this.rotation.y).translate(-position.x, -position.y, -position.z);
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
     * @param z float
     */
    public void setRotation(float x, float y){
        this.rotation.set(x, y);
        this.recalculate();
    }
}
