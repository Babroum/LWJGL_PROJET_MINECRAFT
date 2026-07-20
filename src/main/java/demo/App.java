package demo;

import demo.Managers.BenchmarkManager;
import demo.Others.ChunkSerializer;
import demo.Rendering.SceneGraph;
import demo.Rendering.Window;

/**
 * Point d'entrée du programme
 */
public class App 
{      
    /** Constructeur implicite vide */
    public App(){}
    /*
    mvn exec:exec IMPORTANT car sinon vscode ne prend pas en compte les arguments java en lançant par le bouton classique
    donc pas les optimisations grâce au ZGC

    north = 0
    south = 1
    west = 2
    east = 3
    up = 4
    down = 5
    all = 6
     */
    /**
     * Point d'entrée du programme, crée la fenêtre puis démarre le rendu
     * @param args String[], arguments de la ligne de commande, non utilisés
     * @throws Exception si l'initialisation de la fenêtre échoue
     */
    public static void main( String[] args ) throws Exception
    {
        // création de la fenêtre 
        new Window(1200,800);
        // donne le draw en paramètre
        // l'exécutera après l'initialisation d'opengl
        Window.startWindow(() -> draw(Window.getSceneGraph()));        
    }

    /**
     * Exécuté une fois le contexte OpenGL prêt : initialise la sauvegarde et le benchmark
     * @param scene SceneGraph, la scène de la fenêtre (non utilisée directement ici)
     */
    public static void draw(SceneGraph scene){
        ChunkSerializer.init(); // créer le dossier saves/world/ si n'existe pas
        BenchmarkManager.getInstance().start();
    }  
}