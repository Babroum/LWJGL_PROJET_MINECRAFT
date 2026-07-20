package demo.Rendering;

import java.nio.IntBuffer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;

import demo.Managers.BuffersManager;
import demo.Managers.PlayersManager;
import demo.Managers.ShadersManager;

/**
 * Dessine le contour du bloc visé par le joueur
 */
public class BlockBorderRenderer {

    /** Id du VAO partagé pour toutes les bordures. */
    private static int vao = 0;
    /** Id du VBO partagé pour toutes les bordures. */
    private static int vbo = 0;

    /** Constructeur implicite vide */
    public BlockBorderRenderer(){}

    /**
     * Initialise le mesh du contour de bloc
     */
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

        IntBuffer buffer = MemoryUtil.memAllocInt(lines.length);
        for(int i = 0; i < lines.length; i += 3){
            int packed = 0;
            packed |= (lines[i]  ) << 27;
            packed |= (lines[i+1]) << 22;
            packed |= (lines[i+2]) << 17;
            buffer.put(packed);
        }
        buffer.flip();

        int[] ids = BuffersManager.createVBOVAO(buffer);
        vbo = ids[0];
        vao = ids[1];
        MemoryUtil.memFree(buffer);
    }

    /**
     * Dessine le mesh au coordonnées données
     * @param worldX Coordonnées en x (globale)
     * @param worldY Coordonnées en y (globale)
     * @param worldZ Coordonnées en z (globale)
     */
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

    /**
    * Vide le buffer
    */
    public static void cleanup(){
        BuffersManager.deleteVBOVAO(new int[]{vbo, vao});
    }
}