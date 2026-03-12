package demo.Managers;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import demo.Player;
import demo.Rendering.BlockBorderRenderer;

import org.joml.Vector3f;

/**
 * Gère le joueur (plustard plusieurs)
 */
public class PlayersManager {
    
    public static Player player = new Player();
    public static ConcurrentHashMap<Integer, Vector3f> otherPlayers = new ConcurrentHashMap<>();

    public static void playersLoop(float delta){
        PlayersManager.player.playerLoop(delta);

        for (Map.Entry<Integer, Vector3f> entry : otherPlayers.entrySet()){
            Vector3f pos = entry.getValue();
            BlockBorderRenderer.draw((int)pos.x, (int)pos.y, (int)pos.z);
        }
    }
}
