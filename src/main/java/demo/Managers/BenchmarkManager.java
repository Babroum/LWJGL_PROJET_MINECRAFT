package demo.Managers;

import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import demo.Events.KeyboardInput;

/**
 * Collecte en temps réel les performances du moteur voxel sur trois axes :
 * <ul>
 *   <li><b>FPS</b>         — frames par seconde (fourni par la boucle de rendu).</li>
 *   <li><b>RAM</b>         — heap utilisée en Mo (JVM heap).</li>
 *   <li><b>Chunks/s</b>    — nombre de chunks générés par seconde.</li>
 * </ul>
 *
 * <h2>Intégration rapide</h2>
 * <pre>
 * // 1. Dans votre classe principale (ou Game) — une seule instance :
 * BenchmarkManager bench = new BenchmarkManager();
 * bench.start();
 *
 * // 2. Dans votre boucle OpenGL, à chaque frame :
 * bench.recordFrame();
 *
 * // 3. Dans votre ChunkThread, après chaque generateChunk() :
 * bench.recordChunkGenerated();
 *
 * // 4. Pour arrêter et exporter le rapport :
 * bench.stop();   // génère benchmark_report.html dans le répertoire courant
 * </pre>
 */
public class BenchmarkManager {

    // ─────────────────────────────────────────────────────────────────
    //  Compteurs thread-safe (écrits depuis plusieurs threads)
    // ─────────────────────────────────────────────────────────────────

    /** Nombre de frames rendues depuis le dernier snapshot. */
    private final AtomicInteger frameCounter    = new AtomicInteger(0);

    /** Nombre de chunks générés depuis le dernier snapshot. */
    private final AtomicInteger chunkCounter    = new AtomicInteger(0);

    /** Timestamp (ns) du début de la session. */
    private final AtomicLong    sessionStart    = new AtomicLong(0);

    // ─────────────────────────────────────────────────────────────────
    //  Données collectées (un point par seconde)
    // ─────────────────────────────────────────────────────────────────

    /** Labels de temps en secondes (axe X des graphiques). */
    private final List<Integer> timeLabels  = new ArrayList<>();

    /** Courbe FPS. */
    private final List<Integer> fpsData     = new ArrayList<>();

    /** Courbe RAM en Mo. */
    private final List<Long>    ramData     = new ArrayList<>();

    /** Courbe chunks générés par seconde. */
    private final List<Integer> chunksData  = new ArrayList<>();

    // ─────────────────────────────────────────────────────────────────
    //  Thread de collecte
    // ─────────────────────────────────────────────────────────────────

    /** Thread dédié à la collecte périodique des métriques. */
    private Thread      collectorThread;
    /** Vrai tant que la collecte est active. */
    private volatile boolean running = false;

    /** Accès aux stats mémoire JVM. */
    private final MemoryMXBean memBean = ManagementFactory.getMemoryMXBean();

    // ─────────────────────────────────────────────────────────────────
    //  API publique
    // ─────────────────────────────────────────────────────────────────

    /** Constructeur implicite vide */
    public BenchmarkManager(){}

    // singleton
    /**
     * Instance unique (singleton) du BenchmarkManager.
     */
    private static BenchmarkManager instance;
    /**
     * Retourne l'instance unique du BenchmarkManager, la créant si besoin.
     * @return l'instance singleton
     */
    public static BenchmarkManager getInstance() {
        if (instance == null) instance = new BenchmarkManager();
        return instance;
    }

    /**
     * Démarre la collecte. Appeler une seule fois au lancement du jeu.
     */
    public void start() {
        running = true;
        sessionStart.set(System.nanoTime());

        collectorThread = new Thread(this::collectorLoop, "BenchmarkCollector");
        collectorThread.setDaemon(true); // le thread ne bloque pas la fermeture de la JVM
        collectorThread.start();
    }

    /**
     * Stoppe la collecte et génère le rapport HTML.
     * Appeler depuis n'importe quel thread à la fin de la session.
     */
    public void stop() {
        running = false;
        if (collectorThread != null) collectorThread.interrupt();
        exportHTML("benchmark_report.html");
    }

    /**
     * À appeler depuis la boucle de rendu OpenGL, une fois par frame.
     * Thread-safe (CAS atomique).
     */
    public void recordFrame() {
        frameCounter.incrementAndGet();
    }

    /**
     * À appeler dans ChunkThread après chaque {@code generateChunk()}.
     * Thread-safe (CAS atomique).
     */
    public void recordChunkGenerated() {
        chunkCounter.incrementAndGet();
    }

    // ─────────────────────────────────────────────────────────────────
    //  Boucle de collecte interne (1 snapshot / seconde)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Boucle interne exécutée dans le thread de collecte : prend un instantané des compteurs
     * (FPS, chunks générés, RAM) une fois par seconde et l'enregistre.
     */
    private void collectorLoop() {
        int elapsed = 0;

        while (running) {
            try {
                Thread.sleep(1000); // attend exactement 1 seconde
            } catch (InterruptedException e) {
                break; // stop() a interrompu le thread
            }

            elapsed++;

            // Récupère et remet à zéro les compteurs atomiques
            int fps    = frameCounter.getAndSet(0);
            int chunks = chunkCounter.getAndSet(0);

            // RAM heap utilisée en Mo
            long ramMB = memBean.getHeapMemoryUsage().getUsed() / (1024 * 1024);

            // Enregistrement du point
            timeLabels.add(elapsed);
            fpsData.add(fps);
            ramData.add(ramMB);
            chunksData.add(chunks);

            // Log console en temps réel
            if (KeyboardInput.isBenchMarkShow){
                System.out.printf("[BENCH] t=%3ds | FPS: %4d | RAM: %4d Mo | Chunks/s: %3d%n", elapsed, fps, ramMB, chunks);
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  Export HTML avec Chart.js
    // ─────────────────────────────────────────────────────────────────

    /**
     * Génère un fichier HTML autonome contenant trois graphiques en courbes
     * (FPS, RAM, Chunks/s) via Chart.js (CDN).
     *
     * @param filename Nom du fichier de sortie (ex. "benchmark_report.html").
     */
    private void exportHTML(String filename) {
        StringBuilder sb = new StringBuilder();

        // Sérialise les listes Java en tableaux JavaScript
        String labels     = toJsArray(timeLabels);
        String fpsJs      = toJsArray(fpsData);
        String ramJs      = toJsArray(ramData);
        String chunksJs   = toJsArray(chunksData);

        // Calcule quelques stats résumées
        int    maxFps     = fpsData.stream().mapToInt(i -> i).max().orElse(0);
        int    minFps     = fpsData.stream().mapToInt(i -> i).min().orElse(0);
        double avgFps     = fpsData.stream().mapToInt(i -> i).average().orElse(0);
        long   maxRam     = ramData.stream().mapToLong(i -> i).max().orElse(0);
        int    totalChunks= chunksData.stream().mapToInt(i -> i).sum();

        sb.append("<!DOCTYPE html><html lang='fr'><head><meta charset='UTF-8'>")
          .append("<title>Benchmark Voxel Engine</title>")
          .append("<script src='https://cdn.jsdelivr.net/npm/chart.js'></script>")
          .append("<style>")
          .append("body{font-family:monospace;background:#1a1a2e;color:#e0e0e0;margin:0;padding:20px;}")
          .append("h1{color:#00d4ff;text-align:center;letter-spacing:2px;}")
          .append("h2{color:#a0a0c0;font-size:0.85em;text-align:center;margin-top:-10px;}")
          .append(".stats{display:flex;justify-content:center;gap:30px;margin:20px 0;flex-wrap:wrap;}")
          .append(".stat{background:#16213e;border:1px solid #00d4ff33;border-radius:8px;padding:12px 24px;text-align:center;}")
          .append(".stat .val{font-size:1.8em;color:#00d4ff;font-weight:bold;}")
          .append(".stat .lbl{font-size:0.75em;color:#8080a0;margin-top:4px;}")
          .append(".charts{display:flex;flex-direction:column;gap:30px;max-width:1000px;margin:0 auto;}")
          .append(".chart-box{background:#16213e;border-radius:10px;padding:20px;border:1px solid #ffffff11;}")
          .append(".chart-title{color:#c0c0e0;font-size:0.9em;margin-bottom:12px;text-transform:uppercase;letter-spacing:1px;}")
          .append("canvas{width:100%!important;}")
          .append("</style></head><body>")

          // En-tête
          .append("<h1>⚡ VOXEL ENGINE — BENCHMARK REPORT</h1>")
          .append("<h2>Durée totale : ").append(timeLabels.isEmpty() ? 0 : timeLabels.get(timeLabels.size()-1)).append(" secondes</h2>")

          // Cartes de stats
          .append("<div class='stats'>")
          .append(statCard(String.valueOf(maxFps),    "FPS MAX"))
          .append(statCard(String.valueOf(minFps),    "FPS MIN"))
          .append(String.format(statCard("%.1f", "FPS MOYEN"), avgFps))
          .append(statCard(maxRam + " Mo",            "RAM PEAK"))
          .append(statCard(String.valueOf(totalChunks),"CHUNKS TOTAL"))
          .append("</div>")

          // Graphiques
          .append("<div class='charts'>")
          .append(chartBox("fps-chart",    "🎮 FPS (frames / seconde)",       "#00d4ff"))
          .append(chartBox("ram-chart",    "🧠 RAM utilisée (Mo)",             "#ff6b6b"))
          .append(chartBox("chunk-chart",  "📦 Chunks générés (/ seconde)",    "#51cf66"))
          .append("</div>")

          // Scripts Chart.js
          .append("<script>")
          .append("const labels=").append(labels).append(";")
          .append("function makeChart(id,label,data,color){")
          .append("  new Chart(document.getElementById(id),{")
          .append("    type:'line',")
          .append("    data:{labels,datasets:[{label,data,borderColor:color,backgroundColor:color+'22',")
          .append("      borderWidth:2,pointRadius:0,tension:0.3,fill:true}]},")
          .append("    options:{animation:false,responsive:true,plugins:{legend:{display:false}},")
          .append("      scales:{x:{ticks:{color:'#8080a0'},grid:{color:'#ffffff11'},title:{display:true,text:'Temps (s)',color:'#8080a0'}},")
          .append("              y:{ticks:{color:'#8080a0'},grid:{color:'#ffffff15'}}}}")
          .append("  });}")
          .append("makeChart('fps-chart','FPS',").append(fpsJs).append(",'#00d4ff');")
          .append("makeChart('ram-chart','RAM (Mo)',").append(ramJs).append(",'#ff6b6b');")
          .append("makeChart('chunk-chart','Chunks/s',").append(chunksJs).append(",'#51cf66');")
          .append("</script>")
          .append("</body></html>");

        // Écriture du fichier
        try (FileWriter fw = new FileWriter(filename)) {
            fw.write(sb.toString());
        } catch (IOException e) {
            System.err.println("[BENCH] Erreur lors de l'export : " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  Helpers HTML
    // ─────────────────────────────────────────────────────────────────

    /** Génère le HTML d'une carte de statistique résumée. */
    private String statCard(String val, String label) {
        return "<div class='stat'><div class='val'>" + val + "</div><div class='lbl'>" + label + "</div></div>";
    }

    /** Génère le HTML d'un conteneur de graphique (canvas). */
    private String chartBox(String id, String title, String color) {
        return "<div class='chart-box'><div class='chart-title'>" + title + "</div><canvas id='" + id + "'></canvas></div>";
    }

    /** Convertit une List<? extends Number> en tableau littéral JavaScript. */
    private <T extends Number> String toJsArray(List<T> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(list.get(i));
        }
        return sb.append(']').toString();
    }
}