package demo.Exemple;

import java.util.Objects;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

class RenderEx {

    /**
     * Graphe de scène associé
     */
    final SceneGraphEx sceneGraph = new SceneGraphEx();

    RenderEx(){

    }

    /**
     * Lance la boucle de rendu
     *
     * @param window3D Fenêtre où se dessine la 3D
     * @param window   Référence sur la fenêtrefenêtre
     */
    @ThreadOpenGLEx
    void rendering(final WindowEx window3D, final long window)
    {
        Objects.requireNonNull(window3D, "window must not be null");

        this.initialize3D(window3D.width, window3D.height);

        // Tant que la fenêtre est présente, rafraîchir la 3D
        while (!GLFW.glfwWindowShouldClose(window))
        {
            // Nettoyage de la fenêtre en buffer pour la prochaine frame
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);

            // Dessine la frame courante
            this.drawScene();

            // Rend la fenêtre en buffer dessinée visible à l’écran
            GLFW.glfwSwapBuffers(window);

            // Récupération des événements fenêtre arrivée pendant le dessin de la frame.
            // Les listeners sur les événements claviers seront appelés par cette méthode
            GLFW.glfwPollEvents();

            // TODO traité les évènements survenus
        }

        // La fenêtre de rendu se ferme ou est fermée, on quitte tout proprement
        window3D.exitWidow();
    }
    /**
     * Initialise le rendu OpenGL
     *
     * @param width  Largeur de la fenêtre de dessin
     * @param height Hauteur de la fenêtre de dessin
     */
    @ThreadOpenGLEx
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
        //final double ratio = (double) width / (double) height;
        //GLU.gluPerspective(45.0, ratio, 0.1, 5000.0);
        // Réglage du modèle de la vue
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();

        // Au départ on met un écran blanc
        GL11.glClearColor(1f, 1f, 1f, 1f);
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
        // GL11.glEnable(GL11.GL_LIGHTING);
    }

    /**
     * Dessine la scène 3D
     */
    @ThreadOpenGLEx
    private void drawScene()
    {
        this.sceneGraph.drawScene();
    }
}
