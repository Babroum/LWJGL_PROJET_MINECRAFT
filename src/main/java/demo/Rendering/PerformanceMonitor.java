package demo.Rendering;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Suivi des performances du moteur (FPS, mémoire, draw calls, etc.) et affichage du HUD de debug
 */
public class PerformanceMonitor {

    // --- Timers ---
    /** Timestamp de départ (nanosecondes) de chaque timer nommé actif. */
    private final Map<String, Long>   timerStart   = new LinkedHashMap<>();
    /** Durée (en ms) du dernier résultat de chaque timer nommé. */
    private final Map<String, Double> timerResults = new LinkedHashMap<>();

    // --- Compteurs ---
    /** Nombre de draw calls de la frame courante. */
    private int drawCalls     = 0;
    /** Nombre de triangles dessinés dans la frame courante. */
    private int triangleCount = 0;
    /** Nombre de VAO actifs. */
    private int activeVAOs    = 0;
    /** Nombre total de chunks. */
    private int chunkCount    = 0;
    /** Nombre de chunks visibles (après frustum culling). */
    private int chunkVisible  = 0;
    /** Dernière valeur de lumière de bloc obtenue par raycast. */
    private int blockLight    = 0;
    /** Dernière valeur de lumière du ciel obtenue par raycast. */
    private int skyLight      = 0;

    // --- Mémoire : Heap Java ---
    /** Mémoire heap utilisée en Mo. */
    private long heapUsedMB = 0;
    /** Mémoire heap maximale en Mo. */
    private long heapMaxMB  = 0;
    /** Mémoire heap committée (vue par l'OS) en Mo. */
    private long heapCommittedMB = 0;

    // --- Beans JMX ---
    /** Bean JMX utilisé pour lire l'usage mémoire de la JVM. */
    private final MemoryMXBean memBean;

    // --- Frame time ---
    /** Temps écoulé pour la dernière frame, en ms. */
    private double frameTimeMs = 0;
    /** Timestamp (nanosecondes) de départ de la frame courante. */
    private long   frameStart  = 0;

    // --- Affichage ---
    /** Lignes de texte du HUD de debug, mises à jour périodiquement. */
    private final String[] displayLines = new String[12];

    // --- Throttle HUD ---
    /** Intervalle (en frames) entre deux rafraîchissements du HUD. */
    private static final int HUD_UPDATE_INTERVAL = 10;
    /** Compteur de frames utilisé pour throttle le rafraîchissement du HUD. */
    private int hudFrameCounter = 0;

    // =====================
    //  CONSTRUCTEUR
    // =====================

    /**
     * Créer un nouveau moniteur de performance
     */
    public PerformanceMonitor() {
        memBean = ManagementFactory.getMemoryMXBean();
    }

    // =====================
    //  TIMERS
    // =====================

    /**
     * Démarre un timer nommé
     * @param name nom du timer
     */
    public void startTimer(String name) {
        timerStart.put(name, System.nanoTime());
    }

    /**
     * Arrête un timer nommé et enregistre sa durée
     * @param name nom du timer
     */
    public void stopTimer(String name) {
        Long start = timerStart.get(name);
        if (start != null) {
            timerResults.put(name, (System.nanoTime() - start) / 1_000_000.0);
        }
    }

    /**
     * Retourne la durée du dernier timer nommé
     * @param name nom du timer
     * @return double, durée en ms, ou 0.0 si le timer n'existe pas
     */
    public double getTimer(String name) {
        return timerResults.getOrDefault(name, 0.0);
    }

    // =====================
    //  FRAME
    // =====================

    /**
     * A appeler au début d'une frame
     */
    public void beginFrame() {
        this.frameStart = System.nanoTime();
    }

    /**
     * A appeler à la fin d'une frame, met à jour le temps de frame et réinitialise les compteurs
     */
    public void endFrame() {
        this.frameTimeMs = (System.nanoTime() - frameStart) / 1_000_000.0;
        if (hudFrameCounter % HUD_UPDATE_INTERVAL == 0) {
            sampleMemory();
        }
        drawCalls     = 0;
        triangleCount = 0;
    }

    // =====================
    //  MÉMOIRE
    // =====================

    /**
     * Échantillonne l'usage mémoire de la heap Java
     */
    private void sampleMemory() {
        // 1. Heap Java (gérée par la JVM, limitée par -Xmx)
        MemoryUsage heap = memBean.getHeapMemoryUsage();
        heapUsedMB = heap.getUsed() / (1024 * 1024);
        heapMaxMB  = heap.getMax()  / (1024 * 1024);
        heapCommittedMB = heap.getCommitted() / (1024 * 1024);
    }

    // =====================
    //  COMPTEURS
    // =====================

    /** Incrémente le nombre de draw calls de la frame courante. */
    public void incrementDrawCalls()            { drawCalls++; }
    /**
     * Ajoute des triangles au compteur de la frame courante
     * @param count nombre de triangles à ajouter
     */
    public void addTriangles(int count)         { triangleCount += count; }
    /**
     * Définis le nombre de VAO actifs
     * @param count nombre de VAO actifs
     */
    public void setActiveVAOs(int count)        { activeVAOs = count; }
    /** Incrémente le nombre total de chunks. */
    public void addChunkCount()                 { chunkCount++; }
    /** Réinitialise le compteur de chunks visibles. */
    public void resetChunkVisibleCount()        { chunkVisible = 0; }
    /** Incrémente le nombre de chunks visibles. */
    public void addChunkVisibleCount()          { chunkVisible++; }
    /**
     * Définis la dernière valeur de lumière de bloc obtenue par raycast
     * @param light valeur de lumière de bloc
     */
    public void setBlockLightRayCast(int light) { blockLight = light; }
    /**
     * Définis la dernière valeur de lumière du ciel obtenue par raycast
     * @param light valeur de lumière du ciel
     */
    public void setSkyLightRayCast(int light)   { skyLight = light; }

    // =====================
    //  GETTERS
    // =====================

    /**
     * Retourne le temps de la dernière frame
     * @return double, temps en ms
     */
    public double getFrameTimeMs()       { return frameTimeMs; }
    /**
     * Retourne le nombre de draw calls de la frame courante
     * @return int
     */
    public int    getDrawCalls()         { return drawCalls; }
    /**
     * Retourne le nombre de triangles dessinés dans la frame courante
     * @return int
     */
    public int    getTriangleCount()     { return triangleCount; }
    /**
     * Retourne le nombre de VAO actifs
     * @return int
     */
    public int    getActiveVAOs()        { return activeVAOs; }
    /**
     * Retourne la mémoire heap utilisée
     * @return long, en Mo
     */
    public long   getHeapUsedMB()        { return heapUsedMB; }
    /**
     * Retourne la mémoire heap maximale
     * @return long, en Mo
     */
    public long   getHeapMaxMB()         { return heapMaxMB; }
    /**
     * Retourne la dernière valeur de lumière de bloc obtenue par raycast
     * @return int
     */
    public int    getBlockLightRayCast() { return blockLight; }
    /**
     * Retourne la dernière valeur de lumière du ciel obtenue par raycast
     * @return int
     */
    public int    getSkyLightRayCast()   { return skyLight; }

    // =====================
    //  AFFICHAGE
    // =====================

    /**
     * Retourne les lignes de texte du HUD de debug, rafraîchies périodiquement
     * @return String[]
     */
    public String[] getDisplayLines() {
        if (hudFrameCounter % HUD_UPDATE_INTERVAL != 0) {
            hudFrameCounter++;
            return displayLines;
        }
        hudFrameCounter++;

        displayLines[0]  = String.format("Frame    : %.2f ms", frameTimeMs);
        displayLines[1]  = String.format("Rendu    : %.2f ms", getTimer("render"));
        displayLines[2]  = "Draw calls : " + drawCalls;
        displayLines[3]  = "Triangles  : " + triangleCount;
        displayLines[4]  = "VAOs actifs: " + activeVAOs;
        displayLines[5]  = "Chunks     : " + chunkCount;
        displayLines[6] = String.format("Heap used  : %d Mo / %d Mo", heapUsedMB, heapMaxMB);
        displayLines[7] = String.format("Heap commit: %d Mo  (vu par l'OS)", heapCommittedMB);
        displayLines[8] = buildMemBar(heapUsedMB, heapMaxMB);

        return displayLines;
    }

    /**
     * Construit une barre de progression textuelle pour l'usage mémoire
     * @param used mémoire utilisée
     * @param max mémoire maximale
     * @return String, la barre de progression formatée
     */
    private String buildMemBar(long used, long max) {
        if (max <= 0) return "Heap : N/A";
        int pct    = (int)((used * 100) / max);
        int filled = pct / 10;
        StringBuilder bar = new StringBuilder("Heap [");
        for (int i = 0; i < 10; i++) bar.append(i < filled ? "#" : "-");
        bar.append("] ").append(pct).append("%");
        return bar.toString();
    }
}