package demo.Rendering;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import demo.ShaderManager;
import demo.Annotations.OpenGLThread;
import demo.Enum.Textures;
import demo.Events.InputManager;
import demo.Geometry.Node3D;
import demo.Geometry.Object3D;
import demo.Types.Color4f;

public class Render {
    
    public static SceneGraph sceneGraph = new SceneGraph(); 
    private static TextRender textRender;  
    private static TextureRender textureRender;

    private static double[] fpsHistory = new double[60];

    private static int fpsIndex;
    private static int frameCount;
    private static float displayedFps = 0.0f;

    private static FloatBuffer projBuffer = BufferUtils.createFloatBuffer(16);

    public static PerformanceMonitor monitor = new PerformanceMonitor();


    public static void setupBeforeRendering(){
        // initialise en mémoire l'image des caractères
        try{
            Render.textRender = new TextRender(Render.loadImageFromClasspath("/Textures/font.png"), 20, 20, 19,
                                        " !\"#$%&\'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~");   
            // initialise en mémoire l'image des textures
            Render.textureRender = new TextureRender();
            Render.textureRender.addTexture(Render.loadImageFromClasspath("/Textures/Blocks/null.png"), Textures.NULL);
            Render.textureRender.addTexture(Render.loadImageFromClasspath("/Textures/Blocks/max.png"), Textures.MAX);
            Render.textureRender.addTexture(Render.loadImageFromClasspath("/Textures/Blocks/grass_block_top.png"), Textures.GRASS_TOP);
            Render.textureRender.addTexture(Render.loadImageFromClasspath("/Textures/Blocks/grass_block_side.png"), Textures.GRASS_SIDE);
            Render.textureRender.addTexture(Render.loadImageFromClasspath("/Textures/Blocks/stone.png"), Textures.STONE);
            Render.textureRender.addTexture(Render.loadImageFromClasspath("/Textures/Blocks/dirt.png"), Textures.DIRT);
        }catch(IOException e){
            System.out.println(e);
        }        
    }

    // permet de lire les images qui seront dans le .jar
    public static ByteBuffer loadImageFromClasspath(String path) throws IOException {
        InputStream is = Render.class.getResourceAsStream(path);
        byte[] bytes = is.readAllBytes();
        ByteBuffer buffer = BufferUtils.createByteBuffer(bytes.length);
        buffer.put(bytes).flip();
        return buffer;
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

        Render.initialize3D(Window.width, Window.height);

        Render.setupVBOVAOEBORecursive(Window.getSceneGraph().root);

        // positionne la camera
        Render.sceneGraph.getCamera().setPosition(0f, 0f, 3f);

        while ( !GLFW.glfwWindowShouldClose(windowRef) ) {  
            monitor.beginFrame();

            // Stocke le temps actuel (UNIX) en nanosec
            long nowMs = System.currentTimeMillis();
            long nowNano = System.nanoTime();

			GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT); // clear the framebuffer

            // Modifie le déplacement de la caméra 
            monitor.startTimer("logic");
            InputManager.run(windowRef, sceneGraph, nowMs - lastTimeMs);
            monitor.stopTimer("logic");

            ShaderManager.useShader();
            
            // donne les matrices de camera et perspective au shader
            ShaderManager.runProjection(Render.calculateProjection(45.0f, (float) Window.width / Window.height, 0.1f, 5000.f)); // donne la perspective de l'ecran
            ShaderManager.runView(Render.sceneGraph.getCamera().getViewMatrixBuffer()); // donne la vue de la camera

            // Dessine la scène
            monitor.startTimer("render");
            Render.drawScene();     
            monitor.stopTimer("render");

            monitor.setActiveVAOs(ShaderManager.activeVAOCount);

            // Affiche les fps            
            double delta = (nowNano - lastTimeNano) / 1_000_000_000.0;
            Render.calculateFPS(delta);

            // Affiche le nombre de vertex à l'écran
            Render.getTextRender().drawText("Vertex count : " + Render.sceneGraph.getTotalVertexCount(), 0, 20, 20, Color4f.BLUE);   
            // Affiche la touche à appuyer pour toggle le wireframe
            Render.getTextRender().drawText("Toggle WireFrameMode : E", 0, 40, 15, Color4f.YELLOW);
            // Affiche la touche à appuyer pour toggle le mouselock
            Render.getTextRender().drawText("Toggle MouseLock : P", 0, 60, 15, Color4f.YELLOW);       

            // Affiche le nombre de nœud à l'écran
            Render.getTextRender().drawText("Node3D count : " + Render.sceneGraph.getTotalNodeCount(), 0, 80, 15, Color4f.PURPLE);         

            String[] lines = monitor.getDisplayLines();
            for(int i = 0; i < lines.length; i++){
                if(lines[i] != null){
                    Render.getTextRender().drawText(lines[i], 0, 100 + i * 20, 15, Color4f.PURPLE);
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

            monitor.endFrame();
		}

        Window.exitWindow();
    }

    private static void setupVBOVAOEBORecursive(Node3D node){
        if (node == null) return;
        if (node.getChildren() == null) return;
        if (node instanceof Object3D){
            ((Object3D)node).setupObjectBuffers();
        }
        for(Node3D child : node.getChildren()){
            setupVBOVAOEBORecursive(child);
        }
    }

    public static TextRender getTextRender(){
        return Render.textRender;
    }
    public static TextureRender getTextureRender(){
        return Render.textureRender;
    }

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

        ShaderManager.createShader();    
        ShaderManager.cacheUniformLocations();    
    }
    
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

    // calcule la projection 
    private static FloatBuffer calculateProjection(float fov, float aspect, float near, float far) {
        Matrix4f projection = new Matrix4f().perspective((float) Math.toRadians(fov), aspect, near, far);
        projection.get(projBuffer);
        return projBuffer;
    }
}
