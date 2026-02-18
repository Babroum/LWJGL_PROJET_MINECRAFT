package demo.Rendering;

import static org.lwjgl.system.MemoryUtil.NULL;

import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWFramebufferSizeCallbackI;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.system.MemoryUtil;

import demo.ShaderManager;
import demo.Annotations.OpenGLThread;
import demo.Events.InputManager;
import demo.Events.WindowCloseEventManager;

/**
 * La fenêtre géré par GLFW
 */
public class Window {
    
    /**
     * Largeur de la fenêtre.
     */
    public int width;
    /**
     * Hauteur de la fenêtre.
     */
    public int height;

    // Référence de la fenêtre
    private long window;
    private Render render = new Render();
    private WindowCloseEventManager windowCloseEventManager = new WindowCloseEventManager();
    private TextRender textRender;  
    private GLCapabilities openGL;
    private Runnable onReady;

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
        this.width = Math.max(150, width);
        this.height = Math.max(150, height); 
        new InputManager();  
    }

    public void startWindow(Runnable onReady){   
        // onReady permet juste de verifie que le contexte openGL est créer avant de lancer le dessin      
        this.onReady = onReady;
        // le :: permet de passer  en paramètre la méthode init dans le thread, car this.init() ne fait pas passer  la méthode en paramètre mais l'exécuté
        // le :: est comme un raccourci pour un lambda, sinon il faut faire Thread(() -> this.init()) 
        (new Thread(this::init)).start(); // démarre le thread
    }

    @OpenGLThread
    public void init(){
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

        this.window = GLFW.glfwCreateWindow(this.width, this.height, "Wankraft", NULL, NULL); // Créer la fenêtre
        if (this.window == MemoryUtil.NULL) {
            this.closeGLFW();
            throw new RuntimeException("Impossible de créer la fenêtre");
        }
    
        // Creer Listeners clavier/souris
        InputManager.create(window, this);

        // Management de la fermeture de la fenêtre
        GLFW.glfwSetWindowCloseCallback(this.window, this.windowCloseEventManager);

        GLFW.glfwMakeContextCurrent(this.window); // glfwSwapInterval needs a context on the calling thread, otherwise will cause NO_CURRENT_CONTEXT error    

        GLFW.glfwSwapInterval(1); // Le nombre de dessin avant d'échanger les buffers (Vsync)

        GL.createCapabilities(); // Will let lwjgl know we want to use this context as the context to draw with

        GLFW.glfwShowWindow(this.window); // Affiche la fenêtre

        this.openGL = GL.createCapabilities(); // Will let lwjgl know we want to use this context as the context to draw with

        if(this.onReady != null) this.onReady.run(); // attend que le thread est fini de créer opengl avant de lancer le code hors 
        
        //callback pour redimension de fenetre en gros quand fenetre est redimensionnee executer ce code
        GLFW.glfwSetFramebufferSizeCallback(this.window, new GLFWFramebufferSizeCallbackI(){
            @Override
            public void invoke(long window, int width, int height) {
                Window.this.height = height; // pour modifier la hauteur de la fenêtre dans la classe
                Window.this.width =  width; // pour modifier la largeur de la fenêtre dans la classe
                GL11.glViewport(0, 0, width, height); // Redimensionne le glViewPort
            }
        });

        this.textRender = new TextRender("res/Textures/Font.png", 20, 20, 19,
                                        " !\"#$%&\'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~");   

        this.render.rendering(this, this.window);

    }

    public TextRender getTextRender(){
        return this.textRender;
    }
    public GLCapabilities getOpenGl(){
        return this.openGL;
    }

    public void exitWindow()
    {
        // On libère tous les listeners
        Callbacks.glfwFreeCallbacks(this.window);
        // On finalise la destruction de la fenêtre
        GLFW.glfwDestroyWindow(this.window);
        // On ferme les événements GLFW
        this.closeGLFW();
    }

    public void closeWindow(){
        ShaderManager.closeProgramId();
        this.windowCloseEventManager.closeWidow(this.window);
    }

    private void closeGLFW()
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
    public SceneGraph getSceneGraph(){
        return this.render.sceneGraph;
    }

    /**
     * Retourne la ref de la fenêtre
     * @return Long
     */
    public long getWindow(){
        return this.window;
    }
}
