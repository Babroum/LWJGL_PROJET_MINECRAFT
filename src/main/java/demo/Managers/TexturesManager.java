package demo.Managers;

import java.io.IOException;

import demo.FileLoader;
import demo.Enum.Textures;
import demo.Rendering.TextureRender;

public class TexturesManager {
    
    private static TextureRender textureRender;

    public static void loadTextures(){
        try{
            // initialise en mémoire l'image des textures
            TexturesManager.textureRender = new TextureRender();
            TexturesManager.textureRender.addTexture(FileLoader.loadImageFromClasspathBB("/Textures/Blocks/null.png"), Textures.NULL);
            TexturesManager.textureRender.addTexture(FileLoader.loadImageFromClasspathBB("/Textures/Blocks/max.png"), Textures.MAX);
            TexturesManager.textureRender.addTexture(FileLoader.loadImageFromClasspathBB("/Textures/Blocks/grass_block_top.png"), Textures.GRASS_TOP);
            TexturesManager.textureRender.addTexture(FileLoader.loadImageFromClasspathBB("/Textures/Blocks/grass_block_side.png"), Textures.GRASS_SIDE);
            TexturesManager.textureRender.addTexture(FileLoader.loadImageFromClasspathBB("/Textures/Blocks/stone.png"), Textures.STONE);
            TexturesManager.textureRender.addTexture(FileLoader.loadImageFromClasspathBB("/Textures/Blocks/dirt.png"), Textures.DIRT);
            TexturesManager.textureRender.addTexture(FileLoader.loadImageFromClasspathBB("/Textures/Blocks/homer.png"), Textures.HOMER);
        }catch(IOException e){
            System.out.println(e);
        }
    }

    public static TextureRender getTextureRender(){
        return TexturesManager.textureRender;
    }
}
