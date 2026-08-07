package demo.Rendering;

import java.nio.FloatBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

import demo.Annotations.OpenGLThread;
import demo.Events.KeyboardInput;
import demo.Game.GameLoop;
import demo.Geometry.Chunk;
import demo.Geometry.SubChunk;
import demo.Managers.AudioManager;
import demo.Managers.BenchmarkManager;
import demo.Managers.BlocksManager;
import demo.Managers.BuffersManager;
import demo.Managers.ChunksManager;
import demo.Managers.InputManager;
import demo.Managers.LightManager;
import demo.Managers.LiquidManager;
import demo.Managers.ShadersManager;
import demo.Managers.FrameBufferManager;
import demo.Managers.TextureManager;
import demo.Managers.WorldManager;
import demo.Others.ChunkSerializer;
import demo.Others.FileLoader;
import demo.Types.Color4f;
import demo.Managers.PlayersManager;

/**
 * Gère la boucle de rendu OpenGL, l'initialisation du renderer et les files d'attente entre les threads de génération et le thread OpenGL
 */
public class Render {
    
    /** Racine de la scène 3D. */
    public static SceneGraph sceneGraph = new SceneGraph(); 
    /** Moniteur de performance global. */
    public static PerformanceMonitor monitor = new PerformanceMonitor();

    /** Historique des FPS pour calculer une moyenne glissante. */
    private static double[] fpsHistory = new double[60];

    /** Index courant dans {@link #fpsHistory}. */
    private static int fpsIndex;
    /** Compteur de frames total. */
    private static int frameCount;
    /** FPS moyen affiché. */
    private static float displayedFps = 0.0f;

    // nombre de chunk que le ChunkThread peut mettre avant de s'arreter
    // le OpenGLThread va lui afficher ce qu'il y a dans l'array et des qu'il affiche 1 chunk il le retire 
    // du array ce qui permet au ChunkThread de rajouter un nouveau chunk dans l'array
    /** File des chunks prêts (tous les subchunks maillés) à ajouter à la scène, consommée par le thread OpenGL. */
    public static ArrayBlockingQueue<Chunk> chunkQueue = new ArrayBlockingQueue<>(512); // 512 ajout possibles max
    /** File des subchunks dont le mesh a été (re)généré et qui doivent être uploadés en VBO/VAO. */
    public static ArrayBlockingQueue<SubChunk> dirtySubChunks = new ArrayBlockingQueue<>(8192); // 512*16 modif possibles max
    /** File des chunks à décharger (hors de la distance de rendu). */
    public static ConcurrentLinkedQueue<Chunk> chunksToUnload = new ConcurrentLinkedQueue<>();
    /** File des subchunks en attente de maillage (mesh generation). */
    public static ConcurrentLinkedQueue<SubChunk> subChunkMeshQueue = new ConcurrentLinkedQueue<>();

    /** Buffer réutilisé pour envoyer la matrice de projection à OpenGL. */
    private static FloatBuffer projBuffer = BufferUtils.createFloatBuffer(16);
    /** Matrice de projection courante. */
    private static final Matrix4f projection = new Matrix4f();

    /** Liste des fichiers de texture à charger au démarrage. */
    private static final String[] TEXTURE_FILES  = 
    {"null", "grass_block_top", "grass_block_side", "dirt", "stone", "stone_brick_wall", "cobblestone", "light_block", "bedrock", "sand", "gravel", "glass",
     "oak_log_top_bottom", "oak_log_side", "oak_leaves", "oak_planks", "water", "torch_block_side", "torch_block_top"
    };
    /** Liste des blocs à charger au démarrage. */
    private static final String[] BLOCKS_TO_LOAD = 
    {"null", "air", "grass", "dirt", "stone", "stone_brick_wall", "cobblestone", "light_block", "bedrock", "sand", "gravel", "glass",
     "oak_log", "oak_planks", "oak_leaves", "water", "torch"
    };

    /** FPS cible pour limiter la boucle de rendu. */
    private static final double TARGET_FPS        = 500;
    /** Temps de frame cible correspondant à {@link #TARGET_FPS}. */
    private static final double TARGET_FRAME_TIME = 1.0 / TARGET_FPS;
    /** Timestamp de début de la frame courante. */
    private static double frameStartTime = 0;

    /** Constructeur implicite vide */
    public Render(){}

    /**
     * Est appelé avant le début du draw scene 
     */
    @OpenGLThread
    public static void setupBeforeRendering(){
        
        BlockBorderRenderer.init();
        // initialise en mémoire les textures des caractères
        try{           
            TextureManager.init(TEXTURE_FILES);
            BlocksManager.init(BLOCKS_TO_LOAD);

            AudioManager.loadSound("bird", "/Audios/bird.ogg");
            GuiRenderer.loadAtlasInMemory();       
        }catch(Exception e){
            e.printStackTrace();
        }   
    }

    /**
     * Gère le rendu de la fenêtre.
     */
    @OpenGLThread
    public static void rendering(){     
        double lastTime = GLFW.glfwGetTime(); 

        // Prépare la 3D
        Render.initialize3D(Window.width, Window.height);
        sceneGraph.setBackGroundColor(Color4f.SKY);

        GameLoop.init();
        GameLoop.startGameThread();

        while ( !GLFW.glfwWindowShouldClose(Window.window) ) {  
            BenchmarkManager.getInstance().recordFrame();
            // démarre l'enregistrement de performances pour la frame
            frameStartTime = GLFW.glfwGetTime();
            monitor.beginFrame();
            double now = frameStartTime;

            double delta = now - lastTime;
            lastTime = now;
            // Protection contre les gros sauts
            if (delta > 0.1) delta = 0.1; // 10FPS

            // décharge les chunks hors range
        Chunk toRemove;
        while ((toRemove = Render.chunksToUnload.poll()) != null) {
            ChunkSerializer.save(toRemove);
            WorldManager.onChunkUnloaded(toRemove);
            Render.sceneGraph.removeChunk(toRemove);
            if (ChunksManager.getChunkAt(toRemove.x, toRemove.z) != null) {
                ChunksManager.removeChunk(toRemove.x, toRemove.z);
            }
        }

        Chunk chunk;
        while((chunk = Render.chunkQueue.poll()) != null){
            Render.sceneGraph.root.addChild(chunk);
        }

        SubChunk sub;
        while ((sub = dirtySubChunks.poll()) != null) {
            sub.setupSubChunkVBOVAO();
        }

        LightManager.flushDirtySubChunks();
        LiquidManager.flushDirtySubChunks();
        FrustumCulling.recalculateMatrix();        
        InputManager.runOpenGL();
        PlayersManager.player.playerLoopRender();

        // === PASSE 1 : dessiner la scène 3D dans le FBO ===
        FrameBufferManager.bindForWriting();
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);

        monitor.startTimer("render");
        Render.drawScene();     
        monitor.stopTimer("render");

        monitor.setActiveVAOs(BuffersManager.activeVAOCount);

        // === PASSE 2 : afficher le FBO à l'écran ===
        FrameBufferManager.unbind();
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);

        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);

        ShadersManager.usePostProcessShader();
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, FrameBufferManager.getColorTexture());
        ShadersManager.runScreenTex(0);
        ShadersManager.runSaturationAmountPostProcess(1.5f);

        // force FILL le temps de dessiner le quad, peu importe l'état wireframe voulu
        GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);

        FrameBufferManager.drawQuad();

        if (KeyboardInput.isWireFrameMode) {
            GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE);
        }

        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_DEPTH_TEST);

        // === PASSE 3 : dessiner le texte/HUD par-dessus, hors FBO ===
        GuiRenderer.beginFrame();

        GuiRenderer.drawImage("blue.button.rectangle.pressed", 50, 64);
        GuiRenderer.drawImage("blue.button.rectangle.hover", 50, 138);
        GuiRenderer.drawImage("blue.button.rectangle.normal", 50, 212);

        GuiRenderer.endFrame();
        /*
        ShadersManager.useTextShader();
        FloatBuffer buffer = MemoryUtil.memAllocFloat(16);
        ShadersManager.runTextOrtho(buffer, Window.width, Window.height);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        Render.calculateFPS(delta);

        Render.getTextRender().drawText("+", Window.width / 2, Window.height / 2, 30, Color4f.BLACK);

        if (Render.hudFrameCounter % HUD_UPDATE_INTERVAL == 0) {
            Render.hudFrameCounter++;
            Vector3f pos = PlayersManager.player.getPosition();
            Render.display1 = String.format("Position: (%.2f, %.2f, %.2f)%n", pos.x, pos.y - 1.5f, pos.z);
            Render.display2 = String.format("Sky Light: %d , Block Light: %d %n", Render.monitor.getSkyLightRayCast(), Render.monitor.getBlockLightRayCast());                
        }
        Render.getTextRender().drawText(Render.display1, Window.width / 2,20, 15, Color4f.RED); 
        Render.getTextRender().drawText(Render.display2, Window.width / 2,40, 15, Color4f.RED); 
        Render.hudFrameCounter++;

        String[] lines = monitor.getDisplayLines();
        for(int i = 0; i < lines.length; i++){
            if(lines[i] != null){
                Render.getTextRender().drawText(lines[i], 0, 20 + i * 20, 15, Color4f.PURPLE);
            }
        }    

        GL11.glEnable(GL11.GL_DEPTH_TEST);        
        MemoryUtil.memFree(buffer);
        */
        ShadersManager.useShader();    

        AudioManager.cleanupTempSources();
        
        double elapsed = GLFW.glfwGetTime() - frameStartTime;
        double remaining = TARGET_FRAME_TIME - elapsed;

        if (remaining > 0.002) {
            try { Thread.sleep((long)((remaining - 0.001) * 1000)); }
            catch (InterruptedException ignored) {}
        }
        while (GLFW.glfwGetTime() - frameStartTime < TARGET_FRAME_TIME) {
            Thread.onSpinWait();
        }

        GLFW.glfwSwapBuffers(Window.window);		
        GLFW.glfwPollEvents();
        monitor.endFrame(); 
		}
        
        // ferme le fenêtre quand la boucle de rendu est fini
        GameLoop.stopGameThread();
        FrameBufferManager.close();
        Window.exitWindow();
    }

    /**
     * Remplace la file des chunks prêts à ajouter à la scène
     * @param queue nouvelle file de chunks
     */
    public static void setChunkQueue(ArrayBlockingQueue<Chunk> queue){
        Render.chunkQueue = queue;
    }
    /**
     * Remplace la file des subchunks à uploader en VBO/VAO
     * @param queue nouvelle file de subchunks
     */
    public static void setDirtyChunkQueue(ArrayBlockingQueue<SubChunk> queue){
        Render.dirtySubChunks = queue;
    }

    /**
     * Retourne la matrice de projection
     * @return Matrix4f
     */
    public static Matrix4f getProjection(){
        return Render.projection;
    }
    
    /**
     * Initialise la 3D avant la boucle de Rendu
     * @param width int, largeur de la fenêtre
     * @param height int, hauteur de la fenêtre
     */
    @OpenGLThread
    private static void initialize3D(final int width, final int height)
    {
        GL11.glViewport(0, 0, width, height);
        GL11.glEnable(GL11.GL_NORMALIZE);
        GL11.glClearColor(1f, 0f, 0f, 1f);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glCullFace(GL11.GL_FRONT);  

        ShadersManager.createShader();   
        ShadersManager.createBlockBorderShader();
        ShadersManager.createPostProcessShader();
        ShadersManager.createLiquidShader();

        ShadersManager.getUniformLocations();      
        
        int renderDistanceBlocks = WorldManager.RENDER_DISTANCE * 16;
        float fogEnd = renderDistanceBlocks * 0.9f;
        float fogStart = fogEnd * 0.2f;

        ShadersManager.useLiquidShader();
        ShadersManager.runFogStartLiquid(fogStart);
        ShadersManager.runFogEndLiquid(fogEnd);
        ShadersManager.runSkyColorLiquid(Color4f.SKY.red, Color4f.SKY.green, Color4f.SKY.blue);
        ShadersManager.runBlockLightColorLiquid(1.0f, 0.75f, 0.5f);

        ShadersManager.useShader();
        ShadersManager.runFogStart(fogStart);
        ShadersManager.runFogEnd(fogEnd);
        ShadersManager.runSkyColor(Color4f.SKY.red, Color4f.SKY.green, Color4f.SKY.blue);
        ShadersManager.runBlockLightColor(1.0f, 0.75f, 0.5f);

        FrameBufferManager.create(width, height);
    }

    /**
     * Dessine la scène 
     */
    @OpenGLThread
    private static void drawScene(){
        Render.sceneGraph.drawScene();
    }

    /**
     * Calcule les fps 
     * @param delta double, temps en secondes entre 2 frames
     */
    @OpenGLThread
    private static void calculateFPS(double delta){
        if (delta > 0){
            Render.fpsHistory[Render.fpsIndex] = 1.0 / delta;
            Render.fpsIndex = (Render.fpsIndex + 1) % Render.fpsHistory.length;
            if (Render.frameCount % 30 == 0){
                double sum = 0;
                for (double fps : fpsHistory){
                    sum += fps;
                }
                Render.displayedFps = (float)(sum / Render.fpsHistory.length);
            }
            //Render.getTextRender().drawText("AVG FPS : " + String.format("%.0f", Render.displayedFps) , 0, 0, 20, Color4f.RED);
            Render.frameCount++;
        }        
    }

    /**
     * Calcule la matrice de projection et la retourne en FloatBuffer pour les shaders
     * @param fov float, champ de vision en degrés
     * @param aspect float, ratio largeur/hauteur
     * @param near float, plan proche
     * @param far float, plan éloigné
     * @return FloatBuffer, la matrice de projection
     */
    public static FloatBuffer calculateProjection(float fov, float aspect, float near, float far) {
        Render.projBuffer.clear();
        Render.projection.setPerspective((float) Math.toRadians(fov), aspect, near, far);
        Render.projection.get(Render.projBuffer);
        return Render.projBuffer;
    }
}