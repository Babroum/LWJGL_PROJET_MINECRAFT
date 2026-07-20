package demo.Managers;

import demo.Game.Player;

/**
 * Gère le joueur (plustard plusieurs)
 */
public class PlayersManager {

    /** Constructeur implicite vide */
    public PlayersManager(){}
    
    /**
     * Instance du joueur (unique pour le moment).
     */
    public static Player player = new Player();

    /**
     * Met à jour la logique du joueur, à appeler à chaque tick.
     * @param delta temps écoulé depuis le dernier tick (en secondes)
     */
    public static void playersLoop(float delta){
        PlayersManager.player.playerLoopLogic(delta);
    }
}