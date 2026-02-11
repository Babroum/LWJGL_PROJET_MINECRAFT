package demo.Exemple;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWWindowCloseCallbackI;

class WindowCloseEventManager implements GLFWWindowCloseCallbackI{

    /**
     * Appeler au moment qu’une fenêtre est sur le point de se fermer
     * @param window Référence sur la fenêtre entrain de se fermer
     */
    @Override
    public void invoke(long window){
        this.closeWindow(window);
    }


    // Documentation sur la méthode, pour voir il faut passer la souris dessus
    /**
     * Gère la fermeture d’une fenêtre 
     * @param window Référence sur la fenêtre à fermer
     */
    void closeWindow(long window){
        GLFW.glfwSetWindowShouldClose(window, true);
    }
}