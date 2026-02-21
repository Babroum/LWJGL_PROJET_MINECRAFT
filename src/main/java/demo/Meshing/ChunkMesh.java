package demo.Meshing;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import demo.Annotations.OpenGLThread;
import demo.Events.KeyboardInput;
import demo.Exceptions.ArrayEmpty;
import demo.Exceptions.Empty;
import demo.Geometry.Chunk;
import demo.Managers.BuffersManager;
import demo.Managers.TexturesManager;
import demo.Rendering.Render;

/**
 * Une Maille décrivant l'ensemble des ..
 */
// IL NE VA PLUS AVOIR D'EBO CAR
// SANS VERTEX PACKING (AVEC EBO) : (AVANT)
// 4 sommets * 6 ints = 24 ints par faces
// 6 indices * 1 int = 6 ints par faces
// total = 30 ints par faces, 180 ints par cube
// 180 * 65280(max cube par chunk) * 15 * 15 (render distance) = 2.6 milliards d'ints
// 32go de ram = 8.6 milliards d'int 

// SANS VERTEX PACKING (SANS EBO) :
// 6 sommets * 6 ints = 36 ints par faces
// total = 36 ints par faces, 216 ints par cube
// 216 * 65280(max cube par chunk) * 15 * 15 (render distance) = 3.2 milliards d'ints

// AVEC VERTEX PACKING (AVEC EBO)
// 4 sommets * 1 int packé = 4 ints par faces
// 6 indices * 1 int = 6 ints par faces
// total = 10 ints par faces, 60 ints par cube
// 60 * 65280(max cube par chunk) * 15 * 15 (render distance) = 881 millions d'ints

// AVEC VERTEX PACKING (SANS EBO):
// 6 sommets * 1 int packé = 6 ints par faces
// total = 6 ints par faces, 36 ints par cube
// 36 * 65280(max cube par chunk) * 15 * 15 (render distance) = 529 millions d'ints

// 10 go de ram = 2.6 milliards d'ints 
// 2 684 354 560 - 36 * 65280 * 1000(nb chunks chargé) = 334 274 560 d'ints restants sur 10go de ram
// ~8.7 Go utilisés

// meilleur : VERTEX PACKING SANS EBO
public class ChunkMesh {

    // Tout les sommets de tout les objets en IntBuffer pour opengl pack
    private IntBuffer vertexBufferPack;
    // Tout les sommets de tout les objets avant la transformation en IntBuffer pack
    private int[] vertexArrayPack;
    /**
     * Le nombre de sommet dans la maille
     */
    private int vertexCount;
    /**
     * Le nombre de face dans la maille
     */
    private int faceCount;
    // ids des vao vbo, et le vbo et vao du chunkborder
    private int[] ids; 

    /**
     * Ajoute les mailles d'un cube à la maille du chunk <br>
     * <b>Vertex Packing Ici</b>
     * @param array
     * @param cubeX
     * @param cubeY
     * @param cubeZ
     */
    //[ 00000 | 00000000 | 00000 | 0 | 0 | 000 | 000000000 ]
    //    x        y         z     u   v   dir   textureId
    // 512 textures max
    public void addObjectVertexToArray(int[] array, int cubeX, int cubeY, int cubeZ) throws ArrayEmpty{
        if (array.length < 7){
            throw new ArrayEmpty();
        }
        int addedVertices = 0; // compte les vertices ajoutés lors de cet appel
        for(int i = 0; i < array.length; i += 7){
            int packed = 0;
            // |= c'est le OU permet de combiner sans supprimer les valeurs
            // & c'est un ET il applique un masque pour la sécurité au cas ou par exemple on recoit 6 bits au lieu de 5
            packed |= ((array[i]   + cubeX) & 0x1F) << 27; // 5 bits, décale de 27 bits (donc prend les 5 premiers bits, 32 - 27 = 5)
            packed |= ((array[i+1] + cubeY) & 0xFF) << 19; // 8 bits
            packed |= ((array[i+2] + cubeZ) & 0x1F) << 14; // 5 bits
            packed |= (array[i+3] & 0x1)            << 13; // 1 bit
            packed |= (array[i+4] & 0x1)            << 12; // 1 bit
            packed |= (array[i+5] & 0x7)            << 9;  // 3 bits
            packed |= (array[i+6] & 0x1FF);                // 9 bits

            this.vertexArrayPack[this.vertexCount] = packed;

            this.vertexCount++;
            addedVertices++;
        }    
        this.faceCount += addedVertices / 6; // /6 car 6 sommets par face
    }

    /**
     * Définit la taille du buffer pour les vertexs
     */
    public void setupChunkBuffers(){
        this.vertexCount = 0;
        this.vertexArrayPack = new int[Chunk.MAXBLOCK * 4 * 6];
        this.vertexBufferPack = BufferUtils.createIntBuffer(Chunk.MAXBLOCK * 4 * 6); // 65536 blocks max * 4 sommets (1 int par sommet) * 6 faces
    }

    /**
     * Remplis le buffer avec le tableau de sommet
     */
    public void fillChunkVertexBufferWithArray(){
        this.vertexBufferPack.clear();
        for(int i = 0; i < this.vertexCount; i++){
            this.vertexBufferPack.put(this.vertexArrayPack[i]);
        }
        this.vertexBufferPack.flip(); // remet la position du buffer à 0, sinon il lirai de la fin des données à la fin du buffer
    }

    /**
     * Permet d'ajouter un bloc et de vider puis remplir le buffer du chunk <br>
     * <b>Les données ne doivent pas être packé !</b>
     * @param array int[], la maille d'un bloc à ajouté
     * @param cubeX
     * @param cubeY
     * @param cubeZ
     */
    public void updateChunkVertexBufferWithArray(int[] array, int cubeX, int cubeY, int cubeZ){
        this.addObjectVertexToArray(array, cubeX, cubeY, cubeZ);
        this.fillChunkVertexBufferWithArray();
        this.updateChunkVBOVAO();
    }

    /**
     * Créer les buffers sur la CG <br>
     * Définis les ids des buffers créer avec les sommets
     */
    @OpenGLThread
    public void createChunkVBOVAO(){
        this.ids = new int[4];
        int[] result = BuffersManager.createChunkVBOVAO(this.vertexBufferPack);
        this.ids[0] = result[0];
        this.ids[1] = result[1];
    }

    /**
     * Dessine les bordures d'un chunk avec des lignes
     */
    @OpenGLThread
    public void createChunkBorderVBOVAO(){      
        // 4 sommets du contour du chunk en bas
        int[] lines = {
            // bas
            0, 0, 0,    16, 0, 0,
            16, 0, 0,   16, 0, 16,
            16, 0, 16,  0, 0, 16,
            0, 0, 16,   0, 0, 0,
            // verticales
            0, 0, 0,    0, 255, 0,
            16, 0, 0,   16, 255, 0,
            16, 0, 16,  16, 255, 16,
            0, 0, 16,   0, 255, 16,
            // haut
            0, 255, 0,  16, 255, 0,
            16, 255, 0, 16, 255, 16,
            16, 255, 16,0, 255, 16,
            0, 255, 16, 0, 255, 0
        };   
        
        IntBuffer linesBuffer = BufferUtils.createIntBuffer(lines.length);
        
        for(int i = 0; i < lines.length; i+=3){
            int packed = 0;
            packed |= ((lines[i]  ) & 0x1F) << 27;
            packed |= ((lines[i+1]) & 0xFF) << 19;
            packed |= ((lines[i+2]) & 0x1F) << 14;
            linesBuffer.put(packed);
        }
        linesBuffer.flip();

        int[] chunkBorderIds = BuffersManager.createChunkVBOVAO(linesBuffer);
        this.ids[2] = chunkBorderIds[0];
        this.ids[3] = chunkBorderIds[1];
    }

    /**
     * Met à jour le vbo du chunk
     */
    @OpenGLThread
    public void updateChunkVBOVAO(){
        BuffersManager.updateChunkVBO(this.vertexBufferPack, this.ids[0]);
    }

    /**
     * Dessine la maille si les vbo et vao ont bien étaient initialisé
     * @throws Empty
     */
    @OpenGLThread
    public void drawChunkMesh(FloatBuffer buffer) throws Empty{   
        if (this.ids == null) throw new Empty("les vbo et vao ne sont pas initialisé");
        TexturesManager.getTextureRender().draw();
        if(KeyboardInput.isChunkBorderShow){
            GL30.glBindVertexArray(ids[3]);
            GL11.glDrawArrays(GL11.GL_LINES, 0, 24);
            GL30.glBindVertexArray(0);  
        }
        GL30.glBindVertexArray(ids[1]);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, this.vertexCount);
        GL30.glBindVertexArray(0);        
        // enregistre le nombre d'appele de dessin 
        Render.monitor.incrementDrawCalls();
        // et le nombre de triangle dessiné
        Render.monitor.addTriangles(this.faceCount * 2); // nb face * nb triangle par face     
    } 
}
