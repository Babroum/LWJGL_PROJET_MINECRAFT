package demo;

import org.joml.Vector3f;

import demo.Managers.AudioManager;
import demo.Managers.PlayersManager;

/**
 * Gère tout les évènements du jeu, hors rendu
 */
public class GameLoop {
    
    /**
     * Executer avant la boucle de rendu
     */
    public static void init(){
        AudioManager.playLoop("bird");
        AudioManager.setVolume("bird", 10f);

        // positionne la camera
        PlayersManager.player.setPosition(new Vector3f(0, 8, 0));
    }

    /**
     * Executer pendant la boucle de rendu
     */
    public static void run(float delta){
        PlayersManager.playersLoop(delta);
    }

    /**
     * Executer après la boucle de rendu
     */
    public static void quit(){
        
    }
}
