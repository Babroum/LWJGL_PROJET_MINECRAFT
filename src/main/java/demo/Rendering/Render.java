package demo.Rendering;

import java.nio.FloatBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import demo.Annotations.OpenGLThread;
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
import demo.Managers.ShadersManager;
import demo.Managers.TextureManager;
import demo.Managers.WorldManager;
import demo.Others.FileLoader;
import demo.Types.Color4f;
import demo.Managers.PlayersManager;

public class Render {
    
    public static SceneGraph sceneGraph = new SceneGraph(); 
    public static PerformanceMonitor monitor = new PerformanceMonitor();

    private static TextRender textRender;  

    private static double[] fpsHistory = new double[60];

    private static int fpsIndex;
    private static int frameCount;
    private static float displayedFps = 0.0f;

    // nombre de chunk que le ChunkThread peut mettre avant de s'arreter
    // le OpenGLThread va lui afficher ce qu'il y a dans l'array et des qu'il affiche 1 chunk il le retire 
    // du array ce qui permet au ChunkThread de rajouter un nouveau chunk dans l'array
    public static ArrayBlockingQueue<Chunk> chunkQueue = new ArrayBlockingQueue<>(512); // 512 ajout possibles max
    public static ArrayBlockingQueue<SubChunk> dirtySubChunks = new ArrayBlockingQueue<>(8192); // 512*16 modif possibles max
    public static ConcurrentLinkedQueue<Chunk> chunksToUnload = new ConcurrentLinkedQueue<>();
    public static ConcurrentLinkedQueue<SubChunk> subChunkMeshQueue = new ConcurrentLinkedQueue<>();

    private static FloatBuffer projBuffer = BufferUtils.createFloatBuffer(16);
    private static final Matrix4f projection = new Matrix4f();

    private static final String[] TEXTURE_FILES  = 
    {"null", "grass_block_top", "grass_block_side", "dirt", "stone", "stone_brick_wall", "cobblestone", "light_block", "bedrock", "sand", "gravel","glass",
     "oak_log_top_bottom", "oak_log_side", "oak_leaves", "oak_planks"
    };
    private static final String[] BLOCKS_TO_LOAD = 
    {"null", "air", "grass", "dirt", "stone", "stone_brick_wall", "cobblestone", "light_block", "bedrock", "sand", "gravel","glass",
     "oak_log", "oak_planks", "oak_leaves"
    };

    // pour du debug, à modif car à chier
    private static final int HUD_UPDATE_INTERVAL = 10;
    private static int hudFrameCounter = 0;
    private static String display1;
    private static String display2;

    private static final double TARGET_FPS        = 500;
    private static final double TARGET_FRAME_TIME = 1.0 / TARGET_FPS;
    private static double frameStartTime = 0;

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
            // initialise en mémoire l'image des caractères
            Render.textRender = new TextRender(FileLoader.loadFileFromClasspathBB("/Textures/font.png"), 20, 20, 19,
                                        " !\"#$%&\'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~");          
        }catch(Exception e){
            System.out.println(e);
        }   
    }

    /**
     * Gère le rendu de la fenêtre.
     * @param windowRef
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

            // nettoie le frame buffer
			GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);         

            // décharge les chunks hors range
            Chunk toRemove;
            while ((toRemove = Render.chunksToUnload.poll()) != null) {
                WorldManager.onChunkUnloaded(toRemove); // retire du unloadingChunks
                Render.sceneGraph.removeChunk(toRemove);
                if (ChunksManager.getChunkAt(toRemove.x, toRemove.z) != null) {
                    ChunksManager.removeChunk(
                        ChunksManager.floorDiv(toRemove.x, SubChunk.CHUNKSIZEXZ),
                        ChunksManager.floorDiv(toRemove.z, SubChunk.CHUNKSIZEXZ)
                    );
                }
            }

            // poll les chunks prêts et upload sur GPU
            Chunk chunk;
            while((chunk = Render.chunkQueue.poll()) != null){
                Render.sceneGraph.root.addChild(chunk);
            }

            // subChunks modifiés update le VBO existant
            SubChunk sub;
            while ((sub = dirtySubChunks.poll()) != null) {
                sub.setupSubChunkVBOVAO();
            }

            LightManager.flushDirtySubChunks();
            InputManager.runOpenGL();
            PlayersManager.player.playerLoopRender();

            ShadersManager.useShader();            
            // donne les matrices de camera et perspective au shader
            ShadersManager.runProjection(Render.calculateProjection(45.0f, (float) Window.width / Window.height, 0.1f, 5000.f)); // donne la perspective de l'ecran
            ShadersManager.runView(PlayersManager.player.getCamera().getViewMatrixBuffer()); // donne la vue de la camera
            FrustumCulling.recalculateMatrix();

            // enregistre le temps de rendu
            monitor.startTimer("render");
            // Dessine la scène
            Render.drawScene();     
            monitor.stopTimer("render");

            // récupère le nombre de vbo
            monitor.setActiveVAOs(BuffersManager.activeVAOCount);
            
            // Affiche les fps            
            Render.calculateFPS(delta);

            // crosshair simple
            Render.getTextRender().drawText("+", Window.width / 2, Window.height / 2, 30, Color4f.BLACK);

            // Affiche la pos du joueur
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

            // TESTE A UTILISE POUR VOIR SI IL Y A ACCUMULATION DE RAM (FUITE DE MEMOIRE)
            /*
            if(frameCount % 600 == 0){ // toutes les 600 frames (~10sec à 60fps)
                System.gc();
                System.out.println("RAM après GC: " + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024*1024) + " MB");
            }*/

            AudioManager.cleanupTempSources();
            
            // Attend que le temps de frame cible soit écoulé
            double elapsed = GLFW.glfwGetTime() - frameStartTime;
            double remaining = TARGET_FRAME_TIME - elapsed;

            if (remaining > 0.002) {
                // Pour les longues attentes : sleep (libère le CPU)
                try { Thread.sleep((long)((remaining - 0.001) * 1000)); }
                catch (InterruptedException ignored) {}
            }
            // Pour la dernière milliseconde : spin-wait précis
            while (GLFW.glfwGetTime() - frameStartTime < TARGET_FRAME_TIME) {
                Thread.onSpinWait(); // hint au CPU, évite une boucle vide agressive
            }

            // Rends la fenêtre en buffer dessinée visible à l’écran
            GLFW.glfwSwapBuffers(Window.window);		

            GLFW.glfwPollEvents();
            monitor.endFrame();   
		}
        
        // ferme le fenêtre quand la boucle de rendu est fini
        GameLoop.stopGameThread();
        Window.exitWindow();
    }

    public static void setChunkQueue(ArrayBlockingQueue<Chunk> queue){
        Render.chunkQueue = queue;
    }
    public static void setDirtyChunkQueue(ArrayBlockingQueue<SubChunk> queue){
        Render.dirtySubChunks = queue;
    }

    /**
     * Retourne le textRender
     * @return TextRender
     */
    public static TextRender getTextRender(){
        return Render.textRender;
    }

    /**
     * Retourne la matrice de projection
     * @return
     */
    public static Matrix4f getProjection(){
        return Render.projection;
    }
    
    /**
     * Initialise la 3D avant la boucle de Rendu
     * @param width int
     * @param height int
     */
    @OpenGLThread
    private static void initialize3D(final int width, final int height)
    {
        // ** Réglage de la perspective  ***
        // On fixe le rectangle de la fenêtre où la 3D sera dessinée.
        // Ici on veut dessiner sur toute la fenêtre
        // Sur le "frustum", c’est la zone proche de l’utilisateur
        // S’appelle aussi ’view port’
        GL11.glViewport(0, 0, width, height);
        // On veut une projection normalisée
        GL11.glEnable(GL11.GL_NORMALIZE);

        // Au départ on met un écran rouge
        GL11.glClearColor(1f, 0f, 0f, 1f);
        // On active le test de profondeur
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        // Active les face visible que d’un côté (Pour éviter de dessiner des choses qui seront toujours cachées)
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glCullFace(GL11.GL_FRONT);  

        // Créer les shaders
        ShadersManager.createShader();   
        ShadersManager.createBlockBorderShader();
         
        // récupère les ids des uniformes dans les .glsl
        // pour ne pas à avoir à les re recup à chaque frame
        ShadersManager.getUniformLocations();    
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
     * @param delta double, temps en nano seconde entre 2 frames
     * @param window Window
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
            Render.getTextRender().drawText("AVG FPS : " + String.format("%.0f", Render.displayedFps) , 0, 0, 20, Color4f.RED);
            Render.frameCount++;
        }        
    }

    // calcule la matrice de projection et la retourne en floatbuffer pour les shaders
    public static FloatBuffer calculateProjection(float fov, float aspect, float near, float far) {
        Render.projBuffer.clear();
        Render.projection.setPerspective((float) Math.toRadians(fov), aspect, near, far);
        Render.projection.get(Render.projBuffer);
        return Render.projBuffer;
    }
}
