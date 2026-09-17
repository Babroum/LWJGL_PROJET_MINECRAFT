package demo.Game;

import java.util.concurrent.atomic.AtomicInteger;

import demo.Events.KeyboardInput;
import demo.Managers.ChunksManager;
import demo.Managers.LightManager;
import demo.Managers.LiquidManager;
import demo.Managers.PlayersManager;
import demo.Managers.WorldManager;
import demo.Others.ChunkSerializer;
import demo.Rendering.Render;

public class GameSession {

    private static volatile boolean running = false;
    private static final AtomicInteger epoch = new AtomicInteger(0);

    private GameSession(){}

    /**
     * Retourne l'epoch (génération) de partie actuelle.
     * Toute tâche asynchrone doit capturer cette valeur à sa soumission
     * et la comparer avant d'écrire un résultat dans un état partagé.
     */
    public static int currentEpoch(){
        return epoch.get();
    }

    public static void start(){
        if (running) return;

        epoch.incrementAndGet();

        KeyboardInput.isMouseLock = true;
        KeyboardInput.isCursorHidden = true;

        Render.sceneGraph.root.removeAllChildren();
        ChunkSerializer.init();
        
        PlayersManager.player.restartActionThread();

        GameLoop.init();
        GameLoop.startGameThread();

        running = true;
    }

    public static void stop(){
        if (!running) return;

        epoch.incrementAndGet();

        KeyboardInput.isMouseLock = false;
        KeyboardInput.isCursorHidden = false;

        GameLoop.stopGameThread();
        PlayersManager.player.stopActionThread();

        for (var chunk : ChunksManager.getAllChunks()) {
            ChunkSerializer.save(chunk);
        }

        ChunksManager.clearAllChunks();
        WorldManager.reset();
        Render.sceneGraph.root.removeAllChildren();

        Render.chunkQueue.clear();
        Render.dirtySubChunks.clear();
        Render.chunksToUnload.clear();
        Render.subChunkMeshQueue.clear();

        LightManager.clearAll();
        LiquidManager.clearAll();

        running = false;
    }

    public static boolean isRunning(){
        return GameSession.running;
    }
}