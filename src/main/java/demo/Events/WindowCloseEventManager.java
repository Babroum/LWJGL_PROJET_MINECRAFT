package demo.Events;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWWindowCloseCallbackI;

/**
 * Management de la fermeture de fenêtre
 */
// Permet de fermer proprement la fenêtre
public class WindowCloseEventManager implements GLFWWindowCloseCallbackI{
    /**
     * Appeler au moment qu’une fenêtre est sur le point de se fermer
     * @param window long
     */
    @Override
    public void invoke(long window)
    {
        this.closeWidow(window);
    }

    /**
     * Gère la fermeture d’une fenêtre 
     * @param window long
     */
    public void closeWidow(long window)
    {
        // Pour le moment on autorise la fenêtre à se fermer dans tous les cas
        GLFW.glfwSetWindowShouldClose(window, true);
    }
}
