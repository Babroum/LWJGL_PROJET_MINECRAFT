package demo.Meshing;

import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import demo.Annotations.OpenGLThread;
import demo.Exceptions.Empty;
import demo.Geometry.Chunk;
import demo.Managers.BuffersManager;
import demo.Managers.TexturesManager;

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
    // ids des vao vbo
    private int[] ids; 

    /**
     * Ajoute les mailles d'un cube à la maille du chunk <br>
     * <b>Vertex Packing Ici</b>
     * @param array
     */
    //[ 0000 | 00000000 | 0000 | 0 | 0 | 000 | 00000000000 ]
    //    x        y        z    u   v   dir    textureId
    public void addObjectVertexToArray(int[] array){
        for(int i = 0; i < array.length; i++){
            int packed = 0;
            // |= c'est le OU permet de combiner sans supprimer les valeurs
            // & c'est un ET il applique un masque pour la sécurité au cas ou par exemple on recoit 5 bits au lieu de 4
            packed |= (array[0] & 0xF)         << 28; // 4 bits, décale de 28 bits (donc prend les 4 premiers bits, 32 - 28 = 4)
            packed |= (array[1] & 0xFF)        << 20; // 8 bits
            packed |= (array[2] & 0xF)         << 16; // 4 bits
            packed |= (array[3] & 0x1)         << 15; // 1 bit
            packed |= (array[4] & 0x1)         << 14; // 1 bit
            packed |= (array[5] & 0x7)         << 11; // 3 bits
            packed |= (array[7] & 0x7FF);             // 11 bits

            vertexArrayPack[vertexCount] = packed;

            vertexCount++;
        } 
        this.faceCount++; 
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
        if (this.vertexCount == 0 ) throw new Empty("pas assez de sommets");
        this.vertexBufferPack.clear();
        for(int i : this.vertexArrayPack){
            this.vertexBufferPack.put(i);
        }
        this.vertexBufferPack.flip(); // remet la position du buffer à 0, sinon il lirai de la fin des données à la fin du buffer
    }

    /**
     * Permet d'ajouter un bloc et de vider puis remplir le buffer du chunk <br>
     * <b>Les données ne doivent pas être packé !</b>
     * @param array int[], la maille d'un bloc à ajouté
     */
    public void updateChunkVertexBufferWithArray(int[] array){
        this.addObjectVertexToArray(array);
        this.fillChunkVertexBufferWithArray();
    }

    /**
     * Créer les buffers sur la CG <br>
     * Définis les ids des buffers créer avec les sommets
     */
    @OpenGLThread
    public void createChunkVBOVAO(){
        this.ids = new int[2];
        this.ids = BuffersManager.createChunkVBOVAO(this.vertexBufferPack);
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
    public void drawChunkMesh() throws Empty{   
        TexturesManager.getTextureRender().draw();
        GL30.glBindVertexArray(ids[1]);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, this.faceCount * 6); // nb face * nb int par face
        GL30.glBindVertexArray(0);        
    } 
}
