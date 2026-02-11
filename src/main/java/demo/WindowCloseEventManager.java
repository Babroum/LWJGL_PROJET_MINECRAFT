package demo;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWWindowCloseCallbackI;

class WindowCloseEventManager implements GLFWWindowCloseCallbackI{
    /**
     * Appeler au moment qu’une fenêtre est sur le point de se fermer
     * @param window Référence sur la fenêtre entrain de se fermer
     */
    @Override
    public void invoke(long window)
    {
        this.closeWidow(window);
    }

    /**
     * Gère la fermeture d’une fenêtre 
     * @param window Référence sur la fenêtre à fermer
     */
    void closeWidow(long window)
    {
        // Pour le moment on autorise la fenêtre à se fermer dans tous les cas
        GLFW.glfwSetWindowShouldClose(window, true);
    }
}
