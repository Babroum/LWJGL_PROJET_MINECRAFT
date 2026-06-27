package demo;

import demo.Geometry.WorldGenerator;
import demo.Managers.BenchmarkManager;
import demo.Managers.ChunksManager;
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

        // chaque thread worker aura sa propre instance isolée
        // FastNoiseLite n'est pas thread-safe
        // Un ThreadLocal c'est une variable qui a une copie différente par thread. Chaque thread lit et écrit sa propre version, ils ne se voient jamais.
        // C'est juste un type, il ne crée aucun thread.
        ThreadLocal<WorldGenerator> tlGenerator = ThreadLocal.withInitial(
            () -> new WorldGenerator(4949849L) // seed
        );
        // thread de génération
        Thread generationThread = new Thread(() -> {
            //ChunksManager.createChunks(-100, 100, -100, 100); // 40 000 chunks, il faut enlevé la limite de ram max dans le pom sinon crash
            //ChunksManager.createChunks(-24, 24, -24, 24);  // 2304 chunks 
            //ChunksManager.createChunks(-16, 16, -16, 16); // 1024 chunks
            ChunksManager.createChunks(-10, 10, -10, 10); // 400 chunks
            //ChunksManager.createChunks(-1, 1, -1, 1); // 16 chunks
            //ChunksManager.createChunks(0, 2, 0, 2); // 4 chunks
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
