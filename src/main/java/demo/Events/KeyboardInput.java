package demo.Events;

import org.lwjgl.glfw.GLFW;

import demo.Annotations.OpenGLThread;
import demo.Enum.BlocksType;
import demo.Geometry.Chunk;
import demo.Geometry.SubChunk;
import demo.Managers.AudioManager;
import demo.Managers.ChunksManager;
import demo.Managers.PlayersManager;
import demo.Rendering.Camera;
import demo.Rendering.Window;
import demo.Player;

/**
 * Définis les listeners GLFW
 * <br>Et détecte les appui continue/toggle
 */
public class KeyboardInput {
    
    // volatile car ils sont tous écrits et lus depuis des threads différents
    // volatile force le thread à toujours lire et écrire directement en RAM (pas en cache)
    public static volatile boolean isWireFrameMode;
    public static volatile boolean isCloseWindow;
    public static volatile boolean isCursorHidden = true;
    public static volatile boolean isMouseLock = true;
    public static volatile boolean isTeleportActive;
    public static volatile boolean isChunkBorderShow;
    public static volatile boolean isJumping;

    @OpenGLThread
    public KeyboardInput(){
        GLFW.glfwSetKeyCallback(Window.window, KeyboardInput::keyPressedToggle);
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
                    if (KeyboardInput.isChunkBorderShow){
                        for (Chunk chunk : ChunksManager.getAllChunks()){
                            for (SubChunk sub : chunk.subChunks){
                                if (sub.mesh.getBorderVAO() == 0 && sub.mesh.getVertexCount() > 0){
                                    sub.mesh.createSubChunkBorderVBOVAO();
                                }
                            }
                        }
                    } else {
                        for (Chunk chunk : ChunksManager.getAllChunks()){
                            for (SubChunk sub : chunk.subChunks){
                                sub.mesh.deleteSubChunkBorderVBOVAO();
                            }
                        }
                    }
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
                    break;

                case GLFW.GLFW_KEY_1:
                    PlayersManager.player.blockInHand = BlocksType.COBBLESTONE;
                    break;

                case GLFW.GLFW_KEY_2:
                    PlayersManager.player.blockInHand = BlocksType.GLOWSTONE;
                    break;

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
        if (GLFW.glfwGetKey(Window.window, GLFW.GLFW_KEY_W) == GLFW.GLFW_PRESS) {
            player.moveForward(move);
        } else if (GLFW.glfwGetKey(Window.window, GLFW.GLFW_KEY_S) == GLFW.GLFW_PRESS) {
            player.moveBackwards(move);
        }
        if (GLFW.glfwGetKey(Window.window, GLFW.GLFW_KEY_A) == GLFW.GLFW_PRESS) {
            player.moveLeft(move);
        } else if (GLFW.glfwGetKey(Window.window, GLFW.GLFW_KEY_D) == GLFW.GLFW_PRESS) {
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
