package demo;

import java.util.concurrent.ConcurrentLinkedQueue;

import demo.Enum.BlocksType;
import demo.Geometry.Chunk;
import demo.Geometry.SubChunk;
import demo.Managers.ChunksManager;
import demo.Rendering.Render;
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
        // ConcurrentLinkedQueue pour passer les chunks prêts au thread OpenGL
        ConcurrentLinkedQueue<Chunk> readyChunks = new ConcurrentLinkedQueue<>();
        ConcurrentLinkedQueue<SubChunk> dirtyChunks = new ConcurrentLinkedQueue<>();

        // thread de génération
        Thread generationThread = new Thread(() -> {

            // créer tous les chunks et placer les blocs
            for (int i = -5; i < 5; i++){
                for (int j = -5; j < 5; j++){
                    Chunk chunk = ChunksManager.createChunk(j, i);
                    chunk.addFloorToChunk(0, 1, BlocksType.GRASS);
                }
            }

            for (int i = -5; i < 5; i++){
                for (int j = -5; j < 5; j++){
                    Chunk chunk = ChunksManager.getChunk(j, i);
                    for(SubChunk sub : chunk.subChunks){
                        sub.fillSubChunkBitSet();
                    }
                }
            }

            // finaliser APRÈS que tous les voisins existent
            for (int i = -5; i < 5; i++){
                for (int j = -5; j < 5; j++){
                    Chunk chunk = ChunksManager.getChunk(j, i);
                    ChunksManager.finalizeChunk(chunk);
                    readyChunks.add(chunk);
                }
            }

            Chunk chunk = ChunksManager.getChunk(0, 0);
            SubChunk sub = chunk.getSubChunk(1);
            sub.addFloorToSubChunk(0, 1, BlocksType.GRASS);
        });     
        
        // daemon pour que le thread se ferme à la fermeture de la jvm
        generationThread.setDaemon(true);
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
