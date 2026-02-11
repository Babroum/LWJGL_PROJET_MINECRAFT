package demo;

import static org.lwjgl.system.MemoryUtil.NULL;

import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryUtil;

public class Window {
    
    // Dimension de la fenetre
    /**
     * Largeur de la fenêtre.
     */
    public int width;
    /**
     * Hauteur de la fenêtre.
     */
    public int height;

    // Référence de la fenetre
    private long window;
    private Render render = new Render();
    private WindowCloseEventManager windowCloseEventManager = new WindowCloseEventManager();

    public Window(){
        this(300, 300);
    }    
    public Window(int width, int height){
        this.width = Math.max(150, width);
        this.height = Math.max(150, height);
        // le :: permet de passé en paramètre la méthode init dans le thread, car this.init() ne fait pas passé la méthode en paramètre mais l'exécuté
        // le :: est comme un raccourci pour un lambda, sinon il faut faire Thread(() -> this.init()) 
        (new Thread(this::init)).start(); // démare le thread
    }

    @OpenGLThread
    public void init(){
        // Toutes les méthodes exécuté depuis init seront dans le thread car init est dans le thread
        // l'annotation permet juste de voir visuellement quelle méthode est dans le thread (elle ne fait absolument rien c'est décoratif)
        GLFWErrorCallback.createPrint(System.err).set();
        if (!GLFW.glfwInit()) {
            GLFW.glfwSetErrorCallback(null).free();
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        GLFW.glfwDefaultWindowHints(); // Loads GLFW's default window settings
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE); // Sets window to be visible
        GLFW.glfwWindowHint(GLFW.GLFW_DECORATED, GLFW.GLFW_TRUE); // contour de fenetre
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_FALSE); // Sets the window not resizable
        GLFW.glfwWindowHint(GLFW.GLFW_MAXIMIZED, GLFW.GLFW_FALSE); // la fenetre est la dimension la plus proche de celle demandé

        this.window = GLFW.glfwCreateWindow(this.width, this.height, "test", NULL, NULL); // Does the actual window creation
        if (this.window == MemoryUtil.NULL) {
            this.closeGLFW();
            throw new RuntimeException("Failed to create window");
        }

        // Listeners clavier souris

        // Management de la fermeture de la fenetre
        GLFW.glfwSetWindowCloseCallback(this.window, this.windowCloseEventManager);

        GLFW.glfwMakeContextCurrent(this.window); // glfwSwapInterval needs a context on the calling thread, otherwise will cause NO_CURRENT_CONTEXT error    

        GLFW.glfwSwapInterval(1); // How many draws to swap the buffer

        GLFW.glfwShowWindow(this.window); // Shows the window

        GL.createCapabilities(); // Will let lwjgl know we want to use this context as the context to draw with
        
        this.render.rendering(this, this.window);
    }

    public void exitWidow()
    {
        // On libère tous les listeners
        Callbacks.glfwFreeCallbacks(this.window);
        // On finalise la destruction de la fenêtre
        GLFW.glfwDestroyWindow(this.window);
        // On ferme les événements GLFW
        this.closeGLFW();
    }

    public void closeWindow(){
        this.windowCloseEventManager.closeWidow(this.window);
    }

    private void closeGLFW()
    {
        // Termine la gestion de la fenêtre par GLFW
        GLFW.glfwTerminate();
        // Arrête de traquer les erreurs
        GLFW.glfwSetErrorCallback(null).free();
    }

    public SceneGraph getSceneGraph(){
        return this.render.sceneGraph;
    }
}
