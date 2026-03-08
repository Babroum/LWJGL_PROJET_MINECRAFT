package demo;

import demo.Managers.AudioManager;
import demo.Managers.InputManager;
import demo.Managers.PlayersManager;
import demo.Rendering.Render;

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
        // Créer les Listeners clavier/souris
        InputManager.create();
        // positionne la camera
        PlayersManager.player.setPosition(0, 8, 0);
    }

    /**
     * Executer pendant la boucle de rendu
     */
    public static void run(float delta){
        // Modifie le déplacement de la caméra 
        InputManager.run(Render.sceneGraph, delta);
        PlayersManager.playersLoop(delta);
    }

    /**
     * Executer après la boucle de rendu
     */
    public static void quit(){
        
    }
}
