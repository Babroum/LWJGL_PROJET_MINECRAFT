package demo.Rendering;

import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import demo.Annotations.OpenGLThread;
import demo.Geometry.Chunk;
import demo.Geometry.Node3D;
import demo.Geometry.SubChunk;
import demo.Managers.ChunksManager;
import demo.Types.Color4f;

/**
 *La scène.
 */
public class SceneGraph {

    public final Node3D root = new Node3D();
    private Color4f backGroundColor = Color4f.WHITE;    

    /**
     * Définis la couleur de la scène
     * @param color Color4f
     */
    public void setBackGroundColor(Color4f color){
        this.backGroundColor = color;
    }

    /**
     * Retourne la couleur de la scène
     * @return Color4f
     */
    public Color4f getBackGroundColor(){
        return this.backGroundColor;
    }

    /**
     * Retourne le nombre de sommet dans la scène
     * @return int
     */
    public int getTotalVertexCount(){
        return this.getNumberOfVertex(root);
    }

    /**
     * Retourne le nombre de nœud dans la scène
     * @return int
     */
    public int getTotalNodeCount(){
        return this.getNumberOfNode(root) - 1;
    }

    // calcule le nombre de sommet
    private int getNumberOfVertex(Node3D node){
        int count = 0;
        for (Chunk chunk : ChunksManager.getAllChunks()) {
            for (SubChunk sub : chunk.getAllSubChunks()) {
                if (sub.mesh != null) count += sub.mesh.getVertexCount();
                if (sub.transparentMesh != null) count += sub.transparentMesh.getVertexCount();
            }
        }
        return count;
    }

    // calcule récursivement le nombre de nœud
    private int getNumberOfNode(Node3D node){
        int count = 1;

        Node3D[] children = node.getChildren();
        if (children != null){
            for (Node3D child : children){
                if (child != null){
                    count += this.getNumberOfNode(child);
                }
            }
        }

        return count;
    }

    /**
     * Retire un chunk de la scène
     * @param chunk Chunk retiré
     */
    public void removeChunk(Chunk chunk) {
        root.removeChild(chunk);
    }

    /**
     * Dessine la scène en appelant le nœud principale
     */
    @OpenGLThread
    public void drawScene(){
        Matrix4f mat = new Matrix4f();
        // Couleur du background de la scène
        GL11.glClearColor(this.backGroundColor.red, this.backGroundColor.green, this.backGroundColor.blue, 1f);
        
        // Passe 1 opaque
        GL11.glDepthMask(true);
        GL11.glDisable(GL11.GL_BLEND);
        this.root.drawNode(mat.identity());

        // Passe 2 transparent
        GL11.glDepthMask(false);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        for (Chunk chunk : ChunksManager.getAllChunks()){
            chunk.drawNodeTransparent(mat.identity());
        }
        GL11.glDepthMask(true);
        GL11.glDisable(GL11.GL_BLEND);
    }
}
