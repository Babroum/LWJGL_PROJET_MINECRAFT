package demo;

import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

import demo.Managers.AudioManager;
import demo.Managers.BuffersManager;
import demo.Managers.ChunksManager;
import demo.Managers.PlayersManager;
import demo.Geometry.Object3D;

/**
 * Gère tout les évènements du jeu, hors rendu
 */
public class GameLoop {

    private static Object3D oldBlockBorder;
    
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
        Vector3f look = PlayersManager.player.getCamera().getLookAt(20);
        Object3D block = null;

        if (look != null){   
            try {
                // floor appliquer seulement sur les valeurs positives
                int bx = look.x < 0 ? (int) Math.floor(look.x) : (int) look.x;
                int by = look.y < 0 ? (int) Math.floor(look.y) : (int) look.y;
                int bz = look.z < 0 ? (int) Math.floor(look.z) : (int) look.z;    
                System.out.println("look x : " + look.x + " look y : " + look.y + " look z : " + look.z);    
                block = ChunksManager.getBlock(bx, by, bz);

                // si le block regarder n'est pas le même que le précédent on créer les buffers
                if (block != null && block != GameLoop.oldBlockBorder){
                    if (GameLoop.oldBlockBorder != null){ BuffersManager.deleteVBOVAO(oldBlockBorder.ids); }
                    GameLoop.oldBlockBorder = block;
                    block.createSubChunkBorderVBOVAO();
                }

                // delete les buffers si le joueur ne regarde plus de block
                if (block == null && GameLoop.oldBlockBorder != null){
                    BuffersManager.deleteVBOVAO(oldBlockBorder.ids); 
                    GameLoop.oldBlockBorder = null;
                }

                // dessine à chaque frame si un bloc est regardé
                if (GameLoop.oldBlockBorder != null){
                    GL11.glLineWidth(4.0f); // épaisseur en pixels
                    GameLoop.oldBlockBorder.drawBlockBorder(bx, by, bz);
                    GL11.glLineWidth(1.0f); // reset après
                }
            } catch(Exception e){
                // le chunk n'existe pas
            }
        }          
        
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
