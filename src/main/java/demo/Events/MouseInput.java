package demo.Events;

import org.joml.Vector2f;
import org.lwjgl.glfw.GLFW;

import demo.Annotations.OpenGLThread;
import demo.Rendering.Window;

/**
 * Définis les listeners GLFW
 * <br>Et détecte les mouvements de souris (quitte l'écran, position, clique)
 * @param windowRef
 */
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
    public MouseInput(long windowRef){
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
                MouseInput.currentPos.x = 0;
                MouseInput.currentPos.y = 0;
                currentPos.x = (float) xpos - Window.width/2;
                currentPos.y = (float) ypos - Window.height/2;
                GLFW.glfwSetCursorPos(windowRef, Window.width/2, Window.height/2);
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
    public static void input() {
        MouseInput.displVec.x = 0;
        MouseInput.displVec.y = 0;
        if (MouseInput.previousPos.x > -Window.width/2 && MouseInput.previousPos.y > -Window.height/2
            && MouseInput.previousPos.x < Window.width/2 && MouseInput.previousPos.y < Window.height/2 && MouseInput.inWindow) {
            double deltax = currentPos.x;
            double deltay = currentPos.y;
            boolean rotateX = deltax != 0;
            boolean rotateY = deltay != 0;
            if (rotateX) {
                MouseInput.displVec.x = (float) deltax;
            }
            if (rotateY) {
                MouseInput.displVec.y = (float) deltay;
            }
        }
        MouseInput.previousPos.x = MouseInput.currentPos.x;
        MouseInput.previousPos.y = MouseInput.currentPos.y;
        MouseInput.currentPos.x = 0;
        MouseInput.currentPos.y = 0;
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
