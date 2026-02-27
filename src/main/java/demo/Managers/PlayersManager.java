package demo.Managers;

import demo.Player;

/**
 * Gère le joueur (plustard plusieurs)
 */
public class PlayersManager {
    
    public static Player player = new Player();

    public static void updateMovement(float delta){
        PlayersManager.player.updateMovement(delta / 1000f); // en seconde
    }
}
