package demo;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

import demo.Managers.BenchmarkManager;
import demo.Others.ChunkSerializer;
import demo.Rendering.GuiRenderer;
import demo.Rendering.SceneGraph;
import demo.Rendering.Window;

/**
 * Point d'entrée du programme
 */
public class App 
{      
    /** Constructeur implicite vide */
    public App(){}
    /*
    mvn exec:exec IMPORTANT car sinon vscode ne prend pas en compte les arguments java en lançant par le bouton classique
    donc pas les optimisations grâce au ZGC

    north = 0
    south = 1
    west = 2
    east = 3
    up = 4
    down = 5
    all = 6
     */
    /**
     * Point d'entrée du programme, crée la fenêtre puis démarre le rendu
     * @param args String[], arguments de la ligne de commande, non utilisés
     * @throws Exception si l'initialisation de la fenêtre échoue
     */
    public static void main( String[] args ) throws Exception
    {   
        /*
        Lancer les 3 lignes ci-dessous seulement si de nouvelles textures ont été ajoutées.
        Les 2 premières ne servent pas au jeu, mais seulement pour savoir à quoi ressemble telle texture.
        */
        //List<GuiImageEntry> entries = listAllGuiFiles("src/main/resources/Textures/GUI");
        //mergeFilesGrid(entries, "atlas_GUI.png");
        //GuiRenderer.createImagesAtlasAndJson(); 
        
        // création de la fenêtre 
        new Window(1200,800);
        // donne le draw en paramètre
        // l'exécutera après l'initialisation d'opengl
        Window.startWindow(() -> draw(Window.getSceneGraph()));
    }

    /**
     * Exécuté une fois le contexte OpenGL prêt : initialise la sauvegarde et le benchmark
     * @param scene SceneGraph, la scène de la fenêtre (non utilisée directement ici)
     */
    public static void draw(SceneGraph scene){
        ChunkSerializer.init(); // créer le dossier saves/world/ si n'existe pas
        BenchmarkManager.getInstance().start();
    }  

    // Génère une planche qui contient tout les éléments dans GUI avec leurs nom pour aider à trouvé une bonne texture plus facilement

    static class GuiImageEntry {
        String path;
        String displayName;

        GuiImageEntry(String path, String displayName) {
            this.path = path;
            this.displayName = displayName;
        }
    }

    static List<GuiImageEntry> listAllGuiFiles(String guiRootPath) throws IOException {
        List<GuiImageEntry> entries = new ArrayList<>();

        File root = new File(guiRootPath);
        File[] subFolders = root.listFiles(File::isDirectory);

        if (subFolders == null) {
            throw new IllegalStateException("Dossier introuvable : " + guiRootPath);
        }

        for (File folder : subFolders) {
            String colorName = folder.getName(); // "Blue", "Green", "Extra", etc.

            File[] images = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".png"));
            if (images == null) continue;

            for (File image : images) {
                String rawName = image.getName(); // "btn_02_n.png"
                String nameWithoutExtension = rawName.substring(0, rawName.lastIndexOf('.'));
                String displayName = colorName + "_" + nameWithoutExtension; // "Blue_btn_02_n"
                entries.add(new GuiImageEntry(image.getAbsolutePath(), displayName));
            }
        }

        return entries;
    }

    static List<String> wrapTextOnUnderscore(String text, FontMetrics fm, int maxWidth) {
        List<String> lines = new ArrayList<>();
        String[] parts = text.split("_");

        StringBuilder currentLine = new StringBuilder();

        for (String part : parts) {
            String candidate = currentLine.length() == 0 ? part : currentLine + "_" + part;

            if (fm.stringWidth(candidate) > maxWidth && currentLine.length() > 0) {
                // La ligne actuelle est pleine, on la valide et on repart avec "part"
                lines.add(currentLine.toString());
                currentLine = new StringBuilder(part);
            } else {
                currentLine = new StringBuilder(candidate);
            }
        }

        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }

        return lines;
    }

    static void mergeFilesGrid(List<GuiImageEntry> entries, String fileName) throws IOException {
        List<BufferedImage> images = new ArrayList<>();
        for (GuiImageEntry entry : entries) {
            BufferedImage image = ImageIO.read(new File(entry.path));
            images.add(image);
        }

        int count = images.size();

        int cols = (int) Math.ceil(Math.sqrt(count));
        int rows = (int) Math.ceil((double) count / cols);

        int cellImgWidth = 0;
        int cellImgHeight = 0;
        for (BufferedImage img : images) {
            cellImgWidth = Math.max(cellImgWidth, img.getWidth());
            cellImgHeight = Math.max(cellImgHeight, img.getHeight());
        }

        int padding = 10;
        int cellWidth = cellImgWidth + padding * 2;

        // Prépare la police et mesure AVANT de calculer les hauteurs, pour connaitre fm
        Font font = new Font("Arial", Font.PLAIN, 12);
        BufferedImage dummy = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D dummyG2d = dummy.createGraphics();
        dummyG2d.setFont(font);
        FontMetrics fm = dummyG2d.getFontMetrics();
        dummyG2d.dispose();

        int maxTextWidth = cellWidth - padding; // marge pour le wrap

        // Pré-calcule les lignes de texte de chaque entrée, et la hauteur de texte max nécessaire
        List<List<String>> allWrappedLines = new ArrayList<>();
        int maxLineCount = 1;
        for (GuiImageEntry entry : entries) {
            List<String> wrapped = wrapTextOnUnderscore(entry.displayName, fm, maxTextWidth);
            allWrappedLines.add(wrapped);
            maxLineCount = Math.max(maxLineCount, wrapped.size());
        }

        int lineHeight = fm.getHeight();
        int textBlockHeight = maxLineCount * lineHeight + 4; // petite marge

        int cellHeight = cellImgHeight + padding * 2 + textBlockHeight;

        int totalWidth = cellWidth * cols;
        int totalHeight = cellHeight * rows;

        BufferedImage atlas = new BufferedImage(totalWidth, totalHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = atlas.createGraphics();

        g2d.setColor(Color.LIGHT_GRAY);
        g2d.fillRect(0, 0, totalWidth, totalHeight);

        g2d.setFont(font);

        for (int i = 0; i < count; i++) {
            BufferedImage img = images.get(i);
            List<String> lines = allWrappedLines.get(i);

            int col = i % cols;
            int row = i / cols;

            int cellX = col * cellWidth;
            int cellY = row * cellHeight;

            g2d.setColor(Color.DARK_GRAY);
            g2d.drawRect(cellX + 1, cellY + 1, cellWidth - 2, cellHeight - 2);

            int imgX = cellX + padding + (cellImgWidth - img.getWidth()) / 2;
            int imgY = cellY + padding + (cellImgHeight - img.getHeight()) / 2;

            g2d.drawImage(img, imgX, imgY, null);

            g2d.setColor(Color.BLACK);
            int textStartY = cellY + padding + cellImgHeight + fm.getAscent() + 2;

            for (int lineIdx = 0; lineIdx < lines.size(); lineIdx++) {
                String line = lines.get(lineIdx);
                int textWidth = fm.stringWidth(line);
                int textX = cellX + (cellWidth - textWidth) / 2;
                int textY = textStartY + lineIdx * lineHeight;
                g2d.drawString(line, textX, textY);
            }
        }

        g2d.dispose();

        File outputFile = new File(fileName);
        ImageIO.write(atlas, "png", outputFile);

        System.out.println("Atlas : " + fileName + " (" + totalWidth + "x" + totalHeight + ")");
    }
}