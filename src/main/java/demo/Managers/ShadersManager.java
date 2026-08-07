package demo.Managers;

import java.io.IOException;
import java.nio.FloatBuffer;

import org.joml.Matrix4f;
import org.lwjgl.opengl.GL20;

import demo.Annotations.OpenGLThread;
import demo.Others.FileLoader;

/**
 * Gère les shaders
 */
public abstract class ShadersManager {

    /** Id du programme shader principal (rendu des chunks opaque/transparent/verre). */
    private static int programId;
    /** Id du programme shader des bordures de bloc. */
    private static int programBlockBorderId;
    /** Id du programme shader du Gui. */
    private static int programGuiId;
    /** Id du programme shader de post-process (FBO). */
    private static int programPostProcessId;
    /** Id du programme shader des liquides. */
    private static int programLiquidId;

    /** Emplacement uniforme "screenTex" du shader de post-process. */
    private static int locationScreenTex;
    /** Emplacement uniforme "saturationAmount" du shader de post-process. */
    private static int locationSaturation;

    /** Emplacements uniformes "projection", "view" et "model" du shader principal. */
    private static int locationProj, locationView, locationModel;
    /** Emplacements uniformes "projection", "view" et "blockPos" du shader des bordures de bloc. */
    private static int locationProjBlockBorder, locationViewBlockBorder, locationBlockPosBlockBorder;
    /** Emplacements uniformes "ortho" et "tex" du shader de Gui. */
    private static int locationOrtho, locationGuiTex;
    /** Emplacement uniforme "fogStart" du shader principal. */
    private static int locationFogStart;
    /** Emplacement uniforme "fogEnd" du shader principal. */
    private static int locationFogEnd;
    /** Emplacement uniforme "skyColor" du shader principal. */
    private static int locationSkyColor;
    /** Emplacement uniforme "blockLightColor" du shader principal. */
    private static int locationBlockLightColor;
    /** Emplacement uniforme "time" du shader principal. */
    private static int locationTime;

    /** Emplacement uniforme "projection" du shader liquide. */
    private static int locationProjLiquid; 
    /** Emplacement uniforme "view" du shader liquide. */
    private static int locationViewLiquid;
    /** Emplacement uniforme "model" du shader liquide. */
    private static int locationModelLiquid;
    /** Emplacement uniforme "time" du shader liquide. */
    private static int locationTimeLiquid;
    /** Emplacement uniforme "skyColor" du shader liquide. */
    private static int locationSkyColorLiquid;
    /** Emplacement uniforme "fogStart" du shader liquide. */
    private static int locationFogStartLiquid;
    /** Emplacement uniforme "fogEnd" du shader liquide. */
    private static int locationFogEndLiquid;
    /** Emplacement uniforme "blockLightColor" du shader liquide. */
    private static int locationBlockLightColorLiquid;

    /** Constructeur implicite vide */
    public ShadersManager(){}

    /** Matrice orthonormée réutilisée pour le rendu du Gui. */
    private static final Matrix4f guiOrthoMatrix = new Matrix4f();
    /** Dernières largeur/hauteur de fenêtre utilisées pour {@link #guiOrthoMatrix}, pour éviter de la recalculer inutilement. */
    private static int lastWidth = 0, lastHeight = 0;

    /**
     * Initialise le shader des liquides
     * @throws RuntimeException si la compilation ou le linkage du shader échoue
     */
    @OpenGLThread
    public static void createLiquidShader(){
        String vertexShader;
        String fragmentShader;
        try{
            vertexShader = FileLoader.loadShaderFromClasspathS("/Shaders/vertex_liquid.glsl");
            fragmentShader = FileLoader.loadShaderFromClasspathS("/Shaders/fragment_liquid.glsl");
        }catch(IOException e){
            e.printStackTrace();
            return;
        }

        int[] ids = ShadersManager.create(vertexShader, fragmentShader);
        int vsId = ids[0];
        int fgId = ids[1];

        ShadersManager.programLiquidId = GL20.glCreateProgram();
        GL20.glAttachShader(ShadersManager.programLiquidId, vsId);
        GL20.glAttachShader(ShadersManager.programLiquidId, fgId);

        GL20.glLinkProgram(ShadersManager.programLiquidId);
        int success = GL20.glGetProgrami(ShadersManager.programLiquidId, GL20.GL_LINK_STATUS);
        if (success == GL20.GL_FALSE){
            throw new RuntimeException("ShaderManagerError : "+GL20.glGetProgramInfoLog(ShadersManager.programLiquidId));
        }

        GL20.glDetachShader(ShadersManager.programLiquidId, vsId);
        GL20.glDeleteShader(vsId);

        GL20.glDetachShader(ShadersManager.programLiquidId, fgId);
        GL20.glDeleteShader(fgId);
    }

    /**
     * Initialise le shader de post-process (FBO)
     * @throws RuntimeException si la compilation ou le linkage du shader échoue
     */
    @OpenGLThread
    public static void createPostProcessShader(){
        String vertexShader;
        String fragmentShader;
        try{
            vertexShader = FileLoader.loadShaderFromClasspathS("/Shaders/vertex_postprocess.glsl");
            fragmentShader = FileLoader.loadShaderFromClasspathS("/Shaders/fragment_postprocess.glsl");
        }catch(IOException e){
            e.printStackTrace();
            return;
        }

        int[] ids = ShadersManager.create(vertexShader, fragmentShader);
        int vsId = ids[0];
        int fgId = ids[1];

        ShadersManager.programPostProcessId = GL20.glCreateProgram();
        GL20.glAttachShader(ShadersManager.programPostProcessId, vsId);
        GL20.glAttachShader(ShadersManager.programPostProcessId, fgId);

        GL20.glLinkProgram(ShadersManager.programPostProcessId);
        int success = GL20.glGetProgrami(ShadersManager.programPostProcessId, GL20.GL_LINK_STATUS);
        if (success == GL20.GL_FALSE){
            throw new RuntimeException("ShaderManagerError : "+GL20.glGetProgramInfoLog(ShadersManager.programPostProcessId));
        }

        GL20.glDetachShader(ShadersManager.programPostProcessId, vsId);
        GL20.glDeleteShader(vsId);

        GL20.glDetachShader(ShadersManager.programPostProcessId, fgId);
        GL20.glDeleteShader(fgId);
    }

    /**
     * Initialise les shaders pour le Gui
     * @throws RuntimeException si la compilation ou le linkage du shader échoue
     */
    @OpenGLThread
    public static void createGuiShader(){
        String vertexGuiShader;
        String fragmentGuiShader;
        try{
            vertexGuiShader = FileLoader.loadShaderFromClasspathS("/Shaders/vertex_Gui.glsl");
            fragmentGuiShader = FileLoader.loadShaderFromClasspathS("/Shaders/fragment_Gui.glsl");
        }catch(IOException e){
            e.printStackTrace();
            return;
        }

        int[] ids = ShadersManager.create(vertexGuiShader, fragmentGuiShader);
        int vsId = ids[0];
        int fgId = ids[1];
        
        // attacher les shader compilés au programme
        ShadersManager.programGuiId = GL20.glCreateProgram();
        GL20.glAttachShader(ShadersManager.programGuiId, vsId);
        GL20.glAttachShader(ShadersManager.programGuiId, fgId);
         
        // vérifie que les sorties du vertex shader corresponde aux entrées du fragment shader
        GL20.glLinkProgram(ShadersManager.programGuiId);
        int success = GL20.glGetProgrami(ShadersManager.programGuiId, GL20.GL_LINK_STATUS);
        if (success == GL20.GL_FALSE){
            throw new RuntimeException("ShaderManagerError : "+GL20.glGetProgramInfoLog(ShadersManager.programGuiId));
        }
        
        // une fois les vertex liés on détache et supprime les shaders
        GL20.glDetachShader(ShadersManager.programGuiId, vsId);
        GL20.glDeleteShader(vsId);
        
        GL20.glDetachShader(ShadersManager.programGuiId, fgId);
        GL20.glDeleteShader(fgId);        
    }

    /**
     * Initialise les shaders pour les bordures de bloc
     * @throws RuntimeException si la compilation ou le linkage du shader échoue
    */
    @OpenGLThread
    public static void createBlockBorderShader(){
        String vertexShader;
        String fragmentShader;
        try{
            vertexShader = FileLoader.loadShaderFromClasspathS("/Shaders/vertex_block_border.glsl");
            fragmentShader = FileLoader.loadShaderFromClasspathS("/Shaders/fragment_block_border.glsl");
        }catch(IOException e){
            e.printStackTrace();
            return;
        }

        int[] ids = ShadersManager.create(vertexShader, fragmentShader);
        int vsId = ids[0];
        int fgId = ids[1];
        
        // attacher les shader compilés au programme
        ShadersManager.programBlockBorderId = GL20.glCreateProgram();
        GL20.glAttachShader(ShadersManager.programBlockBorderId, vsId);
        GL20.glAttachShader(ShadersManager.programBlockBorderId, fgId);

        // vérifie que les sorties du vertex shader corresponde aux entrées du fragment shader
        GL20.glLinkProgram(ShadersManager.programBlockBorderId);
        int success = GL20.glGetProgrami(ShadersManager.programBlockBorderId, GL20.GL_LINK_STATUS);
        if (success == GL20.GL_FALSE){
            throw new RuntimeException("ShaderManagerError : "+GL20.glGetProgramInfoLog(ShadersManager.programBlockBorderId));
        }

        // une fois les vertex liés on détache et supprime les shaders
        GL20.glDetachShader(ShadersManager.programBlockBorderId, vsId);
        GL20.glDeleteShader(vsId);

        GL20.glDetachShader(ShadersManager.programBlockBorderId, fgId);
        GL20.glDeleteShader(fgId);
    }

    /**
     * Initialise les shaders pour les chunks
     * @throws RuntimeException si la compilation ou le linkage du shader échoue
     */
    @OpenGLThread
    public static void createShader(){
        String vertexShader;
        String fragmentShader;
        try{
            vertexShader = FileLoader.loadShaderFromClasspathS("/Shaders/vertex.glsl");
            fragmentShader = FileLoader.loadShaderFromClasspathS("/Shaders/fragment.glsl");
        }catch(IOException e){
            e.printStackTrace();
            return;
        }

        int[] ids = ShadersManager.create(vertexShader, fragmentShader);
        int vsId = ids[0];
        int fgId = ids[1];
        
        // attacher les shader compilés au programme
        ShadersManager.programId = GL20.glCreateProgram();
        GL20.glAttachShader(ShadersManager.programId, vsId);
        GL20.glAttachShader(ShadersManager.programId, fgId);

        // vérifie que les sorties du vertex shader corresponde aux entrées du fragment shader
        GL20.glLinkProgram(ShadersManager.programId);
        int success = GL20.glGetProgrami(ShadersManager.programId, GL20.GL_LINK_STATUS);
        if (success == GL20.GL_FALSE){
            throw new RuntimeException("ShaderManagerError : "+GL20.glGetProgramInfoLog(ShadersManager.programId));
        }

        // une fois les vertex liés on détache et supprime les shaders
        GL20.glDetachShader(ShadersManager.programId, vsId);
        GL20.glDeleteShader(vsId);

        GL20.glDetachShader(ShadersManager.programId, fgId);
        GL20.glDeleteShader(fgId);
    }


    /**
     * Compile un vertex shader et un fragment shader à partir de leur code source.
     * @param vertexShader code source du vertex shader
     * @param fragmentShader code source du fragment shader
     * @return int[] {idVertexShader, idFragmentShader}
     * @throws RuntimeException si la compilation d'un des deux shaders échoue
     */
    @OpenGLThread
    private static int[] create(String vertexShader, String fragmentShader){
        // creation de l'object shader
        int vsId = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
        // attacher le code source au shader
        GL20.glShaderSource(vsId, vertexShader);
        // compiler le shader
        GL20.glCompileShader(vsId);
        int success = GL20.glGetShaderi(vsId, GL20.GL_COMPILE_STATUS);
        if (success == GL20.GL_FALSE){
            throw new RuntimeException("ShaderManagerError : "+GL20.glGetShaderInfoLog(vsId));
        }

        int fgId = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
        GL20.glShaderSource(fgId, fragmentShader);
        GL20.glCompileShader(fgId);
        success = GL20.glGetShaderi(fgId, GL20.GL_COMPILE_STATUS);
        if (success == GL20.GL_FALSE){
            throw new RuntimeException("ShaderManagerError : "+GL20.glGetShaderInfoLog(fgId));
        }

        return new int[]{vsId, fgId};
    }

    /**
     * Recherche et met en cache l'emplacement de tous les uniforms de tous les programmes shader.
     * A appeler une seule fois après avoir créé tous les shaders.
     */
    // Appeller une seule fois après avoir créé les shaders
    public static void getUniformLocations() {
        locationProj                  = GL20.glGetUniformLocation(programId, "projection");
        locationView                  = GL20.glGetUniformLocation(programId, "view");
        locationModel                 = GL20.glGetUniformLocation(programId, "model");
        locationSkyColor              = GL20.glGetUniformLocation(programId, "skyColor");
        locationFogStart              = GL20.glGetUniformLocation(programId, "fogStart");
        locationFogEnd                = GL20.glGetUniformLocation(programId, "fogEnd");
        locationBlockLightColor       = GL20.glGetUniformLocation(programId, "blockLightColor");
        locationTime                  = GL20.glGetUniformLocation(programId, "time");

        locationProjBlockBorder       = GL20.glGetUniformLocation(programBlockBorderId, "projection");
        locationViewBlockBorder       = GL20.glGetUniformLocation(programBlockBorderId, "view");
        locationBlockPosBlockBorder   = GL20.glGetUniformLocation(programBlockBorderId, "blockPos");

        locationOrtho                 = GL20.glGetUniformLocation(programGuiId, "ortho");
        locationGuiTex                = GL20.glGetUniformLocation(programGuiId, "tex");

        locationSaturation            = GL20.glGetUniformLocation(programPostProcessId, "saturationAmount");
        locationScreenTex             = GL20.glGetUniformLocation(programPostProcessId, "screenTex");

        locationProjLiquid            = GL20.glGetUniformLocation(programLiquidId, "projection");
        locationViewLiquid            = GL20.glGetUniformLocation(programLiquidId, "view");
        locationModelLiquid           = GL20.glGetUniformLocation(programLiquidId, "model");
        locationSkyColorLiquid        = GL20.glGetUniformLocation(programLiquidId, "skyColor");
        locationFogStartLiquid        = GL20.glGetUniformLocation(programLiquidId, "fogStart");
        locationFogEndLiquid          = GL20.glGetUniformLocation(programLiquidId, "fogEnd");
        locationBlockLightColorLiquid = GL20.glGetUniformLocation(programLiquidId, "blockLightColor");
        locationTimeLiquid            = GL20.glGetUniformLocation(programLiquidId, "time");
    }   

    /**
     * Active le shader des liquides
     */
    @OpenGLThread
    public static void useLiquidShader(){
        GL20.glUseProgram(ShadersManager.programLiquidId);
    }

    /**
     * Active le shader de post-process (FBO)
     */
    @OpenGLThread
    public static void usePostProcessShader(){
        GL20.glUseProgram(ShadersManager.programPostProcessId);
    }

    /**
     * Active le shader principal (chunks)
     */
    @OpenGLThread
    public static void useShader(){
        GL20.glUseProgram(ShadersManager.programId);
    }

    /**
     * Active le shader des bordures de bloc
     */
    @OpenGLThread
    public static void useBlockBorderShader(){
        GL20.glUseProgram(ShadersManager.programBlockBorderId);
    }
    
    /**
     * Active le shader de Gui
     */
    @OpenGLThread
    public static void useGuiShader(){
        GL20.glUseProgram(ShadersManager.programGuiId);
    }

    /**
     * Envoie le temps écoulé au shader principal
     * @param time float, temps écoulé
     */
    @OpenGLThread
    public static void runTime(float time){
        GL20.glUniform1f(ShadersManager.locationTime, time);
    }

    /**
     * Envoie le temps écoulé au shader liquide
     * @param time float, temps écoulé
     */
    @OpenGLThread
    public static void runTimeLiquid(float time){
        GL20.glUniform1f(ShadersManager.locationTimeLiquid, time);
    }

    /**
     * Envoie l'unité de texture de l'écran au shader de post-process
     * @param textureUnit int, unité de texture
     */
    @OpenGLThread
    public static void runScreenTex(int textureUnit){
        GL20.glUniform1i(ShadersManager.locationScreenTex, textureUnit);
    }

    /**
     * Envoie la distance de début du fog au shader
     * @param fogStart float
     */
    @OpenGLThread
    public static void runFogStart(float fogStart){
        GL20.glUniform1f(ShadersManager.locationFogStart, fogStart);
    }

    /**
     * Envoie la distance de début du fog au shader
     * @param fogStart float
     */
    @OpenGLThread
    public static void runFogStartLiquid(float fogStart){
        GL20.glUniform1f(ShadersManager.locationFogStartLiquid, fogStart);
    }


    /**
     * Envoie la distance de fin du fog au shader
     * @param fogEnd float
     */
    @OpenGLThread
    public static void runFogEnd(float fogEnd){
        GL20.glUniform1f(ShadersManager.locationFogEnd, fogEnd);
    }

    /**
     * Envoie la distance de fin du fog au shader
     * @param fogEnd float
     */
    @OpenGLThread
    public static void runFogEndLiquid(float fogEnd){
        GL20.glUniform1f(ShadersManager.locationFogEndLiquid, fogEnd);
    }

    /**
     * Envoie la couleur du ciel/fog au shader
     * @param r float
     * @param g float
     * @param b float
     */
    @OpenGLThread
    public static void runBlockLightColor(float r, float g, float b){
        GL20.glUniform3f(ShadersManager.locationBlockLightColor, r, g, b);
    }
    /**
     * Envoie la couleur du ciel/fog au shader
     * @param r float
     * @param g float
     * @param b float
     */
    @OpenGLThread
    public static void runBlockLightColorLiquid(float r, float g, float b){
        GL20.glUniform3f(ShadersManager.locationBlockLightColorLiquid, r, g, b);
    }


    /**
     * Envoie la quantité de saturation au shader de post-process
     * @param saturationAmount float, quantité de saturation
     */
    @OpenGLThread
    public static void runSaturationAmountPostProcess(float saturationAmount){
        GL20.glUniform1f(ShadersManager.locationSaturation, saturationAmount);
    }

    /**
     * Envoie la couleur du ciel/fog au shader
     * @param r float
     * @param g float
     * @param b float
     */
    @OpenGLThread
    public static void runSkyColor(float r, float g, float b){
        GL20.glUniform3f(ShadersManager.locationSkyColor, r, g, b);
    }

    /**
     * Envoie la couleur du ciel/fog au shader
     * @param r float
     * @param g float
     * @param b float
     */
    @OpenGLThread
    public static void runSkyColorLiquid(float r, float g, float b){
        GL20.glUniform3f(ShadersManager.locationSkyColorLiquid, r, g, b);
    }

    /**
     * Envoie la projection au shader
     * @param proj FloatBuffer
     */
    @OpenGLThread 
    public static void runProjection(FloatBuffer proj){
        proj.rewind();
        GL20.glUniformMatrix4fv(ShadersManager.locationProj, false, proj);        
    }

    /**
     * Envoie la projection au shader
     * @param proj FloatBuffer
     */
    @OpenGLThread 
    public static void runProjectionLiquid(FloatBuffer proj){
        proj.rewind();      
        GL20.glUniformMatrix4fv(ShadersManager.locationProjLiquid, false, proj);  
    }

    /**
     * Envoie la viewMatrix de la caméra au shader
     * @param view FloatBuffer
     */
    @OpenGLThread 
    public static void runView(FloatBuffer view){
        view.rewind();
        GL20.glUniformMatrix4fv(ShadersManager.locationView, false, view);
    }

    /**
     * Envoie la viewMatrix de la caméra au shader
     * @param view FloatBuffer
     */
    @OpenGLThread 
    public static void runViewLiquid(FloatBuffer view){
        view.rewind();
        GL20.glUniformMatrix4fv(ShadersManager.locationViewLiquid, false, view);
    }

    /**
     * Envoie le model des cubes au shader
     * @param model FloatBuffer
     */
    @OpenGLThread
    public static void runModel(FloatBuffer model){
        model.rewind();
        GL20.glUniformMatrix4fv(ShadersManager.locationModel, false, model);
    }

    /**
     * Envoie le model des cubes au shader
     * @param model FloatBuffer
     */
    @OpenGLThread
    public static void runModelLiquid(FloatBuffer model){
        model.rewind();
        GL20.glUniformMatrix4fv(ShadersManager.locationModelLiquid, false, model);
    }

    /**
     * Envoie la projection au shader des bordures de bloc
     * @param proj FloatBuffer
     */
    @OpenGLThread 
    public static void runProjectionBlockBorder(FloatBuffer proj){
        proj.rewind();
        GL20.glUniformMatrix4fv(ShadersManager.locationProjBlockBorder, false, proj);        
    }

    /**
     * Envoie la viewMatrix de la caméra au shader des bordures de bloc
     * @param view FloatBuffer
     */
    @OpenGLThread 
    public static void runViewBlockBorder(FloatBuffer view){
        view.rewind();
        GL20.glUniformMatrix4fv(ShadersManager.locationViewBlockBorder, false, view);
    }

    /**
     * Envoie la pos des lignes du bloc au shader des bordures de bloc
     * @param x int
     * @param y int
     * @param z int
     */
    @OpenGLThread 
    public static void runBlockPosBlockBorder(int x, int y, int z){
        GL20.glUniform3f(ShadersManager.locationBlockPosBlockBorder, x, y, z);
    }

    /**
     * Envoie la matrice orthonormé au shader de texte
     * @param buffer FloatBuffer
     * @param width int
     * @param height int
     */
    @OpenGLThread
    public static void runGuiOrtho(FloatBuffer buffer, int width, int height){
        // ne change que si la fenetre est redimensionnée
        if (width != lastWidth || height != lastHeight) {
            guiOrthoMatrix.identity().ortho(0, width, height, 0, -1, 1);
            lastWidth = width;
            lastHeight = height;
        }
        guiOrthoMatrix.get(buffer);
        GL20.glUniformMatrix4fv(ShadersManager.locationOrtho, false, buffer);
    }

    /**
     * Indique au shader GUI quelle unité de texture utiliser
     * @param textureUnit int, l'unité de texture active (0 pour GL_TEXTURE0)
     */
    @OpenGLThread
    public static void runGuiTexture(int textureUnit){
        GL20.glUniform1i(ShadersManager.locationGuiTex, textureUnit);
    }

    /**
     * Supprime le programme shader des liquides
     */
    @OpenGLThread
    public static void closeProgramLiquidId(){
        GL20.glDeleteProgram(ShadersManager.programLiquidId);
    }

    /**
     * Supprime le programme shader de post-process
     */
    @OpenGLThread
    public static void closeProgramPostProcessId(){
        GL20.glDeleteProgram(ShadersManager.programPostProcessId);
    }

    /**
     * Supprime le programme shader principal
     */
    @OpenGLThread
    public static void closeProgramId(){
        GL20.glDeleteProgram(ShadersManager.programId);
    }

    /**
     * Supprime le programme shader des bordures de bloc
     */
   @OpenGLThread
    public static void closeProgramBlockBorderId(){
        GL20.glDeleteProgram(ShadersManager.programBlockBorderId);
    }  

    /**
     * Supprime le programme shader de Gui
     */
    @OpenGLThread
    public static void closeProgramGuiId(){
        GL20.glDeleteProgram(ShadersManager.programGuiId);
    }

    /**
     * Retourne l'id du programme shader des liquides
     * @return int
     */
    @OpenGLThread
    public static int getProgramLiquidId(){
        return ShadersManager.programLiquidId;
    }

    /**
     * Retourne l'id du programme shader de post-process
     * @return int
     */
    @OpenGLThread
    public static int getProgramPostProcessId(){
        return ShadersManager.programPostProcessId;
    }

    /**
     * Retourne l'id du programme shader principal
     * @return int
     */
    @OpenGLThread
    public static int getProgramId(){
        return ShadersManager.programId;
    }

    /**
     * Retourne l'id du programme shader des bordures de bloc
     * @return int
     */
    @OpenGLThread
    public static int getProgramBlockBorderId(){
        return ShadersManager.programBlockBorderId;
    }

    /**
     * Retourne l'id du programme shader de Gui
     * @return int
     */
    @OpenGLThread
    public static int getProgramGuiId(){
        return ShadersManager.programGuiId;
    }
}