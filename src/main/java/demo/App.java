package demo;

import demo.Enum.BlocksType;
import demo.Geometry.Chunk;
import demo.Rendering.SceneGraph;
import demo.Rendering.Window;

public class App 
{   
    
    public static void main( String[] args )
    {
        // création de la fenêtre 
        new Window(1700,860);
        
        // donne le draw en paramètre
        // l'exécutera après l'initialisation d'opengl
        Window.startWindow(() -> draw(Window.getSceneGraph()));        
    }

    public static void draw(SceneGraph scene){
        Chunk chunk = new Chunk();
        Chunk chunk2 = new Chunk();
        
        chunk2.setChunkPosition(1, 0, 0);

        // A faire avant d'ajouter à la scène sinon c'est pas pris en compte
        chunk.addFloor(0, 1, BlocksType.GRASS);        
        chunk2.addFloor(1, 1, BlocksType.GRASS);
        
        scene.root.addChild(chunk);
        scene.root.addChild(chunk2);
    }  
    

    /*
    export JAVA_HOME=/home/linuxbrew/.linuxbrew/opt/openjdk@21
    mvn clean                                                                                   
    mvn package
    mvn exec:java
    */
}
