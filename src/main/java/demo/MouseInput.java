package demo;

import org.joml.Vector2f;

import org.lwjgl.glfw.GLFW;

public class MouseInput {
    
    private Vector2f currentPos;
    private Vector2f displVec;
    private boolean inWindow;
    private boolean leftButtonPressed;
    private boolean rightButtonPressed;
    private Vector2f previousPos;

    /**
     * Définit les listeners de GLFW
     * @param windowRef long
     */
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
     * Retourne la position de la souris actuelle
     * @return Vector2f
     */
    public Vector2f getDisplVec() {
        return this.displVec;
    }

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

    public boolean isLeftButtonPressed() {
        return this.leftButtonPressed;
    }

    public boolean isRightButtonPressed() {
        return this.rightButtonPressed;
    }
}
