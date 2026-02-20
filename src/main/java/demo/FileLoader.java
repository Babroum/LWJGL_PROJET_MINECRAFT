package demo;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.lwjgl.BufferUtils;

import demo.Managers.ShaderManager;
import demo.Rendering.Render;

public class FileLoader {
    // permet de lire les images qui seront dans le .jar
    public static ByteBuffer loadImageFromClasspathBB(String path) throws IOException {
        InputStream is = Render.class.getResourceAsStream(path);
        byte[] bytes = is.readAllBytes();
        ByteBuffer buffer = BufferUtils.createByteBuffer(bytes.length);
        buffer.put(bytes).flip();
        return buffer;
    }

    // permet de lire les fichiers qui seront dans le .jar
    public static String loadShaderFromClasspathS(String path) throws IOException {
        InputStream is = ShaderManager.class.getResourceAsStream(path);
        return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }

}

