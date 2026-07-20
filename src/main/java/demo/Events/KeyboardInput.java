package demo.Events;

import org.lwjgl.glfw.GLFW;

import demo.Annotations.OpenGLThread;
import demo.Geometry.Chunk;
import demo.Geometry.SubChunk;
import demo.Managers.AudioManager;
import demo.Managers.ChunksManager;
import demo.Managers.PlayersManager;
import demo.Others.Blocks;
import demo.Rendering.Window;

/**
 * Définis les listeners GLFW
 * <br>Et détecte les appui continue/toggle
 */
public class KeyboardInput {
    
    // volatile car ils sont tous écrits et lus depuis des threads différents
    // volatile force le thread à toujours lire et écrire directement en RAM (pas en cache)
    /** Indique si le mode fil de fer (wireframe) est actif. */
    public static volatile boolean isWireFrameMode;
    /** Indique si une demande de fermeture de la fenêtre a été faite. */
    public static volatile boolean isCloseWindow;
    /** Indique si le curseur de la souris est caché. */
    public static volatile boolean isCursorHidden = true;
    /** Indique si la souris est verrouillée/capturée par la fenêtre. */
    public static volatile boolean isMouseLock = true;
    /** Indique si une téléportation a été demandée. */
    public static volatile boolean isTeleportActive;
    /** Indique si l'affichage des bordures de chunk est actif. */
    public static volatile boolean isChunkBorderShow;
    /** Indique si une demande de saut a été faite. */
    public static volatile boolean isJumping;
    /** Indique si l'affichage du benchmark est actif. */
    public static volatile boolean isBenchMarkShow;

    // états des touches de mouvement, écrits par le thread OpenGL, lus par le game thread
    /** Etat de la touche avancer (W). */
    public static volatile boolean keyForward;
    /** Etat de la touche reculer (S). */
    public static volatile boolean keyBack;
    /** Etat de la touche aller à gauche (A). */
    public static volatile boolean keyLeft;
    /** Etat de la touche aller à droite (D). */
    public static volatile boolean keyRight;

    /**
     * Créer les listeners clavier de GLFW
     */
    @OpenGLThread
    public KeyboardInput(){
        GLFW.glfwSetKeyCallback(Window.window, KeyboardInput::keyPressedToggle);
    }

    // utiliser pour toutes les touches toggle
    // ne fait pas d'action mais modifie un boolean
    // les actions sont traitées dans le InputManager
    /**
     * Callback GLFW appelé à chaque appui de touche, gère les touches toggle (bascule un boolean, l'action est traitée dans l'InputManager)
     * @param window long, référence de la fenêtre
     * @param key int, code de la touche
     * @param scancode int, code matériel de la touche
     * @param action int, type d'action (press/release/repeat)
     * @param mods int, modificateurs actifs (shift, ctrl, etc.)
     */
    @OpenGLThread
    private static void keyPressedToggle(long window, int key, int scancode, int action, int mods) {   
        if(action == GLFW.GLFW_PRESS){
            switch (key) {
                case GLFW.GLFW_KEY_F1:
                    KeyboardInput.isWireFrameMode = !KeyboardInput.isWireFrameMode;
                    break;
                
                case GLFW.GLFW_KEY_ESCAPE:
                    KeyboardInput.isCloseWindow = true;
                    break;
                
                case GLFW.GLFW_KEY_F2:
                    KeyboardInput.isMouseLock = !KeyboardInput.isMouseLock;
                    break;

                case GLFW.GLFW_KEY_F3:
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

                case GLFW.GLFW_KEY_F4:
                    KeyboardInput.isTeleportActive = true;
                    break;

                case GLFW.GLFW_KEY_F5:
                    KeyboardInput.isBenchMarkShow = !KeyboardInput.isBenchMarkShow;
                    break;

                case GLFW.GLFW_KEY_KP_ADD:
                    AudioManager.setMasterVolume(AudioManager.getMasterVolume() + 1);
                    break;

                case GLFW.GLFW_KEY_KP_SUBTRACT:
                    AudioManager.setMasterVolume(AudioManager.getMasterVolume() - 1);
                    break;

                case GLFW.GLFW_KEY_SPACE:
                    KeyboardInput.isJumping = true;
                    break;

                case GLFW.GLFW_KEY_1:
                    PlayersManager.player.blockInHand = Blocks.COBBLESTONE;
                    break;

                case GLFW.GLFW_KEY_2:
                    PlayersManager.player.blockInHand = Blocks.TORCH;
                    break;
                
                case GLFW.GLFW_KEY_3:
                    PlayersManager.player.blockInHand = Blocks.GLASS;
                    break;

                default:
                    break;
            }
        }        
    }

    // appelé depuis le thread OpenGL uniquement : lit glfwGetKey et met à jour les volatile
    /**
     * Récupère les appuies des touches de déplacements (avant, arrière, gauche, droite)
     */
    @OpenGLThread
    public static void pollMovementKeys() {
        keyForward = GLFW.glfwGetKey(Window.window, GLFW.GLFW_KEY_W) == GLFW.GLFW_PRESS;
        keyBack    = GLFW.glfwGetKey(Window.window, GLFW.GLFW_KEY_S) == GLFW.GLFW_PRESS;
        keyLeft    = GLFW.glfwGetKey(Window.window, GLFW.GLFW_KEY_A) == GLFW.GLFW_PRESS;
        keyRight   = GLFW.glfwGetKey(Window.window, GLFW.GLFW_KEY_D) == GLFW.GLFW_PRESS;
    }
}