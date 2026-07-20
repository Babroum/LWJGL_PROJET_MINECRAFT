package demo.Others;

import java.util.concurrent.*;

/**
 * Pool de threads dédié à la génération et au maillage des chunks
 */
public class ChunkGenerationPool {
    // Un ExecutorService c'est un pool de N threads qui restent en vie et se partagent le travail.
    /** Pool de threads fixe utilisé pour toutes les tâches de génération de chunk. */
    private static final ExecutorService pool = buildPool();

    /** Constructeur implicite vide */
    public ChunkGenerationPool(){}

    /**
     * Construit le pool de threads
     * @return ExecutorService, le pool de threads configuré
     */
    private static ExecutorService buildPool() {
        int cores   = Runtime.getRuntime().availableProcessors();
        int workers = Math.max(1, cores - 2);  // -2 pour laisser les threads OpenGL et game logic respirer
        // queue proportionnelle aux workers : assez pour les garder occupés
        // sans accumuler des milliers de tâches en mémoire simultanément
        int queueDepth = 512;

        return new ThreadPoolExecutor(
            workers, // nombre minimum de threads
            workers, // nombre maximum de threads
            0L, // les threads ne meurent jamais (comme c'est un pool fixe)
            TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(queueDepth),
            r -> { Thread t = new Thread(r); t.setDaemon(true); t.setName("ChunkWorker"); return t; },
            //new ThreadPoolExecutor.CallerRunsPolicy() // si queue pleine, le thread appelant exécute lui-même, par exemple le thread de rendu va executé au lieu de faire attendre la tache ici
            new ThreadPoolExecutor.DiscardPolicy() // si queue pleine, alors on ignore silencieusement la tâche et elle sert re ajouté au prochain tick
        );
    }

    /**
     * Envoie une tache au pool de thread
     * @param task La tache envoyer
     */
    public static void submit(Runnable task) {
        pool.submit(task);
    }

    /**
     * Envoie une tache au pool de thread, et retourne la complétion de la tâche
     * @param task La tache envoyer
     * @return Complétion de la tâche
     */
    public static Future<?> submitFuture(Runnable task) {
        return pool.submit(task);
    }
}