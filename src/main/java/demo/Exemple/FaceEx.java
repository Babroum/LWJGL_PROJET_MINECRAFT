package demo.Exemple;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Décrit une face d'une maille
 */
public class FaceEx {
    private final List<VertexEx> vertexs = new ArrayList<>();

    // Vertex... permet de récupérer n'importe quel nombre de vertex en paramètre
    public FaceEx(VertexEx... vertexs)
    {
        if (vertexs != null)
        {
            Collections.addAll(this.vertexs, vertexs);
        }
    }

    public void addVertex(float x, float y, float z)
    {
        this.addVertex(new VertexEx(x, y, z));
    }
    // Surcharge 
    public void addVertex(final VertexEx vertex)
    {
        Objects.requireNonNull(vertex, "vertex must not be null");

        synchronized (this.vertexs)
        {
            this.vertexs.add(vertex);
        }
    }

    public void forEachVertex(final Consumer<VertexEx> action)
    {
        Objects.requireNonNull(action, "action must not be null");

        synchronized (this.vertexs)
        {
            for (final VertexEx vertex : this.vertexs)
            {
                action.accept(vertex);
            }
        }
    }
}
