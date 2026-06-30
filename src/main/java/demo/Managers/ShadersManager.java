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

    private static int programId;
    private static int programBlockBorderId;
    private static int programTextId;

    private static int locationProj, locationView, locationModel;
    private static int locationProjBlockBorder, locationViewBlockBorder, locationBlockPosBlockBorder;
    private static int locationOrtho, locationTextColor;
    private static int locationFogStart;
    private static int locationFogEnd;
    private static int locationSkyColor;

    private static final Matrix4f textOrthoMatrix = new Matrix4f();
    private static int lastWidth = 0, lastHeight = 0;

    
    /**
     * Initialise les shaders pour le texte
     */
    @OpenGLThread
    public static void createTextShader(){
        String vertexTextShader;
        String fragmentTextShader;
        try{
            vertexTextShader = FileLoader.loadShaderFromClasspathS("/Shaders/vertex_text.glsl");
            fragmentTextShader = FileLoader.loadShaderFromClasspathS("/Shaders/fragment_text.glsl");
        }catch(IOException e){
            e.printStackTrace();
            return;
        }

        int[] ids = ShadersManager.create(vertexTextShader, fragmentTextShader);
        int vsId = ids[0];
        int fgId = ids[1];

        // attacher les shader compilés au programme
        ShadersManager.programTextId = GL20.glCreateProgram();
        GL20.glAttachShader(ShadersManager.programTextId, vsId);
        GL20.glAttachShader(ShadersManager.programTextId, fgId);

        // vérifie que les sorties du vertex shader corresponde aux entrées du fragment shader
        GL20.glLinkProgram(ShadersManager.programTextId);
        int success = GL20.glGetProgrami(ShadersManager.programTextId, GL20.GL_LINK_STATUS);
        if (success == GL20.GL_FALSE){
            throw new RuntimeException("ShaderManagerError : "+GL20.glGetProgramInfoLog(ShadersManager.programTextId));
        }

        // une fois les vertex liés on détache et supprime les shaders
        GL20.glDetachShader(ShadersManager.programTextId, vsId);
        GL20.glDeleteShader(vsId);

        GL20.glDetachShader(ShadersManager.programTextId, fgId);
        GL20.glDeleteShader(fgId);
    }

    /**
     * Initialise les shaders pour les chunks
     * @throws ShaderManagerError
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
     * Initialise les shaders pour les bordures de bloc
     * @throws ShaderManagerError
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
     * Initialise les shaders
     * @param vertexShader
     * @param fragmentShader
     * @return
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

    // Appelle cette méthode une seule fois après avoir créé les shaders
    public static void getUniformLocations() {
        locationProj = GL20.glGetUniformLocation(programId, "projection");
        locationView = GL20.glGetUniformLocation(programId, "view");
        locationModel = GL20.glGetUniformLocation(programId, "model");
        
        locationProjBlockBorder = GL20.glGetUniformLocation(programBlockBorderId, "projection");
        locationViewBlockBorder = GL20.glGetUniformLocation(programBlockBorderId, "view");
        locationBlockPosBlockBorder = GL20.glGetUniformLocation(programBlockBorderId, "blockPos");

        locationOrtho = GL20.glGetUniformLocation(programTextId, "ortho");
        locationTextColor = GL20.glGetUniformLocation(programTextId, "color");

        locationFogStart = GL20.glGetUniformLocation(programId, "fogStart");
        locationFogEnd   = GL20.glGetUniformLocation(programId, "fogEnd");
        locationSkyColor = GL20.glGetUniformLocation(programId, "skyColor");
    }

    @OpenGLThread
    public static void useShader(){
        GL20.glUseProgram(ShadersManager.programId);
    }

    @OpenGLThread
    public static void useBlockBorderShader(){
        GL20.glUseProgram(ShadersManager.programBlockBorderId);
    }
    
    @OpenGLThread
    public static void useTextShader(){
        GL20.glUseProgram(ShadersManager.programTextId);
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
     * Envoie la distance de fin du fog au shader
     * @param fogEnd float
     */
    @OpenGLThread
    public static void runFogEnd(float fogEnd){
        GL20.glUniform1f(ShadersManager.locationFogEnd, fogEnd);
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
     * Envoie la projection au shader
     * @param proj FloatBuffer
     */
    @OpenGLThread 
    public static void runProjection(FloatBuffer proj){
        proj.rewind();
        GL20.glUniformMatrix4fv(ShadersManager.locationProj, false, proj);        
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
     * Envoie le model des cubes au shader
     * @param model FloatBuffer
     */
    @OpenGLThread
    public static void runModel(FloatBuffer model){
        model.rewind();
        GL20.glUniformMatrix4fv(ShadersManager.locationModel, false, model);
    }

    /**
     * Envoie la matrice orthonormé au shader de texte
     * @param buffer FloatBuffer
     * @param width int
     * @param height int
     */
    @OpenGLThread
    public static void runTextOrtho(FloatBuffer buffer, int width, int height){
        // ne change que si la fenetre est redimensionnée
        if (width != lastWidth || height != lastHeight) {
            textOrthoMatrix.identity().ortho(0, width, height, 0, -1, 1);
            lastWidth = width;
            lastHeight = height;
        }
        textOrthoMatrix.get(buffer);
        GL20.glUniformMatrix4fv(ShadersManager.locationOrtho, false, buffer);
    }

    /**
     * Envoie la couleur au shader de texte
     * @param red float
     * @param green float
     * @param blue float
     * @param alpha float
     */
    @OpenGLThread
    public static void runTextFragmentColor(float red, float green, float blue, float alpha){
        GL20.glUniform4f(ShadersManager.locationTextColor, red, green, blue, alpha);
    }

    @OpenGLThread
    public static void closeProgramId(){
        GL20.glDeleteProgram(ShadersManager.programId);
    }

   @OpenGLThread
    public static void closeProgramBlockBorderId(){
        GL20.glDeleteProgram(ShadersManager.programBlockBorderId);
    }  

    @OpenGLThread
    public static void closeProgramTextId(){
        GL20.glDeleteProgram(ShadersManager.programTextId);
    }

    @OpenGLThread
    public static int getProgramId(){
        return ShadersManager.programId;
    }

    @OpenGLThread
    public static int getProgramBlockBorderId(){
        return ShadersManager.programBlockBorderId;
    }

    @OpenGLThread
    public static int getProgramTextId(){
        return ShadersManager.programTextId;
    }
}
