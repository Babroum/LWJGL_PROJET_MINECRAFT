package demo.Rendering;

import java.nio.FloatBuffer;

import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import demo.ShaderManager;
import demo.Annotations.OpenGLThread;
import demo.Geometry.Node3D;
import demo.Geometry.Object3D;
import demo.Types.Color4f;

public class Render {
    
    public SceneGraph sceneGraph = new SceneGraph(); 
    private double[] fpsHistory = new double[60];

    private int fpsIndex;
    private int frameCount;
    private float displayedFps = 0.0f;

    /**
     * Gère le rendu de la fenêtre.
     * @param window
     * @param windowRef
     */
    @OpenGLThread
    public void rendering(Window window, long windowRef){
        // Stocke le temps actuel (UNIX)
        long lastTimeMs = System.currentTimeMillis();
        long lastTimeNano = System.nanoTime();

        this.initialize3D(window.width, window.height);

        for(Node3D obj : window.getSceneGraph().root.getChildren()){
            if (obj instanceof Object3D){
                ((Object3D)obj).setupObjectBuffers();
            }
        }  

        // positionne la camera
        this.sceneGraph.getCamera().setPosition(0, 0, 3);

        while ( !GLFW.glfwWindowShouldClose(windowRef) ) {  
            // Stocke le temps actuel (UNIX) en nanosec
            long nowMs = System.currentTimeMillis();
            long nowNano = System.nanoTime();

			GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT); // clear the framebuffer

            // Modifie le déplacement de la caméra 
            window.getInputManager().run(window, sceneGraph, nowMs - lastTimeMs);

            ShaderManager.useShader();
            
            // donne les matrices de camera et perspective au shader
            ShaderManager.runProjection(this.calculateProjection(45.0f, (float) window.width / window.height, 0.1f, 5000.f)); // donne la perspective de l'ecran
            ShaderManager.runView(this.sceneGraph.getCamera().getViewMatrixBuffer()); // donne la vue de la camera
                                     
            // Dessine la scène
            this.drawScene();     

            // Affiche les fps            
            double delta = (nowNano - lastTimeNano) / 1_000_000_000.0;
            this.calculateFPS(delta, window);

            // Affiche le nombre de vertex à l'écran
            window.getTextRender().drawText(window, "Vertex count : " + this.sceneGraph.getTotalVertexCount(), 0, 20, 20, Color4f.BLUE);
            // Affiche le nombre de nœud à l'écran
            window.getTextRender().drawText(window, "Node3D count : " + this.sceneGraph.getTotalNodeCount(), 0, 40, 20, Color4f.YELLOW);
            // Affiche la touche à appuyer pour toggle le wireframe
            window.getTextRender().drawText(window, "Toggle WireFrameMode : E", 0, 60, 15, Color4f.PURPLE);
            // Affiche la touche à appuyer pour toggle le mouselock
            window.getTextRender().drawText(window, "Toggle MouseLock : P", 0, 80, 15, Color4f.PURPLE);

            // Rends la fenêtre en buffer dessinée visible à l’écran
            GLFW.glfwSwapBuffers(windowRef);		

            GLFW.glfwPollEvents();
            lastTimeMs = nowMs;           
            lastTimeNano = nowNano; 
		}

        window.exitWidow();
    }

    @OpenGLThread
    private void initialize3D(final int width, final int height)
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
    }
    
    @OpenGLThread
    private void drawScene(){
        this.sceneGraph.drawScene();
    }

    @OpenGLThread
    /**
     * Calcule les fps 
     * @param delta double, temps en nano seconde entre 2 frames
     * @param window Window
     */
    private void calculateFPS(double delta, Window window){
        if (delta > 0){
            this.fpsHistory[this.fpsIndex] = 1.0 / delta;
            this.fpsIndex = (this.fpsIndex + 1) % this.fpsHistory.length;
            if (this.frameCount % 30 == 0){
                double sum = 0;
                for (double fps : fpsHistory){
                    sum += fps;
                }
                this.displayedFps = (float)(sum / this.fpsHistory.length);
            }
            window.getTextRender().drawText(window, "AVG FPS : " + String.format("%.0f", this.displayedFps) , 0, 0, 20, Color4f.RED);
            this.frameCount++;
        }        
    }

    // calcule la projection 
    private FloatBuffer calculateProjection(float fov, float aspect, float near, float far) {
        FloatBuffer matrixBuffer = BufferUtils.createFloatBuffer(16);
        Matrix4f projection = new Matrix4f().perspective((float) Math.toRadians(fov), aspect, near, far);
        projection.get(matrixBuffer);
        return matrixBuffer;
    }
}
