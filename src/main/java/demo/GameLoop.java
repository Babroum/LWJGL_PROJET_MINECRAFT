package demo;

import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

import demo.Managers.AudioManager;
import demo.Managers.BuffersManager;
import demo.Managers.ChunksManager;
import demo.Managers.PlayersManager;
import demo.Events.MouseInput;
import demo.Geometry.Object3D;
import demo.Geometry.SubChunk;

/**
 * Gère tout les évènements du jeu, hors rendu
 */
public class GameLoop {

    private static Object3D oldBlockBorder;
    private static float breakCooldown = 0f;
    private static float breakDelay = 200f; // temps en millisecondes pour casser/poser un bloc
    
    /**
     * Executer avant la boucle de rendu
     */
    public static void init(){
        AudioManager.playLoop("bird");
        AudioManager.setVolume("bird", 10f);

        // positionne la camera
        PlayersManager.player.getCamera().setPosition(0, 8, 0);
    }

    /**
     * Executer pendant la boucle de rendu
     */
    public static void run(float delta){
        System.out.println(GameLoop.breakCooldown);
        if (GameLoop.breakCooldown > 0) {
            GameLoop.breakCooldown -= delta;
        }

        // récupère les coordonées de ce que regarde le joueur (raycast)
        Vector3f look = PlayersManager.player.getCamera().getLookAt(5);
        Object3D block = null;
        SubChunk sub = null;

        if (look != null){   
            try {
                int bx = (int) Math.floor(look.x);
                int by = (int) Math.floor(look.y);
                int bz = (int) Math.floor(look.z);

                // récupère le block à tel position
                block = ChunksManager.getBlock(bx, by, bz);
                // pareil pour le chunk
                sub = ChunksManager.getSubChunk(bx, by, bz);

                // si on clique gauche, on casse le block si il existe
                if (MouseInput.isLeftButtonPressed()) {
                    // Si on a un bloc et que le cooldown est prêt
                    if (block != null && GameLoop.breakCooldown <= 0f) {
                        sub.removeBlockFromSubChunk(block);
                        GameLoop.breakCooldown = GameLoop.breakDelay;
                        block = null; 
                    }
                } else {
                    GameLoop.breakCooldown = 0;
                }               

                // si le block regarder n'est pas le même que le précédent on créer les buffers
                if (block != null && block != GameLoop.oldBlockBorder){
                    if (GameLoop.oldBlockBorder != null){ BuffersManager.deleteVBOVAO(oldBlockBorder.ids); }
                    GameLoop.oldBlockBorder = block;
                    block.createSubChunkBorderVBOVAO();
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
        } else {
            // Si on ne regarde rien 
            if (GameLoop.oldBlockBorder != null) {
                BuffersManager.deleteVBOVAO(oldBlockBorder.ids);
                GameLoop.oldBlockBorder = null;
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
