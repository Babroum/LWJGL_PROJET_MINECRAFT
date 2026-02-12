package demo;

import java.nio.FloatBuffer;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

public class Render {
    
    public SceneGraph sceneGraph = new SceneGraph();    
    private Camera camera = new Camera();

    /**
     * Gére le rendu de la fenêtre.
     * @param window
     * @param windowRef
     */
    @OpenGLThread
    public void rendering(Window window, long windowRef){

        this.initialize3D(window.width, window.height);

        while ( !GLFW.glfwWindowShouldClose(windowRef) ) {  

			GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT); // clear the framebuffer

            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glLoadIdentity();
            GL11.glLoadMatrixf(this.camera.getViewMatrixBuffer()); // charge la viewMatrix de la camera

            // draw
            this.drawScene();     
                
            // Rend la fenêtre en buffer dessinée visible à l’écran
            GLFW.glfwSwapBuffers(windowRef);		

            GLFW.glfwPollEvents();
		}

        window.exitWidow();
    }
    @OpenGLThread
    private void initialize3D(final int width, final int height)
    {
        // *** Gestion de la transparence ***
        // On indique que l’on souhaite activer la  transparence
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        // Spécification de la précision de la transparence
        GL11.glAlphaFunc(GL11.GL_GREATER, 0.01f);
        // Façon dont la transparence est calculée
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        /// *** Apparence globale ***
        // Les matériaux peuvent être colorés
        GL11.glEnable(GL11.GL_COLOR_MATERIAL);
        // Pour des raisons de performances, on désactive les textures de manière globale, on les activera au cas par cas
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        // On active la possibilité de combiné des effets
        GL11.glEnable(GL11.GL_BLEND);

        // ** Réglage de la perspective  ***
        // On fixe le rectangle de la fenêtre où la 3D sera dessinée.
        // Ici on veut dessiner sur toute la fenêtre
        // Sur le "frustum", c’est la zone proche de l’utilisateur
        // S’appelle aussi ’view port’
        GL11.glViewport(0, 0, width, height);
        // On veut une projection normalisée
        GL11.glEnable(GL11.GL_NORMALIZE);

        // Réglage de la projection
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();

        final double ratio = (double) width / (double) height;
        Render.gluPerspective(45.0, ratio, 0.1, 5000.0);
        // Réglage du modèle de la vue
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();

        // Au départ on met un écran rouge
        GL11.glClearColor(1f, 0f, 0f, 1f);
        //On active le test de profondeur
        GL11.glEnable(GL11.GL_DEPTH_TEST);

        // Active les face visible que d’un côté (Pour éviter de dessiner des choses qui seront toujours cachés)
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glCullFace(GL11.GL_FRONT);

        // Éclairage de base pour un effet agréable
        GL11.glLightModeli(GL11.GL_LIGHT_MODEL_LOCAL_VIEWER, GL11.GL_TRUE);
        GL11.glShadeModel(GL11.GL_SMOOTH);
        GL11.glLightModeli(GL12.GL_LIGHT_MODEL_COLOR_CONTROL, GL12.GL_SEPARATE_SPECULAR_COLOR);
        GL11.glLightModeli(GL11.GL_LIGHT_MODEL_TWO_SIDE, 1);

        // Active l’éclairage
        //GL11.glEnable(GL11.GL_LIGHTING);
    }

    /**
     * Retourne la camera de la scene
     * @return Camera
     */
    public Camera getCamera(){
        return this.camera;
    }

    @OpenGLThread
    private void drawScene(){
        this.sceneGraph.drawScene();
    }

    // gluPerspective n'existe plus, http://forum.lwjgl.org/index.php?topic=6566.0
    private static void gluPerspective(double fovy, double aspect, double near, double far) {
        double bottom = -near * (double) Math.tan(fovy / 2);
        double top = -bottom;
        double left = aspect * bottom;
        double right = -left;
        GL11.glFrustum(left, right, bottom, top, near, far);
    }
}
