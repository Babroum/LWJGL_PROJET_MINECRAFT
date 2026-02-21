package demo;

import java.util.concurrent.ConcurrentLinkedQueue;

import demo.Enum.BlocksType;
import demo.Geometry.Chunk;
import demo.Rendering.Render;
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
        // ConcurrentLinkedQueue pour passer les chunks prêts au thread OpenGL
        ConcurrentLinkedQueue<Chunk> readyChunks = new ConcurrentLinkedQueue<>();
        ConcurrentLinkedQueue<Chunk> dirtyChunks = new ConcurrentLinkedQueue<>();

        // thread de génération
        Thread generationThread = new Thread(() -> {
            Chunk chunk = new Chunk();
            chunk.addFloor(0, 1, BlocksType.GRASS);
            chunk.generateChunkMesh();
            readyChunks.add(chunk);

            Chunk chunk2 = new Chunk();
            chunk2.setChunkPosition(1, 0, 0);
            chunk2.addFloor(1, 1, BlocksType.GRASS);
            chunk2.generateChunkMesh();
            readyChunks.add(chunk2);
        });
        
        generationThread.start();
        Render.setChunkQueue(readyChunks);
        Render.setDirtyChunkQueue(dirtyChunks);
    }  

    /*
    export JAVA_HOME=/home/linuxbrew/.linuxbrew/opt/openjdk@21
    mvn clean                                                                                   
    mvn package
    mvn exec:java
    */
}
