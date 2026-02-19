package demo.Rendering;

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
    private final Map<String, Long> timerStart = new LinkedHashMap<>();
    private final Map<String, Double> timerResults = new LinkedHashMap<>();

    private final String[] displayLines = new String[8]; // variable d'instance

    // --- Compteurs ---
    private int drawCalls = 0;
    private int triangleCount = 0;
    private int faceCulledCount = 0;
    private int activeVAOs = 0;

    // --- Mémoire ---
    private long usedMemoryMB = 0;

    // --- Frame time ---
    private double frameTimeMs = 0;
    private long frameStart = 0;

    // =====================
    //  TIMERS
    // =====================

    public void startTimer(String name) {
        timerStart.put(name, System.nanoTime());
    }

    public void stopTimer(String name) {
        Long start = timerStart.get(name);
        if (start != null) {
            double ms = (System.nanoTime() - start) / 1_000_000.0;
            timerResults.put(name, ms);
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

        // Mémoire
        Runtime rt = Runtime.getRuntime();
        usedMemoryMB = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);

        // Reset compteurs par frame
        drawCalls = 0;
        triangleCount = 0;
        faceCulledCount = 0;
    }

    // =====================
    //  COMPTEURS
    // =====================

    public void incrementDrawCalls() { drawCalls++; }
    public void addTriangles(int count) { triangleCount += count; }
    public void incrementFaceCulled() { faceCulledCount++; }
    public void setActiveVAOs(int count) { activeVAOs = count; }

    // =====================
    //  GETTERS
    // =====================

    public double getFrameTimeMs()  { return frameTimeMs; }
    public int getDrawCalls()       { return drawCalls; }
    public int getTriangleCount()   { return triangleCount; }
    public int getFaceCulledCount() { return faceCulledCount; }
    public int getActiveVAOs()      { return activeVAOs; }
    public long getUsedMemoryMB()   { return usedMemoryMB; }

    // =====================
    //  AFFICHAGE
    // =====================

    public String[] getDisplayLines() {

        this.displayLines[0] = "Frame time: "  + frameTimeMs + " ms";
        this.displayLines[1] = "Draw calls: "  + drawCalls;
        this.displayLines[2] = "Triangles: "   + triangleCount;
        this.displayLines[3] = "Faces culled: "+ faceCulledCount;
        this.displayLines[4] = "VAOs actifs: " + activeVAOs;
        this.displayLines[5] = "RAM: "         + usedMemoryMB + " MB";
        this.displayLines[6] = "Logique: "     + getTimer("logic")  + " ms";
        this.displayLines[7] = "Rendu: "       + getTimer("render") + " ms";
        return this.displayLines;
    }
}