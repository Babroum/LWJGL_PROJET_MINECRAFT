package demo.Exemple;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.lwjgl.opengl.GL11;

/**
 * Maille d'un objet
 */
public class MeshEx {
    private final List<FaceEx> faces = new ArrayList<>();

    public MeshEx()
    {
    }

    public void addFace(final FaceEx face)
    {
        Objects.requireNonNull(face, "face must not be null");

        synchronized (this.faces)
        {
            this.faces.add(face);
        }
    }

    @ThreadOpenGLEx
    void drawMesh()
    {
        synchronized (this.faces)
        {
            for (final FaceEx face : this.faces)
            {
                // Annonce à OpenGL le début du polygone
                GL11.glBegin(GL11.GL_POLYGON);

                face.forEachVertex(vertex -> GL11.glVertex3f(vertex.x, vertex.y, vertex.z));

                // Annonce à OpenGL la fin du polygone
                GL11.glEnd();
            }
        }
    }
}
