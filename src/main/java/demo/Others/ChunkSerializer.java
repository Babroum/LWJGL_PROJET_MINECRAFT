package demo.Others;

import java.io.IOException;
import java.nio.file.*;
import java.util.Comparator;

import demo.Geometry.Chunk;
import demo.Managers.ChunksManager;

public class ChunkSerializer {
    private static final String SAVE_DIR = "saves/world/";

    public static void init(){
        try{                       
            Path path = Paths.get(ChunkSerializer.SAVE_DIR);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            } else {
                // Vider le dossier sans le supprimer lui-même
                try {
                    Files.walk(path)
                        .sorted(Comparator.reverseOrder())
                        .skip(1) // Saute la racine
                        .forEach(file -> {
                            try {
                                // Supprime le fichier ou le dossier (doit être vide)
                                Files.delete(file); 
                            } catch (IOException e) {
                                // Affiche quel fichier pose problème pour le débogage
                                System.err.println("Échec suppression: " + file + " - " + e.getMessage());
                                throw new RuntimeException("Impossible de supprimer : " + file, e);
                            }
                        });
                    System.out.println("Nettoyage terminé.");
                } catch (IOException e) {
                     System.out.println("Erreur lors du parcours du dossier"+e);
                }
            }
        }catch (IOException e){
            System.out.println(e);
        }
    }

    public static void save(Chunk chunk){
        byte[] blocks = chunk.getAllSubChunkBlocks();
        Path path = getPath(ChunksManager.floorDiv(chunk.x, 16), ChunksManager.floorDiv(chunk.z, 16));
        Path tempPath = path.resolveSibling(path.getFileName() + ".tmp");

        try {
            // écrit dans un fichier temporaire
            Files.write(tempPath, blocks, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            
            // déplace le fichier temporaire vers le fichier final
            // cela garantit que le fichier final n'est jamais partiel ou verrouillé pendant l'écriture
            Files.move(tempPath, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            System.err.println("Erreur sauvegarde chunk " + chunk.x + "," + chunk.z + ": " + e.getMessage());
            // nettoyer le fichier temporaire en cas d'échec
            try { Files.deleteIfExists(tempPath); } catch (IOException ex) {}
        }
    }

    public static byte[] load(int cx, int cz){
        Path path = getPath(cx, cz);
        if (!Files.exists(path)) return null;
        try {
            return Files.readAllBytes(path);
        } catch (IOException e) {
            // Cas fréquent sous Windows : "Le processus ne peut pas accéder au fichier car il est utilisé par un autre processus"
            System.err.println("Erreur lecture chunk " + cx + "," + cz + " (Fichier peut-être verrouillé): " + e.getMessage());
            return null;
        }
    }

    /**
     * Retourne la Path pour un chunk
     * @param cx Coordonnées en x (locale)
     * @param cz Coordonnées en x (locale)
     * @return Retourne le Path, exemple : ".../1_2.chunk"
     */
    private static Path getPath(int cx, int cz){
        return Paths.get(ChunkSerializer.SAVE_DIR + cx + "_" + cz + ".chunk");
    }

    private static boolean exists(int cx, int cz){
        Path path = ChunkSerializer.getPath(cx, cz);
        return Files.exists(path);
    }
}
