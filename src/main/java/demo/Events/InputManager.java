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
    public static final float MOVEMENTSPEED = 0.005f;
    /**
     * Sensibilité de la souris
     */
    public static final float MOUSESENS = 0.1f;

    @OpenGLThread
    public static void create(long windowRef){
        new MouseInput(windowRef);
        new KeyboardInput(windowRef);
    }

    /**
     * Fait les actions en fonction des inputs
     * @param window Window
     * @param sceneGraph SceneGraph
     * @param deltaTime long
     */
    @OpenGLThread
    public static void run(long windowRef, SceneGraph sceneGraph, long deltaTime){
        // Récupère le dernier mouvement de la souris
        MouseInput.input();

        // Récupère les inputs clavier/souris
        float move = deltaTime * InputManager.MOVEMENTSPEED;
        Camera camera = sceneGraph.getCamera();

        // détecte les appuie continues
        KeyboardInput.keyPressedContinue(windowRef, camera, move);

        // Active/désactive le wireframe/cull face
        if (KeyboardInput.isWireFrameMode){
            //GL11.glDisable(GL11.GL_CULL_FACE); 
            GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE); 
        }else{
            //GL11.glEnable(GL11.GL_CULL_FACE); 
            GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL); 
        }

        // Ferme la fenêtre
        if (KeyboardInput.isCloseWindow){
            Window.closeWindow();
        }

        // Déplace la caméra avec la souris et la bloque au milieu, desactive si pas sélectionné ou la souris débloqué
        if (GLFW.glfwGetWindowAttrib(Window.getWindow(),GLFW.GLFW_FOCUSED) == 1 && KeyboardInput.isMouseLock){
            Vector2f displVec = MouseInput.getDisplVec();
            camera.addRotation((float) Math.toRadians(displVec.x * InputManager.MOUSESENS),
                            (float) Math.toRadians(displVec.y * InputManager.MOUSESENS));
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
