package demo.Rendering;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.LinkedHashMap;
import java.util.Map;

/*
FAIT PAR CLAUDE CAR J'AI LA FLEMME DE LE FAIRE VOILA
*/

/**
 * Moniteur de performances du jeu.
 * Mesure les temps de rendu, logique, et statistiques globales.
 */
public class PerformanceMonitor {

    // --- Timers ---
    private final Map<String, Long>   timerStart   = new LinkedHashMap<>();
    private final Map<String, Double> timerResults = new LinkedHashMap<>();

    // --- Compteurs ---
    private int drawCalls      = 0;
    private int triangleCount  = 0;
    private int activeVAOs     = 0;
    private int chunkCount     = 0;
    private int chunkVisible   = 0;


    private int blockLight     = 0;

    // --- Mémoire ---
    // Heap Java (gérée par la JVM, comptée dans -Xmx)
    private long heapUsedMB    = 0; // heap actuellement utilisée
    private long heapMaxMB     = 0; // heap max allouable (-Xmx)
    // Off-heap Java (direct buffers : ByteBuffer.allocateDirect, MemoryUtil.memAlloc...)
    private long offHeapUsedMB = 0;
    private long offHeapMaxMB  = 0;

    private final MemoryMXBean memBean = ManagementFactory.getMemoryMXBean();

    // --- Frame time ---
    private double frameTimeMs = 0;
    private long   frameStart  = 0;

    // --- Affichage (réutilisé chaque frame) ---
    private final String[] displayLines = new String[13];

    // =====================
    //  TIMERS
    // =====================

    public void startTimer(String name) {
        timerStart.put(name, System.nanoTime());
    }

    public void stopTimer(String name) {
        Long start = timerStart.get(name);
        if (start != null) {
            timerResults.put(name, (System.nanoTime() - start) / 1_000_000.0);
        }
    }

    public double getTimer(String name) {
        return timerResults.getOrDefault(name, 0.0);
    }

    // =====================
    //  FRAME
    // =====================

    public void beginFrame() {
        this.frameStart = System.nanoTime();
    }

    public void endFrame() {
        this.frameTimeMs = (System.nanoTime() - frameStart) / 1_000_000.0;
        sampleMemory();
        // Reset compteurs par frame
        drawCalls       = 0;
        triangleCount   = 0;
    }

    // =====================
    //  MÉMOIRE
    // =====================

    private void sampleMemory() {
        // 1. Heap Java
        MemoryUsage heap = memBean.getHeapMemoryUsage();
        heapUsedMB = heap.getUsed() / (1024 * 1024);
        heapMaxMB  = heap.getMax()  / (1024 * 1024);

        // 2. Off-heap
        MemoryUsage nonHeap = memBean.getNonHeapMemoryUsage();
        offHeapUsedMB = nonHeap.getUsed() / (1024 * 1024);
        offHeapMaxMB  = nonHeap.getMax() == -1 ? -1 : nonHeap.getMax() / (1024 * 1024);
    }

    // =====================
    //  COMPTEURS
    // =====================

    public void incrementDrawCalls()            { drawCalls++; }
    public void addTriangles(int count)         { triangleCount += count; }
    public void setActiveVAOs(int count)        { activeVAOs = count; }
    public void addChunkCount()                 { chunkCount++; }
    public void resetChunkVisibleCount()        { chunkVisible = 0; }
    public void addChunkVisibleCount()          { chunkVisible++; }
    public void setBlockLightRayCast(int light) { blockLight = light; }

    // =====================
    //  GETTERS
    // =====================

    public double getFrameTimeMs()       { return frameTimeMs; }
    public int    getDrawCalls()         { return drawCalls; }
    public int    getTriangleCount()     { return triangleCount; }
    public int    getActiveVAOs()        { return activeVAOs; }
    public long   getHeapUsedMB()        { return heapUsedMB; }
    public long   getHeapMaxMB()         { return heapMaxMB; }
    public long   getOffHeapUsedMB()     { return offHeapUsedMB; }
    public int    getBlockLightRayCast() { return blockLight; }

    // =====================
    //  AFFICHAGE
    // =====================

    public String[] getDisplayLines() {
        displayLines[0]  = String.format("Frame : %.2f ms", frameTimeMs);
        displayLines[1]  = String.format("Rendu : %.2f ms",
                                          getTimer("render"));
        displayLines[2]  = "Draw calls : "   + drawCalls;
        displayLines[3]  = "Triangles : "    + triangleCount;
        displayLines[4]  = "VAOs actifs : "  + activeVAOs;
        displayLines[5]  = "Chunks : "       + chunkCount;
        // RAM : les 3 métriques séparées pour diagnostiquer
        displayLines[6]  = String.format("Heap Java   : %d Mo / %d Mo", heapUsedMB, heapMaxMB);
        displayLines[7]  = String.format("Off-heap    : %d Mo%s",
                                          offHeapUsedMB,
                                          offHeapMaxMB == -1 ? " (illimite)" : " / " + offHeapMaxMB + " Mo");
        displayLines[8] = buildMemBar(heapUsedMB, heapMaxMB);
        displayLines[9] = "Chunks Visible : " + chunkVisible;
        displayLines[10] = null;
        return displayLines;
    }

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