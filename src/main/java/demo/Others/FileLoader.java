package demo.Others;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.lwjgl.system.MemoryUtil;

import demo.Managers.ShadersManager;
import demo.Rendering.Render;

/**
 * Permet de lire des fichiers
 */
public class FileLoader {

    /** Constructeur implicite vide */
    public FileLoader(){}

    /**
     * Lit un fichier (image/son) depuis le classpath (.jar) et le retourne en ByteBuffer off-heap
     * @param path chemin du fichier dans le classpath
     * @return ByteBuffer, le contenu du fichier
     * @throws IOException si la lecture du fichier échoue
     */
    public static ByteBuffer loadFileFromClasspathBB(String path) throws IOException {
        InputStream is = Render.class.getResourceAsStream(path);
        byte[] bytes = is.readAllBytes();
        ByteBuffer buffer = MemoryUtil.memAlloc(bytes.length);
        buffer.put(bytes).flip();
        return buffer;
    }

    /**
     * Lit un fichier shader depuis le classpath (.jar) et le retourne en String
     * @param path chemin du fichier dans le classpath
     * @return String, le contenu du fichier
     * @throws IOException si la lecture du fichier échoue
     */
    public static String loadShaderFromClasspathS(String path) throws IOException {
        InputStream is = ShadersManager.class.getResourceAsStream(path);
        return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }
}