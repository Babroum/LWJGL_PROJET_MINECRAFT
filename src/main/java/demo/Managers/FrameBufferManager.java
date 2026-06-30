package demo.Managers;

import org.lwjgl.opengl.*;
import demo.Annotations.OpenGLThread;
import java.nio.ByteBuffer;

public abstract class FrameBufferManager {

    private static int fbo;
    private static int colorTexture;
    private static int depthRenderBuffer;
    private static int quadVAO, quadVBO;

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

    @OpenGLThread
    public static void bindForWriting() {
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo);
    }

    @OpenGLThread
    public static void unbind() {
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
    }

    @OpenGLThread
    public static void drawQuad() {
        GL30.glBindVertexArray(quadVAO);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);
        GL30.glBindVertexArray(0);
    }

    @OpenGLThread
    public static int getColorTexture() {
        return colorTexture;
    }

    @OpenGLThread
    public static void close() {
        GL30.glDeleteFramebuffers(fbo);
        GL11.glDeleteTextures(colorTexture);
        GL30.glDeleteRenderbuffers(depthRenderBuffer);
        GL30.glDeleteVertexArrays(quadVAO);
        GL15.glDeleteBuffers(quadVBO);
    }
}