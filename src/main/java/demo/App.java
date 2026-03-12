package demo;

import demo.Managers.BenchmarkManager;
import demo.Managers.ChunksManager;
import demo.Rendering.SceneGraph;
import demo.Rendering.Window;

public class App 
{   
    public static void main( String[] args ) throws Exception
    {
        // création de la fenêtre 
        new Window(1200,800);
        // donne le draw en paramètre
        // l'exécutera après l'initialisation d'opengl
        Window.startWindow(() -> draw(Window.getSceneGraph()));        
    }

    public static void draw(SceneGraph scene){
        BenchmarkManager.getInstance().start(); // ← démarre ici

        // chaque thread worker aura sa propre instance isolée
        // FastNoiseLite n'est pas thread-safe
        // Un ThreadLocal c'est une variable qui a une copie différente par thread. Chaque thread lit et écrit sa propre version, ils ne se voient jamais.
        ThreadLocal<WorldGenerator> tlGenerator = ThreadLocal.withInitial(
            () -> new WorldGenerator(4949849L) // seed
        );
        // thread de génération
        Thread generationThread = new Thread(() -> {
            ChunksManager.createChunks(-24, 24, -24, 24);  // 2304 chunks 
            //ChunksManager.createChunks(-16, 16, -16, 16); // 1024 chunks
            //ChunksManager.createChunks(-10, 10, -10, 10); // 400 chunks
            //ChunksManager.createChunks(-2, 2, -2, 2); // 16 chunks
            ChunksManager.generateChunks(tlGenerator);
            ChunksManager.finalizeChunks();
        }); 
        
        // daemon pour que le thread se ferme à la fermeture de la jvm
        generationThread.setDaemon(true);
        generationThread.start();
    }  

    /*
    export JAVA_HOME=/home/linuxbrew/.linuxbrew/opt/openjdk@21
    mvn clean                                                                                   
    mvn package
    mvn exec:java
    */
}
