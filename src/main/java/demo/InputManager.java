package demo;

import org.joml.Vector2f;
import org.lwjgl.glfw.GLFW;

import demo.Events.KeyboardInput;
import demo.Events.MouseInput;

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

    public void create(long windowRef){
        this.mouseInput = new MouseInput(windowRef);
        this.keyboardInput = new KeyboardInput(windowRef);
    }

    /**
     * Détecte les inputs clavier/souris et effectue les actions en conséquence
     * @param window Window
     * @param sceneGraph SceneGraph
     * @param deltaTime long
     */
    public void run(Window window, SceneGraph sceneGraph, long deltaTime){
        // Récupère le dernier mouvement de la souris
        this.mouseInput.input();

        // Récupère les inputs clavier/souris
        float move = deltaTime * this.MOVEMENTSPEED;
        Camera camera = sceneGraph.getCamera();

        // Mouvement devant, derrière, gauche, droite, haut, bas
        if (this.keyboardInput.isKeyPressed(GLFW.GLFW_KEY_W)) {
            camera.moveForward(move);
        } else if (this.keyboardInput.isKeyPressed(GLFW.GLFW_KEY_S)) {
            camera.moveBackwards(move);
        }
        if (this.keyboardInput.isKeyPressed(GLFW.GLFW_KEY_A)) {
            camera.moveLeft(move);
        } else if (this.keyboardInput.isKeyPressed(GLFW.GLFW_KEY_D)) {
            camera.moveRight(move);
        }
        if (this.keyboardInput.isKeyPressed(GLFW.GLFW_KEY_LEFT_SHIFT)) {
            camera.moveUp(move);
        } else if (this.keyboardInput.isKeyPressed(GLFW.GLFW_KEY_LEFT_CONTROL)) {
            camera.moveDown(move);
        }

        // Quitte la fenêtre
        if (this.keyboardInput.isKeyPressed(GLFW.GLFW_KEY_ESCAPE)){
            window.closeWindow();
        }

        // Déplace la caméra avec la souris si le click droit est appuyé
        if (this.mouseInput.isRightButtonPressed()) {
            Vector2f displVec = this.mouseInput.getDisplVec();
            camera.addRotation((float) Math.toRadians(-displVec.x * this.MOUSESENS),
                               (float) Math.toRadians(-displVec.y * this.MOUSESENS));
        }
    }
}
