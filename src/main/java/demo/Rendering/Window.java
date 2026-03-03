package demo.Rendering;

import static org.lwjgl.system.MemoryUtil.NULL;

import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWFramebufferSizeCallbackI;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryUtil;

import demo.Annotations.OpenGLThread;
import demo.Events.InputManager;
import demo.Events.WindowCloseEventManager;
import demo.Managers.AudioManager;
import demo.Managers.ShadersManager;

/**
 * La fenêtre géré par GLFW
 */
public class Window {
    
    /**
     * Largeur de la fenêtre.
     */
    public static int width;
    /**
     * Hauteur de la fenêtre.
     */
    public static int height;

    // Référence de la fenêtre
    private static long window;
    private static WindowCloseEventManager windowCloseEventManager = new WindowCloseEventManager();
    private static Runnable onReady;

    /**
     * Créer une fenêtre de taille 300 x 300
     */
    public Window(){
        this(300, 300);
    }    
    /**
     * Créer une fenêtre de taille données
     * @param width int
     * @param height int
     */
    public Window(int width, int height){
        Window.width = Math.max(150, width);
        Window.height = Math.max(150, height); 
        new InputManager();  
    }

    public static void startWindow(Runnable onReady){   
        // onReady permet juste de verifie que le contexte openGL est créer avant de lancer le dessin      
        Window.onReady = onReady;
        // le :: permet de passer  en paramètre la méthode init dans le thread, car this.init() ne fait pas passer  la méthode en paramètre mais l'exécuté
        // le :: est comme un raccourci pour un lambda, sinon il faut faire Thread(() -> this.init()) 
        (new Thread(Window::init)).start(); // démarre le thread
    }

    @OpenGLThread
    public static void init(){
        // Toutes les méthodes exécuté depuis init seront dans le thread car init est dans le thread
        // l'annotation permet juste de voir visuellement quelle méthode est dans le thread (elle ne fait absolument rien c'est décoratif)
        GLFWErrorCallback.createPrint(System.err).set();
        if (!GLFW.glfwInit()) {
            GLFW.glfwSetErrorCallback(null).free();
            throw new IllegalStateException("Pas possible d'initialiser GLFW");
        }

        GLFW.glfwDefaultWindowHints(); // Charge les paramètres par défaut de fenêtre de GLFW
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE); // Cache la fenêtre
        GLFW.glfwWindowHint(GLFW.GLFW_DECORATED, GLFW.GLFW_TRUE); // Contour de fenetre
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE); // Fenêtre redimensionable?
        GLFW.glfwWindowHint(GLFW.GLFW_MAXIMIZED, GLFW.GLFW_FALSE); // La fenêtre essayera d'atteindre la taille demandée

        Window.window = GLFW.glfwCreateWindow(Window.width, Window.height, "Wankraft", NULL, NULL); // Créer la fenêtre
        if (Window.window == MemoryUtil.NULL) {
            Window.closeGLFW();
            throw new RuntimeException("Impossible de créer la fenêtre");
        }
    
        // Créer les Listeners clavier/souris
        InputManager.create(window);

        // Management de la fermeture de la fenêtre
        GLFW.glfwSetWindowCloseCallback(Window.window, Window.windowCloseEventManager);

        GLFW.glfwMakeContextCurrent(Window.window); // glfwSwapInterval needs a context on the calling thread, otherwise will cause NO_CURRENT_CONTEXT error    

        GLFW.glfwSwapInterval(1); // Le nombre de dessin avant d'échanger les buffers (Vsync)

        GL.createCapabilities(); // Will let lwjgl know we want to use this context as the context to draw with

        GLFW.glfwShowWindow(Window.window); // Affiche la fenêtre

        GL.createCapabilities(); // Will let lwjgl know we want to use this context as the context to draw with

        AudioManager.init();

        Render.setupBeforeRendering();

        if(Window.onReady != null) Window.onReady.run(); // attend que le thread est fini de créer opengl avant de lancer le code
        
        //callback pour redimension de fenetre en gros quand fenetre est redimensionnee executer ce code
        GLFW.glfwSetFramebufferSizeCallback(Window.window, new GLFWFramebufferSizeCallbackI(){
            @Override
            public void invoke(long window, int width, int height) {
                Window.height = height; // pour modifier la hauteur de la fenêtre dans la classe
                Window.width =  width; // pour modifier la largeur de la fenêtre dans la classe
                GL11.glViewport(0, 0, width, height); // Redimensionne le glViewPort
            }
        });  

        Render.rendering(Window.window);
    }

    public static void exitWindow()
    {
        // On libère tous les listeners
        Callbacks.glfwFreeCallbacks(Window.window);
        // On finalise la destruction de la fenêtre
        GLFW.glfwDestroyWindow(Window.window);
        // On ferme les événements GLFW
        Window.closeGLFW();

        ShadersManager.closeProgramId();
        ShadersManager.closeProgramTextId();
        ShadersManager.closeProgramBlockBorderId();
        
        AudioManager.cleanup();
        BlockBorderRenderer.cleanup();
        
        Window.windowCloseEventManager.closeWidow(Window.window);

        System.exit(0);
    }

    private static void closeGLFW()
    {
        // Termine la gestion de la fenêtre par GLFW
        GLFW.glfwTerminate();
        // Arrête de traquer les erreurs
        GLFW.glfwSetErrorCallback(null).free();
    }

    /**
     * Retourne la scène
     * @return SceneGraph
     */
    public static SceneGraph getSceneGraph(){
        return Render.sceneGraph;
    }

    /**
     * Retourne la ref de la fenêtre
     * @return Long
     */
    public static long getWindow(){
        return Window.window;
    }
}
