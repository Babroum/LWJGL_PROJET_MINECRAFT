package demo.Managers;

import org.joml.Vector2f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.glfw.GLFW;

import demo.Player;
import demo.Annotations.OpenGLThread;
import demo.Events.KeyboardInput;
import demo.Events.MouseInput;
import demo.Rendering.Camera;
import demo.Rendering.SceneGraph;
import demo.Rendering.Window;

public class InputManager {

    @OpenGLThread
    public static void create(){
        new MouseInput();
        new KeyboardInput();
    }

    /**
     * Fait les actions en fonction des inputs
     * @param windowRef long
     * @param sceneGraph SceneGraph
     * @param deltaTime long
     */
    @OpenGLThread
    public static void run(SceneGraph sceneGraph, float deltaTime){
        // Récupère le dernier mouvement de la souris
        MouseInput.input();

        // Récupère les inputs clavier/souris
        float move = deltaTime * Player.MOVEMENTSPEED;
        Camera camera = PlayersManager.player.getCamera();

        // détecte les appuie continues
        KeyboardInput.keyPressedContinue(Window.window, camera, move, PlayersManager.player);

        // Active/désactive le wireframe/cull face
        if (KeyboardInput.isWireFrameMode){
            GL11.glDisable(GL11.GL_CULL_FACE); 
            GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE); 
        }else{
            GL11.glEnable(GL11.GL_CULL_FACE); 
            GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL); 
        }

        // Ferme la fenêtre
        if (KeyboardInput.isCloseWindow){
            Window.exitWindow();
        }

        // Si le joueur saute
        if (KeyboardInput.isJumping){
            KeyboardInput.isJumping = false;
            PlayersManager.player.jump();
        }

        // Si le joueur veut se téléporter
        if (KeyboardInput.isTeleportActive){
            KeyboardInput.isTeleportActive = false;
            PlayersManager.player.setPosition(5, 100, 5);
        }

        // Déplace la caméra avec la souris et la bloque au milieu, desactive si pas sélectionné ou la souris débloqué
        if (GLFW.glfwGetWindowAttrib(Window.getWindow(),GLFW.GLFW_FOCUSED) == 1 && KeyboardInput.isMouseLock){
            Vector2f displVec = MouseInput.getDisplVec();
            camera.addRotation((float) Math.toRadians(displVec.x * Player.MOUSESENS),
                            (float) Math.toRadians(displVec.y * Player.MOUSESENS));
        }

        // si le curseur est bloqué sur l'écran
        if (KeyboardInput.isMouseLock){
            KeyboardInput.isCursorHidden=true; // on le cache
        }
        else{
            KeyboardInput.isCursorHidden=false; // on l'affiche
        }

        // si le curseur doit être caché alors
        if (KeyboardInput.isCursorHidden){
            GLFW.glfwSetInputMode(Window.getWindow(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_HIDDEN); // on le cache
        }
        else{
            GLFW.glfwSetInputMode(Window.getWindow(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL); // on l'affiche
        }
    }
}
