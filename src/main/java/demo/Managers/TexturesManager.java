package demo.Managers;

import java.io.IOException;

import demo.FileLoader;
import demo.Enum.TexturesType;
import demo.Rendering.TextureRender;

public class TexturesManager {
    
    private static TextureRender textureRender;

    public static void loadTextures(){
        try{
            // initialise en mémoire l'image des textures
            TexturesManager.textureRender = new TextureRender();
            TexturesManager.textureRender.addTexture(FileLoader.loadImageFromClasspathBB("/Textures/Blocks/null.png"), TexturesType.NULL);
            TexturesManager.textureRender.addTexture(FileLoader.loadImageFromClasspathBB("/Textures/Blocks/max.png"), TexturesType.MAX);
            TexturesManager.textureRender.addTexture(FileLoader.loadImageFromClasspathBB("/Textures/Blocks/grass_block_top.png"), TexturesType.GRASS_TOP);
            TexturesManager.textureRender.addTexture(FileLoader.loadImageFromClasspathBB("/Textures/Blocks/grass_block_side.png"), TexturesType.GRASS_SIDE);
            TexturesManager.textureRender.addTexture(FileLoader.loadImageFromClasspathBB("/Textures/Blocks/stone.png"), TexturesType.STONE);
            TexturesManager.textureRender.addTexture(FileLoader.loadImageFromClasspathBB("/Textures/Blocks/dirt.png"), TexturesType.DIRT);
            TexturesManager.textureRender.addTexture(FileLoader.loadImageFromClasspathBB("/Textures/Blocks/homer.png"), TexturesType.HOMER);
        }catch(IOException e){
            System.out.println(e);
        }
    }

    public static TextureRender getTextureRender(){
        return TexturesManager.textureRender;
    }
}
