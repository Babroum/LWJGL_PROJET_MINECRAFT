package demo.Meshing;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import demo.Enum.Directions;
import demo.Annotations.ChunkThread;
import demo.Annotations.OpenGLThread;
import demo.Events.KeyboardInput;
import demo.Exceptions.ArrayEmpty;
import demo.Exceptions.Empty;
import demo.Geometry.SubChunk;
import demo.Managers.BuffersManager;
import demo.Managers.TexturesManager;
import demo.Rendering.Render;

/**
 * Une Maille décrivant l'ensemble des ..
 */
// PERFORMANCE CALCULER SEULEMENT SUR LE PACKING ET PAS TOUT LE RESTE
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
public class SubChunkMesh {

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
    @ChunkThread
    public void addObjectVertexToArray(int[] array, int cubeX, int cubeY, int cubeZ) throws ArrayEmpty{
        if (array.length < 7){
            throw new ArrayEmpty();
        }
        int addedVertices = 0; // compte les vertices ajoutés lors de cet appel
        for(int i = 0; i < array.length; i += 7){
            int packed = 0;
            // |= c'est le OU permet de combiner sans supprimer les valeurs
            // & c'est un ET il applique un masque pour la sécurité au cas ou par exemple on recoit 6 bits au lieu de 5

            packed |= ((array[i]   + cubeX) & 0xF) << 28; // 4 bits, 32 - 4 = 28
            packed |= ((array[i+1] + cubeY) & 0xF) << 24; // 4 bits
            packed |= ((array[i+2] + cubeZ) & 0xF) << 20; // 4 bits
            packed |= (array[i+3] & 0xF)           << 16; // 4 bits UV u
            packed |= (array[i+4] & 0xF)           << 12; // 4 bits UV v
            packed |= (array[i+5] & 0x7)           << 9;  // 3 bits direction
            packed |= (array[i+6] & 0x1FF);               // 9 bits textureId

            this.vertexArrayPack[this.vertexCount] = packed;

            this.vertexCount++;
            addedVertices++;
        }    
        this.faceCount += addedVertices / 6; // /6 car 6 sommets par face
    }

    /**
     * Calcule les sommets du quad
     * @param quad
     * @param textureId
     */
    @ChunkThread
    public void addQuadVertexToArray(Quad quad, int textureId){
        int x = quad.x;
        int y = quad.y;
        int z = quad.z;
        int w = quad.width;
        int h = quad.height;
        int dir = quad.direction.ordinal();
        // on ne peut plus utilisé cube car la taille varie
        int[][] vertices;
        switch(quad.direction){
            case NORTH:
                vertices = new int[][]{
                    {x,     y,     z, 0, h, dir, textureId},
                    {x + w, y,     z, w, h, dir, textureId},
                    {x + w, y + h, z, w, 0, dir, textureId},
                    {x + w, y + h, z, w, 0, dir, textureId},
                    {x,     y + h, z, 0, 0, dir, textureId},
                    {x,     y,     z, 0, h, dir, textureId}
                };
                break;
            case SOUTH:
                vertices = new int[][]{
                    {x + w, y,     z, 0, h, dir, textureId},
                    {x,     y,     z, w, h, dir, textureId},
                    {x,     y + h, z, w, 0, dir, textureId},
                    {x,     y + h, z, w, 0, dir, textureId},
                    {x + w, y + h, z, 0, 0, dir, textureId},
                    {x + w, y,     z, 0, h, dir, textureId}
                };
                break;
            case EAST:
                vertices = new int[][]{
                    {x, y,     z,     0, h, dir, textureId},
                    {x, y,     z + w, w, h, dir, textureId},
                    {x, y + h, z + w, w, 0, dir, textureId},
                    {x, y + h, z + w, w, 0, dir, textureId},
                    {x, y + h, z,     0, 0, dir, textureId},
                    {x, y,     z,     0, h, dir, textureId}
                };
                break;
            case WEST:
                vertices = new int[][]{
                    {x, y,     z + w, 0, h, dir, textureId},
                    {x, y,     z,     w, h, dir, textureId},
                    {x, y + h, z,     w, 0, dir, textureId},
                    {x, y + h, z,     w, 0, dir, textureId},
                    {x, y + h, z + w, 0, 0, dir, textureId},
                    {x, y,     z + w, 0, h, dir, textureId}
                };
                break;
            case UP:
                vertices = new int[][]{
                    {x,     y, z,     0, h, dir, textureId},
                    {x + w, y, z,     w, h, dir, textureId},
                    {x + w, y, z + h, w, 0, dir, textureId},
                    {x + w, y, z + h, w, 0, dir, textureId},
                    {x,     y, z + h, 0, 0, dir, textureId},
                    {x,     y, z,     0, h, dir, textureId}
                };
                break;
            case DOWN:
                vertices = new int[][]{
                    {x + w, y, z,     0, h, dir, textureId},
                    {x,     y, z,     w, h, dir, textureId},
                    {x,     y, z + h, w, 0, dir, textureId},
                    {x,     y, z + h, w, 0, dir, textureId},
                    {x + w, y, z + h, 0, 0, dir, textureId},
                    {x + w, y, z,     0, h, dir, textureId}
                };
                break;
            default:
                return;
        }

        for(int[] v : vertices){
            int packed = 0;
            packed |= (v[0] & 0xF) << 28;
            packed |= (v[1] & 0xF) << 24;
            packed |= (v[2] & 0xF) << 20;
            packed |= (v[3] & 0xF) << 16;
            packed |= (v[4] & 0xF) << 12;
            packed |= (v[5] & 0x7) << 9;
            packed |= (v[6] & 0x1FF);
            this.vertexArrayPack[this.vertexCount] = packed;
            this.vertexCount++;
        }
        this.faceCount++;
    }

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
    @ChunkThread
    public void addObjectFaceVertexToArray(int[] array, int cubeX, int cubeY, int cubeZ) throws ArrayEmpty{
        if (array.length < 7){
            throw new ArrayEmpty();
        }
        // on garde la face
        for(int i = 0; i < array.length; i += 7){
            int packed = 0;
            // |= c'est le OU permet de combiner sans supprimer les valeurs
            // & c'est un ET il applique un masque pour la sécurité au cas ou par exemple on recoit 6 bits au lieu de 5
            packed |= ((array[i]   + cubeX) & 0x1F) << 27; // 5 bits, décale de 27 bits (donc prend les 5 premiers bits, 32 - 27 = 5)
            packed |= ((array[i+1] + cubeY) & 0xFF) << 19; // 8 bits
            packed |= ((array[i+2] + cubeZ) & 0x1F) << 14; // 5 bits
            packed |= (array[i+3] & 0x1)            << 13; // 1 bit
            packed |= (array[i+4] & 0x1)            << 12; // 1 bit
            packed |= (array[i+5] & 0x7)   << 9;  // 3 bits
            packed |= (array[i+6] & 0x1FF);                // 9 bits

            this.vertexArrayPack[this.vertexCount] = packed;

            this.vertexCount++;;
        }    
        this.faceCount++;
    }

    /**
     * Définit la taille du buffer pour les vertexs
     */
    @ChunkThread
    public void setupSubChunkBuffers(){
        this.vertexCount = 0;
        this.vertexArrayPack = new int[SubChunk.MAXBLOCK * 4 * 6];
        this.vertexBufferPack = BufferUtils.createIntBuffer(SubChunk.MAXBLOCK * 4 * 6); // 4096 blocks max * 4 sommets (1 int par sommet) * 6 faces
    }

    /**
     * Remplis le buffer avec le tableau de sommet
     */
    @ChunkThread
    public void fillSubChunkVertexBufferWithArray(){
        this.vertexBufferPack.clear();
        for(int i = 0; i < this.vertexCount; i++){
            this.vertexBufferPack.put(this.vertexArrayPack[i]);
        }
        this.vertexBufferPack.flip(); // remet la position du buffer à 0, sinon il lirai de la fin des données à la fin du buffer
    }

    /**
     * Créer les buffers sur la CG <br>
     * Définis les ids des buffers créer avec les sommets
     */
    @OpenGLThread
    public void createSubChunkVBOVAO(){
        this.ids = new int[4];
        int[] result = BuffersManager.createChunkVBOVAO(this.vertexBufferPack);
        this.ids[0] = result[0];
        this.ids[1] = result[1];
    }

    @OpenGLThread
    public void deleteSubChunkVBOVAO(){
        BuffersManager.deleteVBOVAO(this.ids);
    }

    /**
     * Dessine les bordures d'un chunk avec des lignes
     */
    @OpenGLThread
    public void createSubChunkBorderVBOVAO(){      
        // 4 sommets du contour du chunk en bas
        int[] lines = {
            // bas
            0, 0, 0,    16, 0, 0,
            16, 0, 0,   16, 0, 16,
            16, 0, 16,  0, 0, 16,
            0, 0, 16,   0, 0, 0,
            // verticales
            0, 0, 0,    0, 16, 0,
            16, 0, 0,   16, 16, 0,
            16, 0, 16,  16, 16, 16,
            0, 0, 16,   0, 16, 16,
            // haut
            0, 16, 0,  16, 16, 0,
            16, 16, 0, 16, 16, 16,
            16, 16, 16,0, 16, 16,
            0, 16, 16, 0, 16, 0
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
     * Pas utile pour l'instant
     */
    @OpenGLThread
    @Deprecated
    public void updateSubChunkVBOVAO(){
        BuffersManager.updateChunkVBO(this.vertexBufferPack, this.ids[0]);
    }

    /**
     * Dessine la maille si les vbo et vao ont bien étaient initialisé
     * @throws Empty
     */
    @OpenGLThread
    public void drawSubChunkMesh(FloatBuffer buffer) throws Empty{   
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
