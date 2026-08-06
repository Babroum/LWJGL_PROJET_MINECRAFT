package demo.Rendering;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import demo.Rendering.GuiRenderer.AtlasData;
import demo.Rendering.GuiRenderer.TextureEntry;

public class GuiRenderer {
    private static final String GUIROOTPATH = "src/main/resources/Textures/GUI";
    private static final int ATLASMAXWIDTH = 1024;
    private static BufferedImage atlas;

    // Class pour jackson et la sauvegarde des infos de chaque texture
    // Le bloc principal qui contient chaque texture et la taille de l'atlas
    public static class AtlasData {
        public int atlasWidth;
        public int atlasHeight;
        public List<TextureEntry> textures;

        public AtlasData() {} // constructeur vide requis par Jackson

        public AtlasData(int atlasWidth, int atlasHeight, List<TextureEntry> textures) {
            this.atlasWidth = atlasWidth;
            this.atlasHeight = atlasHeight;
            this.textures = textures;
        }
    }

    // Bloc pour chaque texture dans le bloc principal
    public static class TextureEntry {
        public String name;
        public String parentFolder;
        public int x;
        public int y;
        public int width;
        public int height;

        public TextureEntry() {} // constructeur vide requis par Jackson

        public TextureEntry(String name, String parentFolder, int x, int y, int width, int height) {
            this.name = name;
            this.parentFolder = parentFolder;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }

    // pour contenir le nom du fichier et l'image en même temps
    public static class LoadedTexture {
        String name;
        String parentFolder;
        BufferedImage image;

        LoadedTexture(String name, String parentFolder, BufferedImage image) {
            this.name = name;
            this.image = image;
            this.parentFolder = parentFolder;
        }
    }
    
    public static void createImagesAtlas() throws Exception{
        File root = new File(GuiRenderer.GUIROOTPATH);
        File[] subFolders = root.listFiles(File::isDirectory);
        if (subFolders == null) {
            throw new IllegalStateException("Dossier introuvable : " + GUIROOTPATH);
        }        

        int imagesCount = 0;
        for (File folder : subFolders){
            File[] imagesFile = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".png"));
            if (imagesFile == null) continue;
            imagesCount += imagesFile.length;
        }
        LoadedTexture[] images = new LoadedTexture[imagesCount];
        
        int imageIndex = 0;
        for (File folder : subFolders) {
            String folderName = folder.getName();
            File[] imagesFile = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".png"));       
            if (imagesFile == null) continue;

            for (File imageFile : imagesFile) {
                // On parcourt toutes les images de tous les sous-dossiers.
                try{
                    BufferedImage image = ImageIO.read(new File(imageFile.getAbsolutePath()));
                    int imageHeight = image.getHeight();
                    // Les images sont ensuite triées par taille, la plus haute en premier, par ordre décroissant
                    int i = imageIndex - 1;
                    // On cherche l'indice pour mettre l'image
                    while (i >= 0){
                        if (images[i].image.getHeight() > imageHeight){break;} else {i--;}
                    }      
                    // Puis on décale tout le reste pour faire de la place
                    for (int j = imagesCount - 1; j > i + 1; j--){
                        images[j] = images[j - 1];
                    }        
                    images[i + 1] = new LoadedTexture(imageFile.getName(), folderName, image);
                    imageIndex++;
                }catch(IOException e){
                    System.err.println(e);
                }                
            }
        }

        // Maintenant, il faut faire l'atlas avec la méthode de shelf-packing
        // (On place les images les plus grandes en premier, jusqu'à arriver à la limite de largeur, puis on saute une ligne, et ainsi de suite)
        int width = 0;
        int height = images[0].image.getHeight();
        int imageStartRowIndex = 0;
        for (int i = 0; i < imagesCount; i++){
            if (images[i].image.getWidth() > GuiRenderer.ATLASMAXWIDTH){
                throw new Exception("Image n°" + i + ", "+ images[i].image.getWidth() +"px, plus grande que la taille max de l'atlas, " + GuiRenderer.ATLASMAXWIDTH + "px");
            }
            if (width + images[i].image.getWidth() > GuiRenderer.ATLASMAXWIDTH){
                // On a atteint la largeur max donc la prochaine image est au début de la ligne. On note donc sa hauteur.
                height += images[imageStartRowIndex].image.getHeight();
                width = images[i].image.getWidth();
                imageStartRowIndex = i;
            }else{
                // sinon on continue à placer les images à la suite
                width += images[i].image.getWidth();
            }
        }

        GuiRenderer.atlas = new BufferedImage(GuiRenderer.ATLASMAXWIDTH, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = atlas.createGraphics();
        List<TextureEntry> textureEntries = new ArrayList<>();

        int imageX = 0;
        int imageY = 0;
        imageStartRowIndex = 0;
        for (int i = 0; i < imagesCount; i++){        
            if (imageX + images[i].image.getWidth() > GuiRenderer.ATLASMAXWIDTH){
                imageY += images[imageStartRowIndex].image.getHeight();
                g2d.drawImage(images[i].image, 0, imageY, null);
                textureEntries.add(new TextureEntry(images[i].name, images[i].parentFolder, imageX, imageY, images[i].image.getWidth(), images[i].image.getHeight()));    
                imageX = images[i].image.getWidth();                
                imageStartRowIndex = i;
            }else{
                g2d.drawImage(images[i].image, imageX, imageY, null);
                textureEntries.add(new TextureEntry(images[i].name, images[i].parentFolder, imageX, imageY, images[i].image.getWidth(), images[i].image.getHeight()));    
                imageX += images[i].image.getWidth();
            }
        }
        
        g2d.dispose();

        File outputFile = new File("src/main/resources/Textures/GUI/atlas.png");
        ImageIO.write(atlas, "png", outputFile);

        // sauvegarder les infos dans un json
        AtlasData atlasData = new AtlasData(GuiRenderer.ATLASMAXWIDTH, height, textureEntries);
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.writeValue(new File("src/main/resources/Textures/GUI/atlasInfo.json"), atlasData);
    }
}
