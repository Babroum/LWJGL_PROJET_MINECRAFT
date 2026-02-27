package demo;

import java.util.Vector;

import org.joml.Vector3f;

import demo.Managers.AudioManager;
import demo.Managers.ChunksManager;
import demo.Managers.PlayersManager;
import demo.Geometry.Object3D;

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
        PlayersManager.player.getCamera().setPosition(5, 5, 5);
    }

    /**
     * Executer pendant la boucle de rendu
     */
    public static void run(float delta){
        Vector3f look = PlayersManager.player.getCamera().getLookAt(10);
        Object3D block = null;
        /*
        if (look != null){
            block = ChunksManager.getBlock((int)look.x, (int)look.y, (int)look.z);
            System.out.println("[lookat] : x : " + look.x + " y : " + look.y + " z : " + look.z);
            System.out.println("--------------------------------------");
        }       
        if (block != null){
            System.out.println("[lookat] block : x : " + block.x + " y : " + block.y + " z : " + block.z);
            System.out.println("--------------------------------------");
        }*/
        
        PlayersManager.updateMovement(delta);
    }

    /**
     * Executer après la boucle de rendu
     */
    public static void quit(){
        // termine l'enregistrement de donnée pour la frame
        AudioManager.cleanupTempSources();
    }
}
