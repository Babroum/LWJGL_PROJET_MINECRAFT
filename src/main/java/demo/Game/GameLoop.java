package demo.Game;

import org.joml.Vector3f;

import demo.Managers.InputManager;
import demo.Managers.LightManager;
import demo.Managers.LiquidManager;
import demo.Managers.PlayersManager;
import demo.Managers.WorldManager;

/**
 * Gère tout les évènements du jeu, hors rendu
 */
public class GameLoop {

    /** Indique si la boucle de jeu tourne actuellement. */
    private static volatile boolean running = false;
    /** Thread dédié à la boucle de jeu. */
    private static Thread gameThread;

    /** Nombre de ticks par seconde de la boucle de jeu. */
    private static final int  TPS         = 20;
    /** Timestamp (nanosecondes) du dernier tick exécuté, utilisé pour l'interpolation au rendu. */
    public static volatile long lastTickTime = 0L;
    /** Durée d'un tick en nanosecondes. */
    public static final long NS_PER_TICK = (1_000_000_000L / TPS); // en nanosecondes

    /** Constructeur implicite vide */
    public GameLoop(){}

    /**
     * Démarre le thread de la gameloop
     * La gameloop est dirigé par un nombre de tick par seconde
     */
    public static void startGameThread() {
        running = true;
        gameThread = new Thread(() -> {
            long lastTime = System.nanoTime();
            double lag = 0.0;

            while (running) {
                long now   = System.nanoTime();
                long delta = now - lastTime;
                lastTime   = now;
                lag       += delta;

                int maxTicksPerFrame = 5;
                while (lag >= NS_PER_TICK && maxTicksPerFrame-- > 0) {
                    GameLoop.lastTickTime = System.nanoTime();
                    GameLoop.run(NS_PER_TICK / 1_000_000f); // en ms pour la physique
                    lag -= NS_PER_TICK;
                }

                long sleepNs = (long)(NS_PER_TICK - lag);
                if (sleepNs > 1_000_000L) { // seulement si > 1ms
                    try { Thread.sleep(sleepNs / 1_000_000L); }
                    catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
                }
            }
        }, "GameLoop Thread");
        gameThread.setDaemon(true);
        gameThread.start();
    }

    /**
     * Arrête le thread de la gameloop et attend sa terminaison (max 1 seconde)
     */
    public static void stopGameThread() {
        running = false;
        if (gameThread != null) {
            try { gameThread.join(1000); } catch (InterruptedException ignored) {}
        }
    }

    /**
     * Executer avant la boucle de rendu
     */
    public static void init(){
        // positionne le joueur
        PlayersManager.player.setPosition(5, 80, 5);
        // sync les snapshots pour éviter un saut au premier tick
        PlayersManager.player.prevX = 5;  PlayersManager.player.curX = 5;
        PlayersManager.player.prevY = 80; PlayersManager.player.curY = 80;
        PlayersManager.player.prevZ = 5;  PlayersManager.player.curZ = 5;
    }

    /**
     * Executer pendant la boucle de jeu (20 TPS)
     * @param delta float, temps écoulé depuis le dernier tick, en ms
     */
    public static void run(float delta){
        LightManager.processLightBFS();
        LiquidManager.processLiquidBFS();
        
        InputManager.runLogic(delta);
        PlayersManager.playersLoop(delta);

        Vector3f pos = PlayersManager.player.getPosition();
        WorldManager.tick(pos.x, pos.z);
    }

    /**
     * Executer à la fermeture
     */
    public static void quit(){
        
    }
}