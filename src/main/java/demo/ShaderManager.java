package demo;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.joml.Matrix4f;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import demo.Annotations.OpenGLThread;
import demo.Exceptions.ShaderManagerError;

public abstract class ShaderManager {

    private static int programId;
    private static int programTextId;

    public static int activeVAOCount = 0;

    /**
     * Créer les shaders
     * @throws ShaderManagerError
     */
    @OpenGLThread
    public static void createShader() throws ShaderManagerError{
        String vertexShader;
        String fragmentShader;
        try{
            vertexShader = Files.readString(Paths.get("res/Shaders/vertex.glsl"));
            fragmentShader = Files.readString(Paths.get("res/Shaders/fragment.glsl"));
        }catch(IOException e){
            e.printStackTrace();
            return;
        }

        int[] ids = ShaderManager.create(vertexShader, fragmentShader);
        int vsId = ids[0];
        int fgId = ids[1];
        
        // attacher les shader compilés au programme
        ShaderManager.programId = GL20.glCreateProgram();
        GL20.glAttachShader(ShaderManager.programId, vsId);
        GL20.glAttachShader(ShaderManager.programId, fgId);

        // vérifie que les sorties du vertex shader corresponde aux entrées du fragment shader
        GL20.glLinkProgram(ShaderManager.programId);
        int success = GL20.glGetProgrami(ShaderManager.programId, GL20.GL_LINK_STATUS);
        if (success == GL20.GL_FALSE){
            throw new ShaderManagerError(GL20.glGetProgramInfoLog(ShaderManager.programId));
        }

        // une fois les vertex liés on détache et supprime les shaders
        GL20.glDetachShader(ShaderManager.programId, vsId);
        GL20.glDeleteShader(vsId);

        GL20.glDetachShader(ShaderManager.programId, fgId);
        GL20.glDeleteShader(fgId);
    }

    // pareil mais pour les shaders de texte
    @OpenGLThread
    public static void createTextShader() throws ShaderManagerError{
        String vertexTextShader;
        String fragmentTextShader;
        try{
            vertexTextShader = Files.readString(Paths.get("res/Shaders/vertex_text.glsl"));
            fragmentTextShader = Files.readString(Paths.get("res/Shaders/fragment_text.glsl"));
        }catch(IOException e){
            e.printStackTrace();
            return;
        }

        int[] ids = ShaderManager.create(vertexTextShader, fragmentTextShader);
        int vsId = ids[0];
        int fgId = ids[1];

        // attacher les shader compilés au programme
        ShaderManager.programTextId = GL20.glCreateProgram();
        GL20.glAttachShader(ShaderManager.programTextId, vsId);
        GL20.glAttachShader(ShaderManager.programTextId, fgId);

        // vérifie que les sorties du vertex shader corresponde aux entrées du fragment shader
        GL20.glLinkProgram(ShaderManager.programTextId);
        int success = GL20.glGetProgrami(ShaderManager.programTextId, GL20.GL_LINK_STATUS);
        if (success == GL20.GL_FALSE){
            throw new ShaderManagerError(GL20.glGetProgramInfoLog(ShaderManager.programTextId));
        }

        // une fois les vertex liés on détache et supprime les shaders
        GL20.glDetachShader(ShaderManager.programTextId, vsId);
        GL20.glDeleteShader(vsId);

        GL20.glDetachShader(ShaderManager.programTextId, fgId);
        GL20.glDeleteShader(fgId);
    }

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
            throw new ShaderManagerError(GL20.glGetShaderInfoLog(vsId));
        }

        int fgId = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
        GL20.glShaderSource(fgId, fragmentShader);
        GL20.glCompileShader(fgId);
        success = GL20.glGetShaderi(fgId, GL20.GL_COMPILE_STATUS);
        if (success == GL20.GL_FALSE){
            throw new ShaderManagerError(GL20.glGetShaderInfoLog(fgId));
        }

        return new int[]{vsId, fgId};
    }

    @OpenGLThread
    public static void useShader(){
        GL20.glUseProgram(ShaderManager.programId);
    }
    
    @OpenGLThread
    public static void useTextShader(){
        GL20.glUseProgram(ShaderManager.programTextId);
    }

    /**
     * Envoie la projection au shader
     * @param proj FloatBuffer
     */
    @OpenGLThread 
    public static void runProjection(FloatBuffer proj){
        proj.rewind();
        int locationProj = GL20.glGetUniformLocation(ShaderManager.programId, "projection");
        GL20.glUniformMatrix4fv(locationProj, false, proj);        
    }

    /**
     * Envoie la viewMatrix de la caméra au shader
     * @param proj FloatBuffer
     */
    @OpenGLThread 
    public static void runView(FloatBuffer view){
        view.rewind();
        int locationView = GL20.glGetUniformLocation(ShaderManager.programId, "view");
        GL20.glUniformMatrix4fv(locationView, false, view);
    }
    /**
     * Envoie le model des cubes au shader
     * @param proj FloatBuffer
     */
    @OpenGLThread
    public static void runModel(FloatBuffer model){
        model.rewind();
        int locationModel = GL20.glGetUniformLocation(ShaderManager.programId, "model");
        GL20.glUniformMatrix4fv(locationModel, false, model);
    }

    /**
     * Envoie la couleur au fragment shader
     * @param red float
     * @param green float
     * @param blue float
     * @param alpha float
     */
    @OpenGLThread
    public static void runFragmentColor(float red, float green, float blue, float alpha){
        int locationColor = GL20.glGetUniformLocation(ShaderManager.programId, "color");
        GL20.glUniform4f(locationColor, red, green, blue, alpha);
    }

    /**
     * Envoie la matrice orthonormé au shader de texte
     * @param width int
     * @param height int
     */
    @OpenGLThread
    public static void runTextOrtho(FloatBuffer buffer, int width, int height){
        Matrix4f ortho = new Matrix4f().ortho(0, width, height, 0, -1, 1);
        ortho.get(buffer);
        int locationOrtho = GL20.glGetUniformLocation(ShaderManager.programTextId, "ortho");
        GL20.glUniformMatrix4fv(locationOrtho, false, buffer);
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
        int locationColor = GL20.glGetUniformLocation(ShaderManager.programTextId, "color");
        GL20.glUniform4f(locationColor, red, green, blue, alpha);
    }

    @OpenGLThread
    public static void closeProgramId(){
        GL20.glDeleteProgram(ShaderManager.programId);
    }

    @OpenGLThread
    public static void closeProgramTextId(){
        GL20.glDeleteProgram(ShaderManager.programTextId);
    }

    @OpenGLThread
    public static int getProgramId(){
        return ShaderManager.programId;
    }

    @OpenGLThread
    public static int getProgramTextId(){
        return ShaderManager.programTextId;
    }

    /**
     * Créer les buffers en les remplissant avant les données données
     * @param vertex FloatBuffer, sommet à affiché
     * @param indices IntBuffer, indice sur l'ordre des sommets
     * @param packetSize int, taille des paquets de données
     * @return
     */
    @OpenGLThread
    public static int[] createVBOVAOEBO(FloatBuffer vertex, IntBuffer indices, int packetSize){
        int[] ids = ShaderManager.createBuffer(vertex, indices);

        // 3. Dire au VAO comment lire le VBO
        GL20.glVertexAttribPointer(0, 3, GL20.GL_FLOAT, false, packetSize * Float.BYTES, 0);
        GL20.glEnableVertexAttribArray(0);
        
        GL20.glVertexAttribPointer(1, 2, GL20.GL_FLOAT, false, packetSize * Float.BYTES, 3 * Float.BYTES);
        GL20.glEnableVertexAttribArray(1);

        GL20.glVertexAttribPointer(2, 1, GL20.GL_FLOAT, false, packetSize * Float.BYTES, 5 * Float.BYTES);
        GL20.glEnableVertexAttribArray(2);

        // 4. Unbind
        GL15.glBindBuffer(GL20.GL_ARRAY_BUFFER, 0);
        GL30.glBindVertexArray(0);

        // retourne les références des buffers
        activeVAOCount++;
        return ids;
    }

    // pareil que en haut mais pour les shaders de texte
    @OpenGLThread
    public static int[] createTextVBOVAOEBO(FloatBuffer vertex, IntBuffer indices){
        int[] ids = ShaderManager.createBuffer(vertex, indices);

        // 3. Dire au VAO comment lire le VBO
        GL20.glVertexAttribPointer(0, 2, GL20.GL_FLOAT, false, 4 * Float.BYTES, 0);
        GL20.glEnableVertexAttribArray(0);

        GL20.glVertexAttribPointer(1, 2, GL20.GL_FLOAT, false, 4 * Float.BYTES, 2 * Float.BYTES);
        GL20.glEnableVertexAttribArray(1);

        // 4. Unbind
        GL15.glBindBuffer(GL20.GL_ARRAY_BUFFER, 0);
        GL30.glBindVertexArray(0);

        // retourne les références des buffers
        activeVAOCount++;
        return ids;
    }

    private static int[] createBuffer(FloatBuffer vertex, IntBuffer indices){
        // VAO = configuration de comment lire un vbo
        // 1. VAO, créer, binder et uploader
        int vao = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vao);

        // VBO = données brut à afficher
        // 2. VBO, créer, binder et uploader
        int vbo = GL20.glGenBuffers();
        GL15.glBindBuffer(GL20.GL_ARRAY_BUFFER, vbo); 
        GL15.glBufferData(GL20.GL_ARRAY_BUFFER, vertex, GL20.GL_STATIC_DRAW);

        // EBO = les indices pour lire les sommets pour évité les duplications de sommets
        // 3. EBO — doit être bindé PENDANT que le VAO est actif
        int ebo = GL20.glGenBuffers();
        GL15.glBindBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER, ebo);
        GL15.glBufferData(GL20.GL_ELEMENT_ARRAY_BUFFER, indices, GL20.GL_STATIC_DRAW);

        return new int[]{vbo, vao, ebo};
    }

    public static void updateTextVBOEBO(FloatBuffer vertex, IntBuffer indices, int vbo, int vao, int ebo){
        // update les données sans recréer les buffers
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, vertex);

        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ebo);
        GL15.glBufferSubData(GL15.GL_ELEMENT_ARRAY_BUFFER, 0, indices);
    }

    @OpenGLThread
    public static void deleteVBOVAOEBO(int[] ids){
        GL30.glDeleteVertexArrays(ids[0]); // vao
        GL20.glDeleteBuffers(ids[1]); // vbo
        GL20.glDeleteBuffers(ids[2]); // ebo 
        activeVAOCount--;
    }
}
