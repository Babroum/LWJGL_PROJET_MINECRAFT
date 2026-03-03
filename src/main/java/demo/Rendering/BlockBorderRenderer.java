package demo.Rendering;

import java.nio.IntBuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import demo.Managers.BuffersManager;
import demo.Managers.PlayersManager;
import demo.Managers.ShadersManager;

public class BlockBorderRenderer {

    // Un seul VAO partagé pour toutes les bordures
    private static int vao = 0;
    private static int vbo = 0;

    // Appelé UNE SEULE FOIS au démarrage
    public static void init(){
        int[] lines = {
            0, 0, 0,   1, 0, 0,
            1, 0, 0,   1, 0, 1,
            1, 0, 1,   0, 0, 1,
            0, 0, 1,   0, 0, 0,
            0, 0, 0,   0, 1, 0,
            1, 0, 0,   1, 1, 0,
            1, 0, 1,   1, 1, 1,
            0, 0, 1,   0, 1, 1,
            0, 1, 0,   1, 1, 0,
            1, 1, 0,   1, 1, 1,
            1, 1, 1,   0, 1, 1,
            0, 1, 1,   0, 1, 0
        };

        IntBuffer buffer = BufferUtils.createIntBuffer(lines.length);
        for(int i = 0; i < lines.length; i += 3){
            int packed = 0;
            packed |= ((lines[i]  ) & 0x1F) << 27;
            packed |= ((lines[i+1]) & 0xFF) << 19;
            packed |= ((lines[i+2]) & 0x1F) << 14;
            buffer.put(packed);
        }
        buffer.flip();

        int[] ids = BuffersManager.createVBOVAO(buffer);
        vbo = ids[0];
        vao = ids[1];
    }

    // Appelé à chaque frame si un bloc est regardé
    public static void draw(int worldX, int worldY, int worldZ){
        if (vao == 0) return;

        ShadersManager.useBlockBorderShader();
        ShadersManager.runProjectionBlockBorder(
            Render.calculateProjection(45.0f, (float) Window.width / Window.height, 0.1f, 5000.f)
        );
        ShadersManager.runViewBlockBorder(
            PlayersManager.player.getCamera().getViewMatrixBuffer()
        );
        ShadersManager.runBlockPosBlockBorder(worldX, worldY, worldZ);

        GL30.glBindVertexArray(vao);
        GL11.glDrawArrays(GL11.GL_LINES, 0, 24);
        GL30.glBindVertexArray(0);

        ShadersManager.useShader();
    }

    // Appelé à la fermeture du jeu
    public static void cleanup(){
        BuffersManager.deleteVBOVAO(new int[]{vbo, vao});
    }
}