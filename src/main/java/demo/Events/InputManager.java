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
    private final float MOVEMENTSPEED = 0.005f;
    /**
     * Sensibilité de la souris
     */
    private final float MOUSESENS = 0.1f;
    
    private MouseInput mouseInput;
    private KeyboardInput keyboardInput;

    @OpenGLThread
    public void create(long windowRef, Window window){
        this.mouseInput = new MouseInput(windowRef, window);
        this.keyboardInput = new KeyboardInput(windowRef);
    }

    /**
     * Fait les actions en fonction des inputs
     * @param window Window
     * @param sceneGraph SceneGraph
     * @param deltaTime long
     */
    @OpenGLThread
    public void run(Window window, SceneGraph sceneGraph, long deltaTime){
        // Récupère le dernier mouvement de la souris
        this.mouseInput.input(window);

        // Récupère les inputs clavier/souris
        float move = deltaTime * this.MOVEMENTSPEED;
        Camera camera = sceneGraph.getCamera();

        // détecte les appuie continues
        this.keyboardInput.keyPressedContinue(camera, move);

        // Active/désactive le wireframe/cull face
        if (this.keyboardInput.isWireFrameMode){
            GL11.glDisable(GL11.GL_CULL_FACE); 
            GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE); 
        }else{
            GL11.glEnable(GL11.GL_CULL_FACE); 
            GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL); 
        }

        // Ferme la fenêtre
        if ( this.keyboardInput.isCloseWindow){
            window.closeWindow();
        }

        // Déplace la caméra avec la souris et la bloque au milieu, desactive si pas selectionne ou en pause
        if (GLFW.glfwGetWindowAttrib(window.getWindow(),GLFW.GLFW_FOCUSED)==1 && !this.keyboardInput.isPaused){
            GLFW.glfwSetCursorPos(window.getWindow(), window.width/2, window.height/2);
            Vector2f displVec = this.mouseInput.getDisplVec();
            camera.addRotation((float) Math.toRadians(displVec.x * this.MOUSESENS),
                            (float) Math.toRadians(displVec.y * this.MOUSESENS));
        }

        if (this.keyboardInput.isCursorHidden){
            GLFW.glfwSetInputMode(window.getWindow(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_HIDDEN);
        }
        else{
            GLFW.glfwSetInputMode(window.getWindow(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
        }

        if (this.keyboardInput.isPaused){
            this.keyboardInput.isCursorHidden=false;
        }
        else{
            this.keyboardInput.isCursorHidden=true;
        }
    }
}
