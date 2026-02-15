package demo.Events;

import org.joml.Vector2f;

import org.lwjgl.glfw.GLFW;

import demo.Annotations.OpenGLThread;

public class MouseInput {
    
    private Vector2f currentPos;
    private Vector2f displVec;
    private boolean inWindow;
    private boolean leftButtonPressed;
    private boolean rightButtonPressed;
    private Vector2f previousPos;

    /**
     * Définis les listeners de GLFW
     * <br>Et stocke la postion de la souris 
     * @param windowRef long
     */
    @OpenGLThread
    public MouseInput(long windowRef){
        this.previousPos = new Vector2f(-1, -1);
        this.currentPos = new Vector2f();
        this.displVec = new Vector2f();
        this.leftButtonPressed = false;
        this.rightButtonPressed = false;
        this.inWindow = false;

        // Callback quand la souris est bougé
        GLFW.glfwSetCursorPosCallback(windowRef, (handle, xpos, ypos) -> {
            currentPos.x = (float) xpos;
            currentPos.y = (float) ypos;
        });
        // Callback quand la souris est dans la fenêtre ou en dehors
        GLFW.glfwSetCursorEnterCallback(windowRef, (handle, entered) -> inWindow = entered);
        // Callback quand un bouton de la souris est cliqué
        GLFW.glfwSetMouseButtonCallback(windowRef, (handle, button, action, mode) -> {
            leftButtonPressed = button == GLFW.GLFW_MOUSE_BUTTON_1 && action == GLFW.GLFW_PRESS;
            rightButtonPressed = button == GLFW.GLFW_MOUSE_BUTTON_2 && action == GLFW.GLFW_PRESS;
        });
    }

    /**
     * Retourne la position de la souris actuelle
     * @return Vector2f
     */
    public Vector2f getCurrentPos() {
        return this.currentPos;
    }

    /**
     * Retourne le vecteur entre l'ancienne et la nouvelle position de la souris
     * @return Vector2f
     */
    @OpenGLThread
    public Vector2f getDisplVec() {
        return this.displVec;
    }

    /**
     * Calcule le vecteur de déplacement de la souris
     */
    @OpenGLThread
    public void input() {
        this.displVec.x = 0;
        this.displVec.y = 0;
        if (this.previousPos.x > 0 && this.previousPos.y > 0 && this.inWindow) {
            double deltax = this.currentPos.x - this.previousPos.x;
            double deltay = this.currentPos.y - this.previousPos.y;
            boolean rotateX = deltax != 0;
            boolean rotateY = deltay != 0;
            if (rotateX) {
                this.displVec.y = (float) deltax;
            }
            if (rotateY) {
                this.displVec.x = (float) deltay;
            }
        }
        this.previousPos.x = this.currentPos.x;
        this.previousPos.y = this.currentPos.y;
    }

    /**
     * Retourne un boolean en fonction de si le click gauche de la souris est enfoncé
     * @return boolean
     */
    @OpenGLThread
    public boolean isLeftButtonPressed() {
        return this.leftButtonPressed;
    }

    /**
     * Retourne un boolean en fonction de si le click droit de la souris est enfoncé
     * @return boolean
     */
    @OpenGLThread
    public boolean isRightButtonPressed() {
        return this.rightButtonPressed;
    }
}
