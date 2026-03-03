package demo.Geometry;

import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import demo.Annotations.OpenGLThread;
import demo.Enum.BlocksType;
import demo.Managers.BuffersManager;
import demo.Managers.PlayersManager;
import demo.Managers.ShadersManager;
import demo.Meshing.Mesh;
import demo.Rendering.Render;
import demo.Rendering.Window;

public class Object3D extends Node3D{

    public Mesh mesh = new Mesh();
    public BlocksType blockType;
    public int[] ids;

    /**
     * Dessine les bordures d'un block avec des lignes
     */
    @OpenGLThread
    public void createSubChunkBorderVBOVAO(){     
        this.ids = new int[2]; 
        // 4 sommets du contour du chunk en bas
        int[] lines = {
             // bas
            0, 0, 0,   1, 0, 0,
            1, 0, 0,   1, 0, 1,
            1, 0, 1,   0, 0, 1,
            0, 0, 1,   0, 0, 0,
            // verticales
            0, 0, 0,   0, 1, 0,
            1, 0, 0,   1, 1, 0,
            1, 0, 1,   1, 1, 1,
            0, 0, 1,   0, 1, 1,
            // haut
            0, 1, 0,  1, 1, 0,
            1, 1, 0,  1, 1, 1,
            1, 1, 1,  0, 1, 1,
            0, 1, 1,  0, 1, 0
        };
        
        IntBuffer linesBuffer = BufferUtils.createIntBuffer(lines.length);
        
        for(int i = 0; i < lines.length; i+=3){
            int packed = 0;
            packed |= ((lines[i]  ) & 0x1F) << 27;
            packed |= ((lines[i+1]) & 0xFF) << 19;
            packed |= ((lines[i+2]) & 0x1F) << 14;
            linesBuffer.put(packed);
        }
        linesBuffer.flip();

        if (this.ids != null && this.ids[0] != 0){
            BuffersManager.deleteVBOVAO(this.ids);
        }

        int[] blockBorderIds = BuffersManager.createVBOVAO(linesBuffer);
        this.ids[0] = blockBorderIds[0];
        this.ids[1] = blockBorderIds[1];
    }

    @OpenGLThread
    public void drawBlockBorder(int worldX, int worldY, int worldZ){
        ShadersManager.useBlockBorderShader(); // utilise le shader pour les bordures
        // donne les matrices de camera et perspective au shader
        ShadersManager.runProjectionBlockBorder(Render.calculateProjection(45.0f, (float) Window.width / Window.height, 0.1f, 5000.f)); // donne la perspective de l'ecran
        ShadersManager.runViewBlockBorder(PlayersManager.player.getCamera().getViewMatrixBuffer()); // donne la vue de la camera
        ShadersManager.runBlockPosBlockBorder(worldX, worldY, worldZ);
        
        // dessine les lignes
        GL30.glBindVertexArray(ids[1]);
        GL11.glDrawArrays(GL11.GL_LINES, 0, 24);
        GL30.glBindVertexArray(0);
        ShadersManager.useShader(); // remet l'ancien shader
    }
}
