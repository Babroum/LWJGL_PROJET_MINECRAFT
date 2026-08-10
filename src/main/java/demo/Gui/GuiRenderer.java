package demo.Gui;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryUtil;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.core.type.TypeReference;

import demo.Managers.BuffersManager;
import demo.Managers.ShadersManager;
import demo.Others.FileLoader;
import demo.Rendering.Window;
import demo.Types.Color4f;

public class GuiRenderer {
    /** Chemin relatif du dossier Gui */
    private static final String GUIROOTPATH = "src/main/resources/Textures/GUI";
    /** Chemin relatif de l'atlas */
    private static final String ATLASPATH = "/Textures/GUI/atlas.png";
    /** Largeur maximale de l'atlas, taille arbitraire */
    private static final int ATLASMAXWIDTH = 1024;
    /** Capacité max */
    private static final int BUFFERSIZE = 1024;
    /** Variable contenant l'atlas final */
    private static BufferedImage atlas;
    /** Id de la texture */
    private static int atlasTextureId;
    /** Ids des buffers OpenGL (vbo, vao). */
    private static int[] ids;
    /** Buffer des sommets du texte, réutilisé à chaque appel. */
    private static FloatBuffer vertexsBuffer;
    /** Nombre de vertex écrit dans le vertexsBuffer */
    private static int vertexCount;
    /** Buffer intermediaire entre JOML et OpenGl */
    private static FloatBuffer orthoBuffer = MemoryUtil.memAllocFloat(16);
    /** Alias des textures */
    private static Map<String, String> aliasMapping;
    /** Métadonnées des textures */
    private static AtlasData atlasData;

    /**
     * Class pour Jackson et la sauvegarde des infos de chaque texture
     * Le bloc principal qui contient chaque texture et la taille de l'atlas
     * écriture
     * AtlasData
     */
    public static class AtlasData {
        public int atlasWidth;
        public int atlasHeight;
        public List<TextureEntry> textures;

        /**
         * Constructeur vide requis par Jackson
         */
        public AtlasData() {}

        public AtlasData(int atlasWidth, int atlasHeight, List<TextureEntry> textures) {
            this.atlasWidth = atlasWidth;
            this.atlasHeight = atlasHeight;
            this.textures = textures;
        }
    }

    /**
     * Class pour Jackson, bloc pour chaque texture dans le bloc principal
     * écriture
     * TextureEntry
     */
    public static class TextureEntry {
        public String name;
        public String parentFolder;
        public int x;
        public int y;
        public int width;
        public int height;

        /**
         * Constructeur vide requis par Jackson
         */
        public TextureEntry() {}

        public TextureEntry(String name, String parentFolder, int x, int y, int width, int height) {
            this.name = name;
            this.parentFolder = parentFolder;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }

    /**
     * Class pour contenir le nom du fichier, le dossier parent et l'image en même temps
     * LoadedTexture
     */
    public static class LoadedTexture {
        public String name;
        public String parentFolder;
        public BufferedImage image;

        LoadedTexture(String name, String parentFolder, BufferedImage image) {
            this.name = name;
            this.image = image;
            this.parentFolder = parentFolder;
        }
    }
    
    /**
    * Permet de générer l'atlas de toutes les textures dans les sous-dossiers du dossier Gui,
    * en même temps que le JSON qui contient les informations sur la position de chaque texture.
    * @throws Exception Retournée si une texture est plus large que la largeur maximale de l'atlas définie.
    * @throws IOException Retournée si un problème survient lors de la lecture d'une image.
    */
    public static void createImagesAtlasAndJson() throws Exception, IOException{
        File root = new File(GuiRenderer.GUIROOTPATH);
        File[] subFolders = Arrays.stream(root.listFiles(File::isDirectory))
            .filter(folder -> !folder.getName().toLowerCase().contains("Font"))
            .toArray(File[]::new);

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
                    e.printStackTrace();
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

        File outputFile = new File(GuiRenderer.GUIROOTPATH + "/atlas.png");
        ImageIO.write(atlas, "png", outputFile);

        // sauvegarder les infos dans un json
        AtlasData atlasData = new AtlasData(GuiRenderer.ATLASMAXWIDTH, height, textureEntries);
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.writeValue(new File(GuiRenderer.GUIROOTPATH + "/atlasData.json"), atlasData);
    }

    public static void loadAtlasInMemory() throws Exception{
        IntBuffer widthBuffer   = MemoryUtil.memAllocInt(1);
        IntBuffer heightBuffer  = MemoryUtil.memAllocInt(1);
        IntBuffer channel = MemoryUtil.memAllocInt(1);
        ByteBuffer path   = MemoryUtil.memAlloc(1);

        try{
            path = FileLoader.loadFileFromClasspathBB(GuiRenderer.ATLASPATH);
        }catch(IOException e){
            e.printStackTrace();
            return;
        }
        
        if (path == null) throw new Exception("Erreur avec le path buffer");

        ByteBuffer pixels = STBImage.stbi_load_from_memory(path, widthBuffer, heightBuffer, channel, 4);
        if (pixels == null) throw new Exception(STBImage.stbi_failure_reason());

        GuiRenderer.atlasTextureId = GL11.glGenTextures();
        if (GuiRenderer.atlasTextureId <= 0) { return;}
         
        // définis la texture
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, GuiRenderer.atlasTextureId);       
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, widthBuffer.get(0), heightBuffer.get(0), 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
            
        // libère les mémoires
        STBImage.stbi_image_free(pixels);
        MemoryUtil.memFree(widthBuffer);
        MemoryUtil.memFree(heightBuffer);
        MemoryUtil.memFree(channel);
        MemoryUtil.memFree(path); 
        
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        
        ShadersManager.createGuiShader();        
        GuiRenderer.vertexsBuffer = MemoryUtil.memAllocFloat(GuiRenderer.BUFFERSIZE * 6 * 8); // taille buffer * vertices par quad * floats par vertex
        GuiRenderer.ids = BuffersManager.createGuiVBOVAO(vertexsBuffer);

        ObjectMapper mapper = new ObjectMapper();
        GuiRenderer.aliasMapping = mapper.readValue(new File(GuiRenderer.GUIROOTPATH + "/imageAlias.json"), new TypeReference<Map<String, String>>() {});
        GuiRenderer.atlasData = mapper.readValue(new File(GuiRenderer.GUIROOTPATH + "/atlasData.json"), AtlasData.class);
    }

    /**
     * Préparation au dessin du Gui à l'écran en utilisant le bon shader et information
     */
    public static void beginFrame(){
        GuiRenderer.vertexCount = 0;
        GuiRenderer.vertexsBuffer.clear();

        // pour écrire par dessus les autres textures
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        // pour prendre en compte la transparence
        GL11.glEnable(GL11.GL_BLEND);        
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        ShadersManager.useGuiShader();
        GL20.glActiveTexture(GL20.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, GuiRenderer.atlasTextureId);
        ShadersManager.runGuiTexture(0);
        ShadersManager.runGuiOrtho(GuiRenderer.orthoBuffer, Window.width, Window.height);        
    }

    /**
     * Envoie un quad au Gpu
     * @param x1 position en x, départ en haut à gauche
     * @param y1 position en y, départ en haut à gauche 
     * @param width largeur du quad
     * @param height hauteur du quad
     * @param u0 position u en haut à gauche
     * @param u1 position u en haut à droite
     * @param v0 position u en bas à gauche
     * @param v1 position u en bas à droite
     * @param color Couleur du quad (RGBA)
     * @throws RuntimeException Retournée si le buffer ne peut plus contenir de vertex
     */
    private static void pushQuad(float x1, float y1, float width, float height, float u0, float u1, float v0, float v1, Color4f color) throws RuntimeException{
        if (GuiRenderer.vertexCount + 6 > GuiRenderer.BUFFERSIZE * 6){throw new RuntimeException("Trop de vertex pour le Buffer Gui");}
        float x2 = x1 + width;
        float y2 = y1 + height;
        if (color == null) {color = Color4f.WHITE;}

        // On .put direct, pas de tableau intermédiaire qui serait alloué à chaque appel, donc coût sur le GC
        // triangle 1
        // haut gauche
        GuiRenderer.vertexsBuffer.put(x1); GuiRenderer.vertexsBuffer.put(y1);
        GuiRenderer.vertexsBuffer.put(u0); GuiRenderer.vertexsBuffer.put(v0);
        GuiRenderer.vertexsBuffer.put(color.red); GuiRenderer.vertexsBuffer.put(color.green); GuiRenderer.vertexsBuffer.put(color.blue); GuiRenderer.vertexsBuffer.put(color.alpha);
        // haut droite
        GuiRenderer.vertexsBuffer.put(x2); GuiRenderer.vertexsBuffer.put(y1);
        GuiRenderer.vertexsBuffer.put(u1); GuiRenderer.vertexsBuffer.put(v0);
        GuiRenderer.vertexsBuffer.put(color.red); GuiRenderer.vertexsBuffer.put(color.green); GuiRenderer.vertexsBuffer.put(color.blue); GuiRenderer.vertexsBuffer.put(color.alpha);
        // bas droite
        GuiRenderer.vertexsBuffer.put(x2); GuiRenderer.vertexsBuffer.put(y2);
        GuiRenderer.vertexsBuffer.put(u1); GuiRenderer.vertexsBuffer.put(v1);
        GuiRenderer.vertexsBuffer.put(color.red); GuiRenderer.vertexsBuffer.put(color.green); GuiRenderer.vertexsBuffer.put(color.blue); GuiRenderer.vertexsBuffer.put(color.alpha);

        // triangle 2
        // bas droite
        GuiRenderer.vertexsBuffer.put(x2); GuiRenderer.vertexsBuffer.put(y2);
        GuiRenderer.vertexsBuffer.put(u1); GuiRenderer.vertexsBuffer.put(v1);
        GuiRenderer.vertexsBuffer.put(color.red); GuiRenderer.vertexsBuffer.put(color.green); GuiRenderer.vertexsBuffer.put(color.blue); GuiRenderer.vertexsBuffer.put(color.alpha);
        // bas gauche
        GuiRenderer.vertexsBuffer.put(x1); GuiRenderer.vertexsBuffer.put(y2);
        GuiRenderer.vertexsBuffer.put(u0); GuiRenderer.vertexsBuffer.put(v1);
        GuiRenderer.vertexsBuffer.put(color.red); GuiRenderer.vertexsBuffer.put(color.green); GuiRenderer.vertexsBuffer.put(color.blue); GuiRenderer.vertexsBuffer.put(color.alpha);
        // haut gauche
        GuiRenderer.vertexsBuffer.put(x1); GuiRenderer.vertexsBuffer.put(y1);
        GuiRenderer.vertexsBuffer.put(u0); GuiRenderer.vertexsBuffer.put(v0);
        GuiRenderer.vertexsBuffer.put(color.red); GuiRenderer.vertexsBuffer.put(color.green); GuiRenderer.vertexsBuffer.put(color.blue); GuiRenderer.vertexsBuffer.put(color.alpha);

        GuiRenderer.vertexCount += 6;        
    }

    public static void endFrame(){
        // rien à dessiné
        if (vertexCount == 0) {
            GL11.glEnable(GL11.GL_DEPTH_TEST);      
            ShadersManager.useShader();
            return;
        }
        // passe en mode lecture le buffer
        GuiRenderer.vertexsBuffer.flip();

        BuffersManager.updateGuiVBO(vertexsBuffer, GuiRenderer.ids[0]);
        GL30.glBindVertexArray(ids[1]);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, GuiRenderer.vertexCount);
        GL30.glBindVertexArray(0);

        GL11.glEnable(GL11.GL_DEPTH_TEST);      
        ShadersManager.useShader();    
    }

    /**
     * Affiche une image grâce à son alias sur l'écran aux position données et d'une taille définis
     * @param alias Alias de l'image
     * @param x Position en x de l'image
     * @param y Position en y de l'image
     * @param width Largeur de l'image
     * @param height Hauteur de l'image
     * @throws RuntimeException Retournée si l'image n'a pas de taille dans les métadonnées
     */
    public static void drawImage(String alias, int x, int y, int width, int height) throws RuntimeException{
        // TODO: implémenté la largeur et la hauteur en paramètre
        try{            
            String rawName = GuiRenderer.aliasMapping.get(alias);
            String parentFolderName = rawName.substring(0, rawName.indexOf("_"));
            String imgName = rawName.substring(rawName.indexOf("_") + 1); // enlève le dossier parent dans le nom

            float xImg = 0;
            float yImg = 0;
            float widthImg = 0;
            float heightImg = 0;
            for (TextureEntry img : GuiRenderer.atlasData.textures){
                if (img.name.equals(imgName) && img.parentFolder.equals(parentFolderName)){
                    xImg = img.x;
                    yImg = img.y;
                    widthImg = img.width;
                    heightImg = img.height;
                }
            }
            if (widthImg <= 0 || heightImg <= 0){throw new RuntimeException("la taille de l'image ne peut pas être de 0 ou moins");}
            float u0 = xImg / (float) GuiRenderer.atlasData.atlasWidth;
            float u1 = (xImg + widthImg) / (float) GuiRenderer.atlasData.atlasWidth;
            float v0 = yImg / (float) GuiRenderer.atlasData.atlasHeight;
            float v1 = (yImg + heightImg) / (float) GuiRenderer.atlasData.atlasHeight;

            GuiRenderer.pushQuad(x, y, widthImg, heightImg, u0, u1, v0, v1, null);
        }catch(Exception e){
            e.printStackTrace();
            return;
        }       
    }

    /**
     * Libère le vertex Buffer et la texture.
     */
    public static void cleanup() {
        MemoryUtil.memFree(GuiRenderer.vertexsBuffer);
        GL11.glDeleteTextures(GuiRenderer.atlasTextureId);
    }
}
