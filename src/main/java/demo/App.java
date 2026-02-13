package demo;

import org.joml.Vector2f;
import org.lwjgl.glfw.GLFW;

import demo.Events.MouseInput;
import demo.Geometry.Cube;
import demo.Types.Color4f;

public class App 
{
    /**
     * Vitesse de déplacement de la caméra
     */
    private static final float MOVEMENTSPEED = 0.005f;
    /**
     * Sensibilité de la souris
     */
    private static final float MOUSESENS = 0.1f;
    
    public static void main( String[] args )
    {
        Window window = new Window(1600,800);
        draw(window.getSceneGraph());
    }

    public static void draw(SceneGraph scene){        
        Cube cube1 = new Cube();
        Clone3D cube2 = new Clone3D(cube1); // replaced Cube cube2 = new Cube(); everything else works the same
        
        scene.root.addChild(cube1);
        scene.root.addChild(cube2);
        
        cube1.setColor(Color4f.BLUE);
        cube1.x = -1f;
        cube1.z = -2f;
        cube1.angleX = 0f;
        cube1.angleY = 0f;

        cube2.setColor(Color4f.RED);        
        cube2.x = 1f;
        cube2.z = -2f;
        cube2.angleX = 0f;
        cube2.angleY = 0f;
    }

    /**
     * Détecte les inputs clavier/souris et effectue les actions en conséquence
     * @param window Window
     * @param sceneGraph SceneGraph
     * @param deltaTime long
     */
    public static void input(Window window, SceneGraph sceneGraph, long deltaTime){
        float move = deltaTime * App.MOVEMENTSPEED;
        Camera camera = sceneGraph.getCamera();

        // Mouvement devant, derrière, gauche, droite, haut, bas
        if (window.getKeyboardInput().isKeyPressed(GLFW.GLFW_KEY_W)) {
            camera.moveForward(move);
        } else if (window.getKeyboardInput().isKeyPressed(GLFW.GLFW_KEY_S)) {
            camera.moveBackwards(move);
        }
        if (window.getKeyboardInput().isKeyPressed(GLFW.GLFW_KEY_A)) {
            camera.moveLeft(move);
        } else if (window.getKeyboardInput().isKeyPressed(GLFW.GLFW_KEY_D)) {
            camera.moveRight(move);
        }
        if (window.getKeyboardInput().isKeyPressed(GLFW.GLFW_KEY_LEFT_SHIFT)) {
            camera.moveUp(move);
        } else if (window.getKeyboardInput().isKeyPressed(GLFW.GLFW_KEY_LEFT_CONTROL)) {
            camera.moveDown(move);
        }

        // Quitte la fenêtre
        if (window.getKeyboardInput().isKeyPressed(GLFW.GLFW_KEY_ESCAPE)){
            window.closeWindow();
        }

        // Déplace la caméra avec la souris si le click droit est appuyé
        MouseInput mouseInput = window.getMouseInput();
        if (mouseInput.isRightButtonPressed()) {
            Vector2f displVec = mouseInput.getDisplVec();
            camera.addRotation((float) Math.toRadians(-displVec.x * App.MOUSESENS),
                               (float) Math.toRadians(-displVec.y * App.MOUSESENS));
                               //(float) Math.toRadians(-displVec.x * App.MOUSESENS)
                               //(float) Math.toRadians(-displVec.y * App.MOUSESENS)
        }
    }

    /*
    mvn clean                                                                                   
    mvn package
    mvn exec:java
    */
}
