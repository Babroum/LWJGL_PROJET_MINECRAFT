package demo.Events;

import org.lwjgl.glfw.GLFW;

public class KeyboardInput {
    private long windowRef;

    public KeyboardInput(long windowRef){
        this.windowRef = windowRef;
    }

    /**
     * Retourne un boolean en fonction de si une touche est enfoncé
     * @param keyCode int
     * @return boolean
     */
    public boolean isKeyPressed(int keyCode) {
        return GLFW.glfwGetKey(this.windowRef, keyCode) == GLFW.GLFW_PRESS;
    }

    /**
     * Retourne un boolean en fonction de si une touche est relâché
     * @param keyCode int
     * @return boolean
     */
    public boolean isKeyRelease(int keyCode) {
        return GLFW.glfwGetKey(this.windowRef, keyCode) == GLFW.GLFW_RELEASE;
    }
}
