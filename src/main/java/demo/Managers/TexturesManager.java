package demo.Managers;

import java.io.IOException;

import demo.Enum.TexturesType;
import demo.Others.FileLoader;
import demo.Rendering.TextureRender;

public class TexturesManager {
    
    private static TextureRender textureRender;

    public static void loadTextures(){
        try{
            // initialise en mémoire l'image des textures
            TexturesManager.textureRender = new TextureRender();
            TexturesManager.textureRender.addTexture(FileLoader.loadFileFromClasspathBB("/Textures/Blocks/null.png"), TexturesType.NULL);
            TexturesManager.textureRender.addTexture(FileLoader.loadFileFromClasspathBB("/Textures/Blocks/max.png"), TexturesType.MAX);
            TexturesManager.textureRender.addTexture(FileLoader.loadFileFromClasspathBB("/Textures/Blocks/grass_block_top.png"), TexturesType.GRASS_TOP);
            TexturesManager.textureRender.addTexture(FileLoader.loadFileFromClasspathBB("/Textures/Blocks/grass_block_side.png"), TexturesType.GRASS_SIDE);
            TexturesManager.textureRender.addTexture(FileLoader.loadFileFromClasspathBB("/Textures/Blocks/stone.png"), TexturesType.STONE);
            TexturesManager.textureRender.addTexture(FileLoader.loadFileFromClasspathBB("/Textures/Blocks/dirt.png"), TexturesType.DIRT);
            TexturesManager.textureRender.addTexture(FileLoader.loadFileFromClasspathBB("/Textures/Blocks/homer.png"), TexturesType.HOMER);
            TexturesManager.textureRender.addTexture(FileLoader.loadFileFromClasspathBB("/Textures/Blocks/sand.png"), TexturesType.SAND);
            TexturesManager.textureRender.addTexture(FileLoader.loadFileFromClasspathBB("/Textures/Blocks/bedrock.png"), TexturesType.BEDROCK);
            TexturesManager.textureRender.addTexture(FileLoader.loadFileFromClasspathBB("/Textures/Blocks/cobblestone.png"), TexturesType.COBBLESTONE);
            TexturesManager.textureRender.addTexture(FileLoader.loadFileFromClasspathBB("/Textures/Blocks/glowstone.png"), TexturesType.GLOWSTONE);
        }catch(IOException e){
            System.out.println(e);
        }
    }

    public static TextureRender getTextureRender(){
        return TexturesManager.textureRender;
    }

    /**
    * Retourne l'index de la texture
    * @param texture Textures
    * @return int
    */
    public static int getIndex(TexturesType texture){
        return texture.ordinal(); // retourne 0 pour GRASS, 1 pour COBBLESTONE etc.
    }
}
