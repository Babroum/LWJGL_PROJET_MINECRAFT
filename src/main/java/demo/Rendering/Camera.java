package demo.Rendering;

import java.nio.FloatBuffer;

import org.joml.*;
import org.lwjgl.BufferUtils;

import demo.Annotations.OpenGLThread;

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
    @OpenGLThread
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
    @OpenGLThread
    public Matrix4f getViewMatrix(){
        return this.viewMatrix;
    }

    /**
     * Retourne la matrice sous forme de FloatBuffer pour OpenGL
     * @return FloatBuffer
     */
    @OpenGLThread
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
    @OpenGLThread
    public void moveBackwards(float inc){
        this.viewMatrix.positiveZ(this.direction).negate().mul(inc);
        this.position.sub(this.direction);
        this.recalculate();
    }

    /**
     * Permet de descendre
     * Modifie le vecteur up qui modifie la viewMatrice
     * @param inc float
     */
    @OpenGLThread
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
    @OpenGLThread
    public void moveForward(float inc){
        this.viewMatrix.positiveZ(this.direction).negate().mul(inc);
        this.position.add(this.direction);
        this.recalculate();
    }

    /**
     * Permet d'aller à gauche
     * Modifie le vecteur right qui modifie la viewMatrice
     * @param inc float
     */
    @OpenGLThread
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
    @OpenGLThread
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
    @OpenGLThread
    public void moveUp(float inc){
        this.viewMatrix.positiveY(this.up).mul(inc);
        this.position.add(this.up);
        this.recalculate();
    }

    /**
     * Recalcule la viewMatrix
     * @param inc float
     */
    @OpenGLThread
    private void recalculate(){
        viewMatrix.identity().rotateX(this.rotation.x).rotateY(this.rotation.y).translate(-position.x, -position.y, -position.z);
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
