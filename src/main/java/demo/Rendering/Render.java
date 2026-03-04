package demo.Rendering;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import demo.GameLoop;
import demo.Annotations.OpenGLThread;
import demo.Events.InputManager;
import demo.Geometry.Chunk;
import demo.Geometry.SubChunk;
import demo.Managers.AudioManager;
import demo.Managers.BuffersManager;
import demo.Managers.ShadersManager;
import demo.Managers.TexturesManager;
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

    // Valeur à changé aussi dans App.java
    public static ArrayBlockingQueue<Chunk> chunkQueue = new ArrayBlockingQueue<>(16);
    public static ArrayBlockingQueue<SubChunk> dirtySubChunks = new ArrayBlockingQueue<>(64);

    private static FloatBuffer projBuffer = BufferUtils.createFloatBuffer(16);

    /**
     * Est appelé avant le début du draw scene 
     */
    @OpenGLThread
    public static void setupBeforeRendering(){
        BlockBorderRenderer.init();
        // initialise en mémoire les textures des caractères
        TexturesManager.loadTextures();
        try{
            AudioManager.loadSound("bird", "/Audios/bird.ogg");
            AudioManager.loadSound("samsung", "/Audios/samsung.ogg");
            AudioManager.loadSound("fanta", "/Audios/fanta.ogg");
            // initialise en mémoire l'image des caractères
            Render.textRender = new TextRender(FileLoader.loadFileFromClasspathBB("/Textures/font.png"), 20, 20, 19,
                                        " !\"#$%&\'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~");          
        }catch(IOException e){
            System.out.println(e);
        }        
    }

    /**
     * Gère le rendu de la fenêtre.
     * @param window
     * @param windowRef
     */
    @OpenGLThread
    public static void rendering(long windowRef){
        // Stocke le temps actuel (UNIX)
        long lastTimeMs = System.currentTimeMillis();
        long lastTimeNano = System.nanoTime();        

        // Prépare la 3D
        Render.initialize3D(Window.width, Window.height);

        GameLoop.init();

        while ( !GLFW.glfwWindowShouldClose(windowRef) ) {  
            // démarre l'enregistrement de performances pour la frame
            monitor.beginFrame();

            // Stocke le temps actuel (UNIX) et en nanosec
            long nowMs = System.currentTimeMillis();
            long nowNano = System.nanoTime();

            // nettoie le frame buffer
			GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);

            // Enregistre le temps pour les inputs
            monitor.startTimer("logic");
            // Modifie le déplacement de la caméra 
            InputManager.run(windowRef, sceneGraph, nowMs - lastTimeMs);
            monitor.stopTimer("logic");

            // utilise le shader 
            ShadersManager.useShader();
            
            // donne les matrices de camera et perspective au shader
            ShadersManager.runProjection(Render.calculateProjection(45.0f, (float) Window.width / Window.height, 0.1f, 5000.f)); // donne la perspective de l'ecran
            ShadersManager.runView(PlayersManager.player.getCamera().getViewMatrixBuffer()); // donne la vue de la camera

            GameLoop.run(nowMs - lastTimeMs);

            // poll les chunks prêts et upload sur GPU
            Chunk chunk;
            while((chunk = Render.chunkQueue.poll()) != null){
                Render.sceneGraph.root.addChild(chunk);
            }

            // Chunks modifiés update le VBO existant
            SubChunk dirty;
            while ((dirty = dirtySubChunks.poll()) != null) {
                dirty.setupSubChunkVBOVAO();
            }

            // enregistre le temps de rendu
            monitor.startTimer("render");
            // Dessine la scène
            Render.drawScene();     
            monitor.stopTimer("render");

            // récupère le nombre de vbo
            monitor.setActiveVAOs(BuffersManager.activeVAOCount);

            // Affiche les fps            
            double delta = (nowNano - lastTimeNano) / 1_000_000_000.0;
            Render.calculateFPS(delta);

            // crosshair simple
            Render.getTextRender().drawText("+", Window.width / 2, Window.height / 2, 30, Color4f.BLACK);

            // Affiche la pos du joueur
            Vector3f pos = PlayersManager.player.getPosition();
            Render.getTextRender().drawText(String.format("Position: (%.2f, %.2f, %.2f)%n", pos.x, pos.y - 1.5f, pos.z), Window.width / 2,20, 15, Color4f.RED);

            // Affiche la touche à appuyer pour toggle le wireframe
            Render.getTextRender().drawText("Toggle WireFrameMode : E", 0, 20, 15, Color4f.YELLOW);
            Render.getTextRender().drawText("Toggle MouseLock : P", 0, 40, 15, Color4f.YELLOW);       
            Render.getTextRender().drawText("Toggle ChunkBorder : B", 0, 60, 15, Color4f.YELLOW);   
            Render.getTextRender().drawText("Toggle Teleport to (0,10,0): T", 0, 80, 15, Color4f.YELLOW);      

            // Affiche le nombre de nœud à l'écran
            Render.getTextRender().drawText("Node3D count : " + Render.sceneGraph.getTotalNodeCount(), 0, 100, 15, Color4f.PURPLE);         

            // Affiche les lignes retourner par le PerformanceManager
            String[] lines = monitor.getDisplayLines();
            for(int i = 0; i < lines.length; i++){
                if(lines[i] != null){
                    Render.getTextRender().drawText(lines[i], 0, 120 + i * 20, 15, Color4f.PURPLE);
                }
            }

            // Rends la fenêtre en buffer dessinée visible à l’écran
            GLFW.glfwSwapBuffers(windowRef);		

            GLFW.glfwPollEvents();

            lastTimeMs = nowMs;           
            lastTimeNano = nowNano; 

            // TESTE A UTILISE POUR VOIR SI IL Y A ACCUMULATION DE RAM (FUITE DE MEMOIRE)
            /*/
            if(frameCount % 600 == 0){ // toutes les 600 frames (~10sec à 60fps)
                System.gc();
                System.out.println("RAM après GC: " + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024*1024) + " MB");
            }*/
            GameLoop.quit();
            monitor.endFrame();
		}

        // ferme le fenêtre quand la boucle de rendu est fini
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
     * Initialise la 3D avant la boucle de Rendu
     * @param width int
     * @param height int
     */
    @OpenGLThread
    private static void initialize3D(final int width, final int height)
    {
        // *** Gestion de la transparence ***
        // On indique que l’on souhaite activer la transparence
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        // Spécification de la précision de la transparence
        GL11.glAlphaFunc(GL11.GL_GREATER, 0.01f);
        // Façon dont la transparence est calculée
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        /// *** Apparence globale ***
        // Pour des raisons de performances, on désactive les textures de manière globale, on les activera au cas par cas
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        // On active la possibilité de combiner des effets
        GL11.glEnable(GL11.GL_BLEND);

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
        Matrix4f projection = new Matrix4f().perspective((float) Math.toRadians(fov), aspect, near, far);
        projection.get(projBuffer);
        return projBuffer;
    }
}
