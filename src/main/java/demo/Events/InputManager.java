package demo.Events;

import org.joml.Vector2f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.glfw.GLFW;

import demo.Annotations.OpenGLThread;
import demo.Rendering.Camera;
import demo.Rendering.SceneGraph;
import demo.Rendering.Window;

public class InputManager {

    /**
     * Vitesse de déplacement de la caméra
     */
    private static final float MOVEMENTSPEED = 0.005f;
    /**
     * Sensibilité de la souris
     */
    private static final float MOUSESENS = 0.1f;

    @OpenGLThread
    public static void create(long windowRef, Window window){
        new MouseInput(windowRef, window);
        new KeyboardInput(windowRef);
    }

    /**
     * Fait les actions en fonction des inputs
     * @param window Window
     * @param sceneGraph SceneGraph
     * @param deltaTime long
     */
    @OpenGLThread
    public static void run(long windowRef, Window window, SceneGraph sceneGraph, long deltaTime){
        // Récupère le dernier mouvement de la souris
        MouseInput.input(window);

        // Récupère les inputs clavier/souris
        float move = deltaTime * InputManager.MOVEMENTSPEED;
        Camera camera = sceneGraph.getCamera();

        // détecte les appuie continues
        KeyboardInput.keyPressedContinue(windowRef, camera, move);

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
            window.closeWindow();
        }

        // Déplace la caméra avec la souris et la bloque au milieu, desactive si pas sélectionné ou la souris débloqué
        if (GLFW.glfwGetWindowAttrib(window.getWindow(),GLFW.GLFW_FOCUSED) == 1 && KeyboardInput.isMouseLock){
            GLFW.glfwSetCursorPos(window.getWindow(), window.width/2, window.height/2);
            Vector2f displVec = MouseInput.getDisplVec();
            camera.addRotation((float) Math.toRadians(displVec.x * InputManager.MOUSESENS),
                            (float) Math.toRadians(displVec.y * InputManager.MOUSESENS));
        }

        // si le curseur est bloqué sur l'écran
        if (KeyboardInput.isMouseLock){
            KeyboardInput.isCursorHidden=true; // on l'affiche
        }
        else{
            KeyboardInput.isCursorHidden=false; // on le cache
        }

        // si le curseur doit être caché alors
        if (KeyboardInput.isCursorHidden){
            GLFW.glfwSetInputMode(window.getWindow(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_HIDDEN); // on le cache
        }
        else{
            GLFW.glfwSetInputMode(window.getWindow(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL); // on l'affiche
        }
    }
}
