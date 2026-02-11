package demo.Exemple;

import java.util.Objects;
import org.lwjgl.opengl.GL11;

/**
 * Le graphe de scène
 */
public class SceneGraphEx
{
    public final Node3DEx  root            = new Node3DEx();
    private      Color4fEx backgroundColor = Color4fEx.WHITE;

    public SceneGraphEx()
    {
    }

    public Color4fEx getBackground()
    {
        return this.backgroundColor;
    }

    public void setBackground(final Color4fEx backgroundColor)
    {
        this.backgroundColor = Objects.requireNonNull(backgroundColor, "backgroundColor must not be null");
    }

    @ThreadOpenGLEx
    void drawScene()
    {
        GL11.glClearColor(this.backgroundColor.red, this.backgroundColor.green, this.backgroundColor.blue, 1f);
        this.root.drawNode();
    }
}