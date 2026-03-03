package demo.Events;

import org.lwjgl.glfw.GLFW;

import demo.Annotations.OpenGLThread;
import demo.Managers.AudioManager;
import demo.Rendering.Camera;
import demo.Player;

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
    public static boolean isTeleportActive;
    public static boolean isChunkBorderShow;
    public static boolean isJumping;

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

                case GLFW.GLFW_KEY_T:
                    KeyboardInput.isTeleportActive = true;
                    break;

                case GLFW.GLFW_KEY_KP_ADD:
                    AudioManager.setMasterVolume(AudioManager.getMasterVolume() + 1);
                    break;

                case GLFW.GLFW_KEY_KP_SUBTRACT:
                    AudioManager.setMasterVolume(AudioManager.getMasterVolume() - 1);
                    break;

                case GLFW.GLFW_KEY_SEMICOLON:
                    AudioManager.playOnce("fanta");
                    break;

                case GLFW.GLFW_KEY_SPACE:
                    KeyboardInput.isJumping = true;

                default:
                    break;
            }
        }        
    }

    // utiliser pour toutes les touches avec appui continue
    // Principalement pour les mouvements
    @OpenGLThread
    public static void keyPressedContinue(long windowRef, Camera camera, float move, Player player) {
        
        // Mouvement devant, derrière, gauche, droite, haut, bas
        if (GLFW.glfwGetKey(KeyboardInput.windowRef, GLFW.GLFW_KEY_W) == GLFW.GLFW_PRESS) {
            player.moveForward(move);
        } else if (GLFW.glfwGetKey(KeyboardInput.windowRef, GLFW.GLFW_KEY_S) == GLFW.GLFW_PRESS) {
            player.moveBackwards(move);
        }
        if (GLFW.glfwGetKey(KeyboardInput.windowRef, GLFW.GLFW_KEY_A) == GLFW.GLFW_PRESS) {
            player.moveLeft(move);
        } else if (GLFW.glfwGetKey(KeyboardInput.windowRef, GLFW.GLFW_KEY_D) == GLFW.GLFW_PRESS) {
            player.moveRight(move);
        }

        /* Plus valable pour un joueur 
        if (GLFW.glfwGetKey(KeyboardInput.windowRef, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS) {
            camera.moveUp(move);
        } else if (GLFW.glfwGetKey(KeyboardInput.windowRef, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS) {
            camera.moveDown(move);
        }  */  
    }
}
