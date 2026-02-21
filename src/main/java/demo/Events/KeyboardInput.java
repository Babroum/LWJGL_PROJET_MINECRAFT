package demo.Events;

import org.lwjgl.glfw.GLFW;

import demo.Annotations.OpenGLThread;
import demo.Rendering.Camera;

/**
 * Définis les listeners GLFW
 * <br>Et détecte les appui continue/toggle
 * @param windowRef
 */
public class KeyboardInput {
    
    private static long windowRef;
    public static boolean isWireFrameMode;
    public static boolean isCloseWindow;
    public static boolean isCursorHidden = true;
    public static boolean isMouseLock = true;
    public static boolean isChunkBorderShow;

    @OpenGLThread
    public KeyboardInput(long windowRef){
        KeyboardInput.windowRef = windowRef;
        GLFW.glfwSetKeyCallback(windowRef, KeyboardInput::keyPressedToggle);
    }

    // utiliser pour toutes les touches toggle
    // ne fait pas d'action mais modifie un boolean
    // les actions sont traitées dans le InputManager
    @OpenGLThread
    private static void keyPressedToggle(long window, int key, int scancode, int action, int mods) {   
        if(action == GLFW.GLFW_PRESS){
            switch (key) {
                case GLFW.GLFW_KEY_E:
                    KeyboardInput.isWireFrameMode = !KeyboardInput.isWireFrameMode;
                    break;
                
                case GLFW.GLFW_KEY_ESCAPE:
                    KeyboardInput.isCloseWindow = true;
                    break;
                
                case GLFW.GLFW_KEY_P:
                    KeyboardInput.isMouseLock = !KeyboardInput.isMouseLock;
                    break;

                case GLFW.GLFW_KEY_B:
                    KeyboardInput.isChunkBorderShow = !KeyboardInput.isChunkBorderShow;
                    break;

                default:
                    break;
            }
        }        
    }

    // utiliser pour toutes les touches avec appui continue
    // Principalement pour les mouvements
    @OpenGLThread
    public static void keyPressedContinue(long windowRef, Camera camera, float move) {
        
        // Mouvement devant, derrière, gauche, droite, haut, bas
        if (GLFW.glfwGetKey(KeyboardInput.windowRef, GLFW.GLFW_KEY_W) == GLFW.GLFW_PRESS) {
            camera.moveForward(move);
        } else if (GLFW.glfwGetKey(KeyboardInput.windowRef, GLFW.GLFW_KEY_S) == GLFW.GLFW_PRESS) {
            camera.moveBackwards(move);
        }
        if (GLFW.glfwGetKey(KeyboardInput.windowRef, GLFW.GLFW_KEY_A) == GLFW.GLFW_PRESS) {
            camera.moveLeft(move);
        } else if (GLFW.glfwGetKey(KeyboardInput.windowRef, GLFW.GLFW_KEY_D) == GLFW.GLFW_PRESS) {
            camera.moveRight(move);
        }
        if (GLFW.glfwGetKey(KeyboardInput.windowRef, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS) {
            camera.moveUp(move);
        } else if (GLFW.glfwGetKey(KeyboardInput.windowRef, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS) {
            camera.moveDown(move);
        }    
    }
}
