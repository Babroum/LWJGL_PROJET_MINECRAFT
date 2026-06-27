package demo;

import demo.Managers.BenchmarkManager;
import demo.Rendering.SceneGraph;
import demo.Rendering.Window;

public class App 
{   
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
    public static void main( String[] args ) throws Exception
    {
        // création de la fenêtre 
        new Window(1200,800);
        // donne le draw en paramètre
        // l'exécutera après l'initialisation d'opengl
        Window.startWindow(() -> draw(Window.getSceneGraph()));        
    }

    public static void draw(SceneGraph scene){
        BenchmarkManager.getInstance().start();
    }  
}
