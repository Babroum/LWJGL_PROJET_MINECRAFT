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

/**
 * Gère le chargement et le binding du tableau de textures (texture array) des blocs
 */
public class TextureManager {
    /**
     * Id OpenGL du tableau de textures (texture array) contenant toutes les textures de blocs.
     */
    private static int textureArrayId;
    /**
     * Associe le nom d'une texture à son index (couche) dans le tableau de textures.
     */
    private static final Map<String, Integer> nameToIndex = new HashMap<>();
    /**
     * Emplacement uniforme "tex" du shader principal, mis en cache après la première recherche.
     * -2 = pas encore recherché ; -1 = recherché mais introuvable.
     */
    private static int texUniformLocation = -2; // -2 = pas encore recherché ; -1 = recherché mais introuvable
    /**
     * Emplacement uniforme "tex" du shader liquide, mis en cache après la première recherche.
     * -2 = pas encore recherché ; -1 = recherché mais introuvable.
     */
    private static int texUniformLocationLiquid = -2;

    /** Constructeur implicite vide */
    public TextureManager(){}

    /**
     * Initialise les textures données en paramètre
     * @param files les textures
     * @throws Exception si une texture ne peut pas être chargée depuis le classpath
     */
    public static void init(String[] files) throws Exception {
        textureArrayId = GL11.glGenTextures();
        GL11.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, textureArrayId);

        // Allocation de l'Array (Profondeur = nombre de fichiers dans la liste)
        GL30.glTexImage3D(GL30.GL_TEXTURE_2D_ARRAY, 0, GL11.GL_RGBA, 16, 16, files.length, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer)null);

        for (int i = 0; i < files.length; i++) {
            String name = files[i];
            loadToGL("/Textures/Blocks/" + name + ".png", i);
            nameToIndex.put(name, i);
        }

        GL11.glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
    }

    /**
     * Charge les textures pour OpenGL
     * @param path Chemin relatif de la texture
     * @param layer Indice de la texture
     * @throws Exception si le fichier de texture ne peut pas être chargé depuis le classpath
     */
    private static void loadToGL(String path, int layer) throws Exception {
        ByteBuffer imageBuffer = FileLoader.loadFileFromClasspathBB(path);

        IntBuffer w = MemoryUtil.memAllocInt(1);
        IntBuffer h = MemoryUtil.memAllocInt(1);
        IntBuffer c = MemoryUtil.memAllocInt(1);

        // STB charge directement depuis le ByteBuffer
        ByteBuffer pixels = STBImage.stbi_load_from_memory(imageBuffer, w, h, c, 4);
        
        GL30.glTexSubImage3D(GL30.GL_TEXTURE_2D_ARRAY, 0, 0, 0, layer, 16, 16, 1, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);

        STBImage.stbi_image_free(pixels);
        MemoryUtil.memFree(w); MemoryUtil.memFree(h); MemoryUtil.memFree(c);
        MemoryUtil.memFree(imageBuffer);
    }

    /**
     * Bind la texture pour le shader principal (opaque/glass)
     */
    public static void bind() {
        GL30.glActiveTexture(GL30.GL_TEXTURE0);
        GL11.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, textureArrayId);
        // recherché une seule fois (1er appel), mis en cache pour toutes les frames suivantes
        if (texUniformLocation == -2) {
            texUniformLocation = GL30.glGetUniformLocation(ShadersManager.getProgramId(), "tex");
        }
        if (texUniformLocation != -1) {
            GL30.glUniform1i(texUniformLocation, 0);
        }
    }
    
    /**
     * Bind la texture pour le shader liquide
     */
    public static void bindLiquid() {
        GL30.glActiveTexture(GL30.GL_TEXTURE0);
        GL11.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, textureArrayId);
        if (texUniformLocationLiquid == -2) {
            texUniformLocationLiquid = GL30.glGetUniformLocation(ShadersManager.getProgramLiquidId(), "tex");
        }
        if (texUniformLocationLiquid != -1) {
            GL30.glUniform1i(texUniformLocationLiquid, 0);
        }
    }

    /**
     * Retourne l'index (couche) d'une texture dans le tableau de textures à partir de son nom.
     * @param name nom de la texture
     * @return l'index de la texture, ou 0 si le nom est introuvable
     */
    public static int getIndex(String name) {
        return nameToIndex.getOrDefault(name, 0);
    }
}