package demo.Managers;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.stb.STBImage;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;
import org.lwjgl.system.MemoryUtil;
import demo.Others.FileLoader;

public class TextureManager {
    private static int textureArrayId;
    private static final Map<String, Integer> nameToIndex = new HashMap<>();

    public static void init(String[] files) throws Exception {
        textureArrayId = GL11.glGenTextures();
        GL11.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, textureArrayId);

        // Allocation de l'Array (Profondeur = nombre de fichiers dans notre liste)
        GL30.glTexImage3D(GL30.GL_TEXTURE_2D_ARRAY, 0, GL11.GL_RGBA, 16, 16, files.length, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer)null);

        for (int i = 0; i < files.length; i++) {
            String name = files[i];
            loadToGL("/Textures/Blocks/" + name + ".png", i);
            nameToIndex.put(name, i);
        }

        GL11.glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
    }

    private static void loadToGL(String path, int layer) throws Exception {
        ByteBuffer imageBuffer = FileLoader.loadFileFromClasspathBB(path);

        IntBuffer w = MemoryUtil.memAllocInt(1);
        IntBuffer h = MemoryUtil.memAllocInt(1);
        IntBuffer c = MemoryUtil.memAllocInt(1);

        // STB charge directement depuis ton ByteBuffer
        ByteBuffer pixels = STBImage.stbi_load_from_memory(imageBuffer, w, h, c, 4);
        
        GL30.glTexSubImage3D(GL30.GL_TEXTURE_2D_ARRAY, 0, 0, 0, layer, 16, 16, 1, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);

        STBImage.stbi_image_free(pixels);
        MemoryUtil.memFree(w); MemoryUtil.memFree(h); MemoryUtil.memFree(c);
        MemoryUtil.memFree(imageBuffer);
    }

    public static void bind() {
        GL30.glActiveTexture(GL30.GL_TEXTURE0);
        
        GL11.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, textureArrayId);

        int location = GL30.glGetUniformLocation(ShadersManager.getProgramId(), "tex");
        if (location != -1) {
            GL30.glUniform1i(location, 0);
        }
    }

    public static int getIndex(String name) {
        return nameToIndex.getOrDefault(name, 0);
    }
}