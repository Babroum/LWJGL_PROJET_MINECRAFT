package demo.Exemple;

import java.util.Objects;

import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.Callbacks;
import org.lwjgl.system.MemoryUtil;

public class WindowEx {
    public final int width;
    public final int height;
    public final String title;

    private long window;
    private final WindowCloseEventManager windowCloseEventManager = new WindowCloseEventManager();
    private final RenderEx render = new RenderEx();

    public WindowEx(int width, int height, String title)
    {
        this.title = Objects.requireNonNull(title, "title must not be null");
        // Évitons les fenêtres trop petite
        this.width  = Math.max(128, width);
        this.height = Math.max(128, height);

        // Création et lancement du Thread OpenGL
        (new Thread(this::startOpenGLThread)).start();
    }
    
    @ThreadOpenGLEx
    private void startOpenGLThread()
    {
        // Capture les erreurs LWJGL et les affichent sur la console
        GLFWErrorCallback.createPrint(System.err).set();   

        if (!GLFW.glfwInit())
        {
            // On désinscrit la gestion d’erreurs puisque nous ne pouvons pas continuer
            GLFW.glfwSetErrorCallback(null).free();
            System.err.println("GLFW initialisation failed");
            return;
        }

        // Configure GLFW
        GLFW.glfwDefaultWindowHints();
        // On empêche, pour le moment, que la fenêtre s’affiche afin d’avoir le temps de la configurée
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);

        // La fenêtre aura un contour
        GLFW.glfwWindowHint(GLFW.GLFW_DECORATED, GLFW.GLFW_TRUE);
        // La fenêtre ne pourra pas être redimensionnée
        // Un redimensionnement obligerait à écouter le changement de taille et à y réagir.
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_FALSE);
        // On veut que la fenêtre fasse au plus proche possible la dimension demandée
        GLFW.glfwWindowHint(GLFW.GLFW_MAXIMIZED, GLFW.GLFW_FALSE);

        // Création de la fenêtre
        this.window = GLFW.glfwCreateWindow(this.width, this.height, this.title, MemoryUtil.NULL, MemoryUtil.NULL);

        if (this.window == MemoryUtil.NULL)
        {
            // La fenêtre n’a pas pu être réservée en mémoire, on ne peut pas continuer
            this.closeGLFW();
            System.err.println("GLFW can’t create window");
            return;
        }

        // Management de la fermeture de la fenêtre pour libérer proprement la mémoire
        GLFW.glfwSetWindowCloseCallback(this.window, this.windowCloseEventManager);

        // Associe le contexte Open GL à la fenêtre
        GLFW.glfwMakeContextCurrent(this.window);
        // Activation du mode v-sync
        GLFW.glfwSwapInterval(1);

        // Maintenant que tout est configuré la fenêtre peut se montrer
        GLFW.glfwShowWindow(this.window);

        // Cet appel est crucial pour que la management d’Open GL avec GLFW se passe bien
        GL.createCapabilities();

        //Lancement du rendu OpenGL
        this.render.rendering(this, this.window);
    }

    public void closeWindow()
    {
        this.windowCloseEventManager.closeWindow(this.window);
    }

    void exitWidow()
    {
        // On libère tous les listeners
        Callbacks.glfwFreeCallbacks(this.window);
        // On finalise la destruction de la fenêtre
        GLFW.glfwDestroyWindow(this.window);
        // On ferme les événements GLFW
        this.closeGLFW();
    }

    private void closeGLFW()
    {
        // Termine la gestion de la fenêtre par GLFW
        GLFW.glfwTerminate();
        // Arrête de traquer les erreurs
        GLFW.glfwSetErrorCallback(null).free();
    }

    public SceneGraphEx getSceneGraph()
    {
        return this.render.sceneGraph;
    }
}
