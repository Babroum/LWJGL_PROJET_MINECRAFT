package demo.Managers;

import org.lwjgl.opengl.*;
import demo.Annotations.OpenGLThread;
import java.nio.ByteBuffer;

/**
 * Gère un framebuffer object (FBO) hors écran utilisé pour le rendu en post-process.
 */
public abstract class FrameBufferManager {

    /**
     * Id OpenGL du framebuffer object.
     */
    private static int fbo;
    /**
     * Id OpenGL de la texture couleur attachée au framebuffer.
     */
    private static int colorTexture;
    /**
     * Id OpenGL du renderbuffer de profondeur attaché au framebuffer.
     */
    private static int depthRenderBuffer;
    /**
     * Id OpenGL du VAO et du VBO du quad plein écran utilisé pour dessiner la texture du framebuffer.
     */
    private static int quadVAO, quadVBO;

    /** Constructeur implicite vide */
    public FrameBufferManager(){}

    /**
     * Crée le framebuffer, sa texture couleur, son renderbuffer de profondeur et le quad plein écran.
     * @param width largeur du framebuffer en pixels
     * @param height hauteur du framebuffer en pixels
     * @throws RuntimeException si le framebuffer est incomplet
     */
    @OpenGLThread
    public static void create(int width, int height) {
        fbo = GL30.glGenFramebuffers();
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo);

        colorTexture = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, colorTexture);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGB, width, height, 0,
                GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);

        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL20.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL20.GL_CLAMP_TO_EDGE);

        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0,
                GL11.GL_TEXTURE_2D, colorTexture, 0);

        depthRenderBuffer = GL30.glGenRenderbuffers();
        GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, depthRenderBuffer);
        GL30.glRenderbufferStorage(GL30.GL_RENDERBUFFER, GL11.GL_DEPTH_COMPONENT, width, height);
        GL30.glFramebufferRenderbuffer(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT,
                GL30.GL_RENDERBUFFER, depthRenderBuffer);

        if (GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER) != GL30.GL_FRAMEBUFFER_COMPLETE) {
            throw new RuntimeException("FramebufferManagerError : FBO incomplet !");
        }

        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);

        setupQuad();
    }

    /**
     * Recrée le framebuffer, sa texture couleur et son renderbuffer de profondeur à une nouvelle taille.
     * Le quad plein écran n'est pas recréé car il ne dépend pas de la résolution.
     * @param width nouvelle largeur en pixels
     * @param height nouvelle hauteur en pixels
     * @throws RuntimeException si le framebuffer est incomplet après le resize
     */
    @OpenGLThread
    public static void resize(int width, int height) {
        // supprime l'ancien FBO et ses ressources
        GL30.glDeleteFramebuffers(fbo);
        GL11.glDeleteTextures(colorTexture);
        GL30.glDeleteRenderbuffers(depthRenderBuffer);

        // recrée tout à la nouvelle taille (réutilise la logique de create, sans refaire le quad)
        fbo = GL30.glGenFramebuffers();
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo);

        colorTexture = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, colorTexture);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGB, width, height, 0,
                GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
                
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL20.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL20.GL_CLAMP_TO_EDGE);

        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0,
                GL11.GL_TEXTURE_2D, colorTexture, 0);

        depthRenderBuffer = GL30.glGenRenderbuffers();
        GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, depthRenderBuffer);
        GL30.glRenderbufferStorage(GL30.GL_RENDERBUFFER, GL11.GL_DEPTH_COMPONENT, width, height);
        GL30.glFramebufferRenderbuffer(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT,
                GL30.GL_RENDERBUFFER, depthRenderBuffer);

        if (GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER) != GL30.GL_FRAMEBUFFER_COMPLETE) {
            throw new RuntimeException("FramebufferManagerError : FBO incomplet après resize !");
        }

        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        // pas besoin de refaire setupQuad() : le quad plein écran (-1 à 1) ne dépend pas de la résolution
    }

    /**
     * Crée le VAO/VBO du quad plein écran (coordonnées -1 à 1) utilisé pour dessiner la texture du framebuffer.
     */
    @OpenGLThread
    private static void setupQuad() {
        float[] quadVertices = {
            // pos        // uv
            -1f,  1f,      0f, 1f,
            -1f, -1f,      0f, 0f,
             1f, -1f,      1f, 0f,

            -1f,  1f,      0f, 1f,
             1f, -1f,      1f, 0f,
             1f,  1f,      1f, 1f
        };

        quadVAO = GL30.glGenVertexArrays();
        quadVBO = GL15.glGenBuffers();

        GL30.glBindVertexArray(quadVAO);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, quadVBO);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, quadVertices, GL15.GL_STATIC_DRAW);

        GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 4 * Float.BYTES, 0);
        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, 4 * Float.BYTES, 2 * Float.BYTES);
        GL20.glEnableVertexAttribArray(1);

        GL30.glBindVertexArray(0);
    }

    /**
     * Bind le framebuffer pour y dessiner (rendu hors écran).
     */
    @OpenGLThread
    public static void bindForWriting() {
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo);
    }

    /**
     * Débind le framebuffer et revient au framebuffer par défaut (l'écran).
     */
    @OpenGLThread
    public static void unbind() {
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
    }

    /**
     * Dessine le quad plein écran (utilisé pour afficher la texture du framebuffer en post-process).
     */
    @OpenGLThread
    public static void drawQuad() {
        GL30.glBindVertexArray(quadVAO);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);
        GL30.glBindVertexArray(0);
    }

    /**
     * Retourne l'id OpenGL de la texture couleur du framebuffer.
     * @return l'id de la texture
     */
    @OpenGLThread
    public static int getColorTexture() {
        return colorTexture;
    }

    /**
     * Libère toutes les ressources OpenGL du framebuffer (fbo, texture, renderbuffer, quad).
     */
    @OpenGLThread
    public static void close() {
        GL30.glDeleteFramebuffers(fbo);
        GL11.glDeleteTextures(colorTexture);
        GL30.glDeleteRenderbuffers(depthRenderBuffer);
        GL30.glDeleteVertexArrays(quadVAO);
        GL15.glDeleteBuffers(quadVBO);
    }
}