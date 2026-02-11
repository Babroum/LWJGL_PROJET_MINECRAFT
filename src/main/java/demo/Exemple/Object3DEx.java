package demo.Exemple;

import java.util.Objects;

/**
 * Objet 3D
 */
public class Object3DEx extends Node3DEx
{
    public final MeshEx mesh = new MeshEx();
    private Color4fEx color = Color4fEx.GREY;

    public Object3DEx()
    {
    }

    public Color4fEx getColor()
    {
        return this.color;
    }

    public void setColor(final Color4fEx color)
    {
        this.color = Objects.requireNonNull(color, "color must not be null");
    }

    @ThreadOpenGLEx
    @Override
    void drawSpecific()
    {
        this.color.apply();
        this.mesh.drawMesh();
    }
}
