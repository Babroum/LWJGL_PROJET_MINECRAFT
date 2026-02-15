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
    private long windowRef;

    public boolean isWireFrameMode;
    public boolean isCloseWindow;

    @OpenGLThread
    public KeyboardInput(long windowRef){
        this.windowRef = windowRef;
        GLFW.glfwSetKeyCallback(windowRef, this::keyPressedToggle);
    }

    // utiliser pour toutes les touches toggle
    // ne fait pas d'action mais modifie un boolean
    // les actions sont traitées dans le InputManager
    @OpenGLThread
    private void keyPressedToggle(long window, int key, int scancode, int action, int mods) {   
        if(action == GLFW.GLFW_PRESS){
            switch (key) {
                case GLFW.GLFW_KEY_E:
                    this.isWireFrameMode = !this.isWireFrameMode;
                    break;
                
                case GLFW.GLFW_KEY_ESCAPE:
                    this.isCloseWindow = true;
                    break;

                default:
                    break;
            }
        }        
    }

    // utiliser pour toutes les touches avec appui continue
    // Principalement les mouvements
    @OpenGLThread
    public void keyPressedContinue(Camera camera, float move) {
        
        // Mouvement devant, derrière, gauche, droite, haut, bas
        if (GLFW.glfwGetKey(this.windowRef, GLFW.GLFW_KEY_W) == GLFW.GLFW_PRESS) {
            camera.moveForward(move);
        } else if (GLFW.glfwGetKey(this.windowRef, GLFW.GLFW_KEY_S) == GLFW.GLFW_PRESS) {
            camera.moveBackwards(move);
        }
        if (GLFW.glfwGetKey(this.windowRef, GLFW.GLFW_KEY_A) == GLFW.GLFW_PRESS) {
            camera.moveLeft(move);
        } else if (GLFW.glfwGetKey(this.windowRef, GLFW.GLFW_KEY_D) == GLFW.GLFW_PRESS) {
            camera.moveRight(move);
        }
        if (GLFW.glfwGetKey(this.windowRef, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS) {
            camera.moveUp(move);
        } else if (GLFW.glfwGetKey(this.windowRef, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS) {
            camera.moveDown(move);
        }    
    }
}
