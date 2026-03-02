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
            Chunk chunk = ChunksManager.createChunk(0, 0);
            Chunk chunk2 = ChunksManager.createChunk(-1, 0);
            Chunk chunk3 = ChunksManager.createChunk(-1, -1);
            Chunk chunk4 = ChunksManager.createChunk(0, -1);

            // sol pour se repérer
            chunk.addFloorToChunk(0, 1, BlocksType.GRASS);       
            chunk2.addFloorToChunk(0, 1, BlocksType.GRASS);    
            chunk3.addFloorToChunk(0, 1, BlocksType.GRASS);   
            chunk4.addFloorToChunk(0, 1, BlocksType.GRASS);

            // Tour de 3x3 au centre des 4 chunks (autour de x=0, z=0)
            for (int y = 0; y < 5; y++){
                chunk.addBlockToChunk(BlocksType.GRASS, 0, y, 0);
                chunk2.addBlockToChunk(BlocksType.GRASS, 15, y, 0);
                chunk3.addBlockToChunk(BlocksType.GRASS, 15, y, 15);
                chunk4.addBlockToChunk(BlocksType.GRASS, 0, y, 15);
            }

            ChunksManager.rebuildAllCHunks();
            readyChunks.add(chunk);            
            readyChunks.add(chunk2);
            readyChunks.add(chunk3);
            readyChunks.add(chunk4);
        });
        /* EXEMPLE UTILISATION CASSER AJOUTER DANS INPUT MANAGER (pas opti de créer un thread a chaque click, il faudra utiliser un pool de thread)
        if (MouseInput.isLeftClick()) { // casser un bloc
            Chunk chunk = getChunkAtCursor(); // existe pas
            int[] pos = getBlockAtCursor(); // existe pas
            new Thread(() -> {
                chunk.removeBlock(pos[0], pos[1], pos[2]);
                chunk.rebuildMesh();
                Render.dirtyChunks.add(chunk);
            }).start();
        }

        if (MouseInput.isRightClick()) { // placer un bloc
            Chunk chunk = getChunkAtCursor(); // existe pas
            int[] pos = getBlockAtCursor(); // existe pas
            new Thread(() -> {
                chunk.addBlock(BlocksType.GRASS, pos[0], pos[1], pos[2]);
                chunk.rebuildMesh();
                Render.dirtyChunks.add(chunk);
            }).start();
        }
        */

        
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
