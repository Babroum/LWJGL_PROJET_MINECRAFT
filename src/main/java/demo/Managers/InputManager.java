package demo.Managers;

import org.joml.Vector2f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.glfw.GLFW;

import demo.Annotations.OpenGLThread;
import demo.Events.KeyboardInput;
import demo.Events.MouseInput;
import demo.Game.Player;
import demo.Rendering.Camera;
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
    // Appelé depuis le thread OpenGL (Render)
    @OpenGLThread
    public static void runOpenGL() {
        MouseInput.input(); // capture le delta souris via GLFW
        KeyboardInput.pollMovementKeys();

        if (KeyboardInput.isWireFrameMode) {
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE);
        } else {
            GL11.glEnable(GL11.GL_CULL_FACE);
            GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);
        }

        // rotation caméra avec la souris
        boolean focused = GLFW.glfwGetWindowAttrib(Window.getWindow(), GLFW.GLFW_FOCUSED) == 1;
        if (focused && KeyboardInput.isMouseLock) {
            Vector2f displVec = MouseInput.getDisplVec();
            Camera camera = PlayersManager.player.getCamera();
            camera.addRotation(
                (float) Math.toRadians(displVec.x * Player.MOUSESENS),
                (float) Math.toRadians(displVec.y * Player.MOUSESENS)
            );
        }

        // curseur
        if (KeyboardInput.isCursorHidden) {
            GLFW.glfwSetInputMode(Window.getWindow(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_HIDDEN);
        } else {
            GLFW.glfwSetInputMode(Window.getWindow(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
        }
    }

    // Appelé depuis le GameLoop thread
    public static void runLogic(float deltaTime) {

        // thread safe, GLFW le lit depuis le render thread au prochain pollEvents()
        if (KeyboardInput.isCloseWindow) GLFW.glfwSetWindowShouldClose(Window.getWindow(), true);

        if (KeyboardInput.isJumping) {
            KeyboardInput.isJumping = false;
            PlayersManager.player.jump();
        }

        if (KeyboardInput.isTeleportActive) {
            KeyboardInput.isTeleportActive = false;
            PlayersManager.player.setPosition(5, 100, 5);
        }

        // met à jour le flag curseur pour que runOpenGL le lise
        KeyboardInput.isCursorHidden = KeyboardInput.isMouseLock;
    }
}
