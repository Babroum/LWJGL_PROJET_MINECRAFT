package demo.Others;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.lwjgl.BufferUtils;

import demo.Managers.ShadersManager;
import demo.Rendering.Render;

public class FileLoader {
    // permet de lire les images/sons qui seront dans le .jar
    public static ByteBuffer loadFileFromClasspathBB(String path) throws IOException {
        InputStream is = Render.class.getResourceAsStream(path);
        byte[] bytes = is.readAllBytes();
        ByteBuffer buffer = BufferUtils.createByteBuffer(bytes.length);
        buffer.put(bytes).flip();
        return buffer;
    }

    // permet de lire les fichiers qui seront dans le .jar
    public static String loadShaderFromClasspathS(String path) throws IOException {
        InputStream is = ShadersManager.class.getResourceAsStream(path);
        return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }
}

