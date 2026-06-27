package demo.Rendering;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.LinkedHashMap;
import java.util.Map;

public class PerformanceMonitor {

    // --- Timers ---
    private final Map<String, Long>   timerStart   = new LinkedHashMap<>();
    private final Map<String, Double> timerResults = new LinkedHashMap<>();

    // --- Compteurs ---
    private int drawCalls     = 0;
    private int triangleCount = 0;
    private int activeVAOs    = 0;
    private int chunkCount    = 0;
    private int chunkVisible  = 0;
    private int blockLight    = 0;
    private int skyLight      = 0;

    // --- Mémoire : Heap Java ---
    private long heapUsedMB = 0;
    private long heapMaxMB  = 0;
    private long heapCommittedMB = 0;

    // --- Beans JMX ---
    private final MemoryMXBean memBean;

    // --- Frame time ---
    private double frameTimeMs = 0;
    private long   frameStart  = 0;

    // --- Affichage ---
    private final String[] displayLines = new String[12];

    // --- Throttle HUD ---
    private static final int HUD_UPDATE_INTERVAL = 10;
    private int hudFrameCounter = 0;

    // =====================
    //  CONSTRUCTEUR
    // =====================

    public PerformanceMonitor() {
        memBean = ManagementFactory.getMemoryMXBean();
    }

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
        if (hudFrameCounter % HUD_UPDATE_INTERVAL == 0) {
            sampleMemory();
        }
        drawCalls     = 0;
        triangleCount = 0;
    }

    // =====================
    //  MÉMOIRE
    // =====================

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

    public void incrementDrawCalls()            { drawCalls++; }
    public void addTriangles(int count)         { triangleCount += count; }
    public void setActiveVAOs(int count)        { activeVAOs = count; }
    public void addChunkCount()                 { chunkCount++; }
    public void resetChunkVisibleCount()        { chunkVisible = 0; }
    public void addChunkVisibleCount()          { chunkVisible++; }
    public void setBlockLightRayCast(int light) { blockLight = light; }
    public void setSkyLightRayCast(int light)   { skyLight = light; }

    // =====================
    //  GETTERS
    // =====================

    public double getFrameTimeMs()       { return frameTimeMs; }
    public int    getDrawCalls()         { return drawCalls; }
    public int    getTriangleCount()     { return triangleCount; }
    public int    getActiveVAOs()        { return activeVAOs; }
    public long   getHeapUsedMB()        { return heapUsedMB; }
    public long   getHeapMaxMB()         { return heapMaxMB; }
    public int    getBlockLightRayCast() { return blockLight; }
    public int    getSkyLightRayCast()   { return skyLight; }

    // =====================
    //  AFFICHAGE
    // =====================

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