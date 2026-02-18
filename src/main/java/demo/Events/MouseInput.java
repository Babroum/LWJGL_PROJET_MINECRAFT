package demo.Events;

import org.joml.Vector2f;
import org.lwjgl.glfw.GLFW;

import demo.Annotations.OpenGLThread;
import demo.Rendering.Window;

public class MouseInput {
    
    private static Vector2f currentPos;
    private static Vector2f displVec;
    private static boolean inWindow;
    private static boolean leftButtonPressed;
    private static boolean rightButtonPressed;
    private static Vector2f previousPos;

    /**
     * Définis les listeners de GLFW
     * <br>Et stocke la postion de la souris 
     * @param windowRef long
     */
    @OpenGLThread
    public MouseInput(long windowRef, Window window){
        MouseInput.previousPos = new Vector2f(-1, -1);
        MouseInput.currentPos = new Vector2f();
        MouseInput.displVec = new Vector2f();
        MouseInput.leftButtonPressed = false;
        MouseInput.rightButtonPressed = false;
        MouseInput.inWindow = false;

        // Callback quand la souris est bougé
        //setCursorPosCallback(windowRef, window);
        GLFW.glfwSetCursorPosCallback(windowRef, (handle, xpos, ypos) -> {
            if (KeyboardInput.isMouseLock){
                currentPos.x = (float) xpos - window.width/2;
                currentPos.y = (float) ypos - window.height/2;
                GLFW.glfwSetCursorPos(windowRef, window.width/2, window.height/2);
            }
        });
        // Callback quand la souris est dans la fenêtre ou en dehors
        GLFW.glfwSetCursorEnterCallback(windowRef, (handle, entered) -> inWindow = entered);
        // Callback quand un bouton de la souris est cliqué
        GLFW.glfwSetMouseButtonCallback(windowRef, (handle, button, action, mode) -> {
            leftButtonPressed = button == GLFW.GLFW_MOUSE_BUTTON_1 && action == GLFW.GLFW_PRESS;
            rightButtonPressed = button == GLFW.GLFW_MOUSE_BUTTON_2 && action == GLFW.GLFW_PRESS;
        });
    }

    /*
    @OpenGLThread
    public void setCursorPosCallback(long windowRef, Window window){
        GLFW.glfwSetCursorPosCallback(windowRef, (handle, xpos, ypos) -> {
            currentPos.x = (float) xpos - window.width/2;
            currentPos.y = (float) ypos - window.height/2;
            GLFW.glfwSetCursorPos(windowRef, window.width/2, window.height/2);
        });
    }
    */

    /**
     * Retourne la position de la souris actuelle
     * @return Vector2f
     */
    public static Vector2f getCurrentPos() {
        return MouseInput.currentPos;
    }

    /**
     * Retourne le vecteur entre l'ancienne et la nouvelle position de la souris
     * @return Vector2f
     */
    @OpenGLThread
    public static Vector2f getDisplVec() {
        return MouseInput.displVec;
    }

    /**
     * Calcule le vecteur de déplacement de la souris
     */
    @OpenGLThread
    public static void input(Window window) {
        MouseInput.displVec.x = 0;
        MouseInput.displVec.y = 0;
        if (MouseInput.previousPos.x > -window.width/2 && MouseInput.previousPos.y > -window.height/2
            && MouseInput.previousPos.x < window.width/2 && MouseInput.previousPos.y < window.height/2 && MouseInput.inWindow) {
            double deltax = currentPos.x;
            double deltay = currentPos.y;
            boolean rotateX = deltax != 0;
            boolean rotateY = deltay != 0;
            if (rotateX) {
                MouseInput.displVec.y = (float) deltax;
            }
            if (rotateY) {
                MouseInput.displVec.x = (float) deltay;
            }
        }
        MouseInput.previousPos.x = MouseInput.currentPos.x;
        MouseInput.previousPos.y = MouseInput.currentPos.y;
    }

    /**
     * Retourne un boolean en fonction de si le click gauche de la souris est enfoncé
     * @return boolean
     */
    @OpenGLThread
    public static boolean isLeftButtonPressed() {
        return MouseInput.leftButtonPressed;
    }

    /**
     * Retourne un boolean en fonction de si le click droit de la souris est enfoncé
     * @return boolean
     */
    @OpenGLThread
    public static boolean isRightButtonPressed() {
        return MouseInput.rightButtonPressed;
    }
}
