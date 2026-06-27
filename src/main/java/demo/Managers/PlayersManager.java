package demo.Managers;

import demo.Game.Player;

/**
 * Gère le joueur (plustard plusieurs)
 */
public class PlayersManager {
    
    public static Player player = new Player();

    public static void playersLoop(float delta){
        PlayersManager.player.playerLoopLogic(delta);
    }
}
