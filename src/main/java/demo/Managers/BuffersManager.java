package demo.Managers;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import demo.Annotations.OpenGLThread;
import demo.Geometry.Chunk;
import demo.Geometry.Node3D;
import demo.Geometry.Object3D;

/**
 * Gère les buffers tel que VBO VAO EBO
 */
public class BuffersManager {

    public static int activeVAOCount = 0;
    
    /**
     * Créer les buffers en les remplissant avant les données données
     * @param vertex IntBuffer, sommet à affiché
     * @return
     */
    @OpenGLThread
    public static int[] createChunkVBOVAO(IntBuffer vertex){
        // VAO = configuration de comment lire un vbo
        // 1. VAO, créer, binder et uploader
        int vao = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vao);

        // VBO = données brut à afficher
        // 2. VBO, créer, binder et uploader
        int vbo = GL20.glGenBuffers();
        GL15.glBindBuffer(GL20.GL_ARRAY_BUFFER, vbo); 
        GL15.glBufferData(GL20.GL_ARRAY_BUFFER, vertex, GL20.GL_STATIC_DRAW);

        // 3. Dire au VAO comment lire le VBO
        GL30.glVertexAttribIPointer(0, 1, GL20.GL_INT, Integer.BYTES, 0);
        GL20.glEnableVertexAttribArray(0);

        // 4. Unbind
        GL15.glBindBuffer(GL20.GL_ARRAY_BUFFER, 0);
        GL30.glBindVertexArray(0);

        // retourne les références des buffers
        activeVAOCount++;
        return new int[]{vbo, vao};
    }

    // pareil que en haut mais pour les shaders de texte
    @OpenGLThread
    public static int[] createTextVBOVAOEBO(FloatBuffer vertex, IntBuffer indices){
        // VAO = configuration de comment lire un vbo
        // 1. VAO, créer, binder et uploader
        int vao = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vao);

        // VBO = données brut à afficher
        // 2. VBO, créer, binder et uploader
        int vbo = GL20.glGenBuffers();
        GL15.glBindBuffer(GL20.GL_ARRAY_BUFFER, vbo); 
        GL15.glBufferData(GL20.GL_ARRAY_BUFFER, vertex, GL20.GL_STATIC_DRAW);

        // EBO = les indices pour lire les sommets pour évité les duplications de sommets
        // 3. EBO — doit être bindé PENDANT que le VAO est actif
        int ebo = GL20.glGenBuffers();
        GL15.glBindBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER, ebo);
        GL15.glBufferData(GL20.GL_ELEMENT_ARRAY_BUFFER, indices, GL20.GL_STATIC_DRAW);

        // 3. Dire au VAO comment lire le VBO
        GL20.glVertexAttribPointer(0, 2, GL20.GL_FLOAT, false, 4 * Float.BYTES, 0);
        GL20.glEnableVertexAttribArray(0);

        GL20.glVertexAttribPointer(1, 2, GL20.GL_FLOAT, false, 4 * Float.BYTES, 2 * Float.BYTES);
        GL20.glEnableVertexAttribArray(1);

        // 4. Unbind
        GL15.glBindBuffer(GL20.GL_ARRAY_BUFFER, 0);
        GL30.glBindVertexArray(0);

        // retourne les références des buffers
        activeVAOCount++;
        return new int[]{vbo, vao, ebo};
    }

    /**
     * Mets à jour le vbo d'un chunk
     * @param IntBuffer, données à ajouté
     */
    @OpenGLThread
    public static void updateChunkVBO(IntBuffer vertex, int vbo){
        // update les données sans recréer les buffers
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, vertex);
    }

    /**
     * Mets à jour le vbo et ebo du texte
     * @param vertex FloatBuffer
     * @param indices IntBuffer
     * @param vbo int
     * @param ebo int
     */
    @OpenGLThread
    public static void updateTextVBOEBO(FloatBuffer vertex, IntBuffer indices, int vbo, int ebo){
        // update les données sans recréer les buffers
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, vertex);

        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ebo);
        GL15.glBufferSubData(GL15.GL_ELEMENT_ARRAY_BUFFER, 0, indices);
    }

    @OpenGLThread
    public static void deleteVBOVAOEBO(int[] ids){
        GL30.glDeleteVertexArrays(ids[0]); // vao
        GL20.glDeleteBuffers(ids[1]); // vbo
        GL20.glDeleteBuffers(ids[2]); // ebo 
        activeVAOCount--;
    }

    @OpenGLThread
    public static void deleteVBOVAO(int[] ids){
        GL30.glDeleteVertexArrays(ids[0]); // vao
        GL20.glDeleteBuffers(ids[1]); // vbo
        activeVAOCount--;
    }

    @OpenGLThread
    public static void setupVBOVAOEBORecursive(Node3D node){
        if (node == null) return;
        if (node.getChildren() == null) return;

        // on récursive sur les enfants
        for(Node3D child : node.getChildren()){
            setupVBOVAOEBORecursive(child);
        }

        if (node instanceof Object3D){
            ((Object3D)node).setupObjectBuffers();
        }
        
        if (node instanceof Chunk){
            ((Chunk)node).setupChunkBuffers();
        }
    }
}
