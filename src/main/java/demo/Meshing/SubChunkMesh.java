package demo.Meshing;

import java.nio.IntBuffer;

import org.lwjgl.system.MemoryUtil;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import demo.Annotations.ChunkThread;
import demo.Annotations.OpenGLThread;
import demo.Events.KeyboardInput;
import demo.Exceptions.ArrayEmpty;
import demo.Exceptions.Empty;
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

    private final int[][] vertsBuffer = new int[6][7];
    private final int[] arrayBuffer   = new int[48];

    /**
     * Le nombre de sommet dans la maille
     */
    private int vertexCount;

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
    //[ 00000 | 00000 | 00000 | 0 | 0 | 000 | 000000000000 ]
    //    x        y      z     u   v   dir   textureId, quad.ao
    // 4096 textures max
    @ChunkThread
    public void addObjectVertexToArray(int[] array, int cubeX, int cubeY, int cubeZ) throws ArrayEmpty{
        if (array.length < 7){
            throw new ArrayEmpty();
        }
        for(int i = 0; i < array.length; i += 7){
            int packed = 0;
            // |= c'est le OU permet de combiner sans supprimer les valeurs
            // le x max est 15 donc pourquoi pas avoir << 28 car 15 = 1111 donc 4 bits
            // le centre du cube n'est pas centré donc pour avoir le sommet le plus loin on fait x + 1 donc 15 + 1 = 16 ce qui dépasse 4bits
            packed |= (array[i]   + cubeX)  << 27; // 5 bits, décale de 27 bits (donc prend les 5 premiers bits, 32 - 27 = 5)
            packed |= (array[i+1] + cubeY)  << 22; // 5 bits
            packed |= (array[i+2] + cubeZ)  << 17; // 5 bits
            packed |= (array[i+3])          << 16; // 1 bit
            packed |= (array[i+4])          << 15; // 1 bit
            packed |=  array[i+5]           << 12; // 3 bits
            packed |=  array[i+6];                 // 12 bits

            this.vertexArrayPack[this.vertexCount * 2] = packed;

            this.vertexCount++;
        }    
    }

    /**
     * Ajoute les mailles d'un cube à la maille du chunk <br>
     * <b>Vertex Packing Ici</b>
     * @param array
     * @param cubeX
     * @param cubeY
     * @param cubeZ
     */
    //[ 00000 | 00000 | 00000 | 0 | 0 | 000 | 000000000000 ]
    //    x       y       z     u   v   dir     textureId
    // 4096 textures max
    @ChunkThread
    public void addObjectFaceVertexToArray(int[] array, int cubeX, int cubeY, int cubeZ, int w, int h, int light) throws ArrayEmpty{
        if (array.length < 8){
            throw new ArrayEmpty();
        }
        // on garde la face
        for(int i = 0; i < array.length; i += 8){
            int packed = 0;
            // |= c'est le OU permet de combiner sans supprimer les valeurs
            // le x max est 15 donc pourquoi pas avoir << 28 car 15 = 1111 donc 4 bits
            // le centre du cube n'est pas centré donc pour avoir le sommet le plus loin on fait x + 1 donc 15 + 1 = 16 ce qui dépasse 4bits
            packed |= (array[i]   + cubeX)  << 27; // 5 bits, décale de 27 bits (donc prend les 5 premiers bits, 32 - 27 = 5)
            packed |= (array[i+1] + cubeY)  << 22; // 5 bits
            packed |= (array[i+2] + cubeZ)  << 17; // 5 bits
            packed |= (array[i+3])          << 16; // 1 bit
            packed |= (array[i+4])          << 15; // 1 bit
            packed |=  array[i+5]           << 12; // 3 bits
            packed |=  array[i+6];                 // 12 bits

            // coordonnées UV 
            int packed2 = 0;
            packed2 |= (w)          << 27; // 5 bits
            packed2 |= (h)          << 22; // 5 bits
            packed2 |= (array[i+7] & 0x3) << 20; // 2 bits valeur 0-3
            packed2 |= (light & 0xF)    << 16; // 4 bits valeur 0-15
 
            this.vertexArrayPack[this.vertexCount * 2] = packed;
            this.vertexArrayPack[this.vertexCount * 2 + 1] = packed2;

            this.vertexCount++;;
        }    
    }


    @ChunkThread
    public void addQuadVertexToArray(Quad quad, int textureId, int light) {
        int x = quad.x;
        int y = quad.y;
        int z = quad.z;
        int w = quad.width;
        int h = quad.height;
        int d = quad.direction.ordinal();

        // Les 4 coins : A=origine, B=+width, C=+height, D=+width+height
        // 2 triangles sans EBO : A-B-D, A-D-C
        int[][] verts = this.vertsBuffer; // 6 sommets * {x, y, z, u, v, dir, texId, ao}

        switch(quad.direction){
            case SOUTH:
                verts[0] = new int[]{x,   y,   z, 1, 1, d, textureId, quad.ao0, quad.light0};
                verts[1] = new int[]{x+w, y,   z, 0, 1, d, textureId, quad.ao1, quad.light1};
                verts[2] = new int[]{x+w, y+h, z, 0, 0, d, textureId, quad.ao3};
                verts[3] = new int[]{x+w, y+h, z, 0, 0, d, textureId, quad.ao3};
                verts[4] = new int[]{x,   y+h, z, 1, 0, d, textureId, quad.ao2};
                verts[5] = new int[]{x,   y,   z, 1, 1, d, textureId, quad.ao0};
                break;
            case NORTH:
                verts[0] = new int[]{x+w, y,   z+1, 1, 1, d, textureId, quad.ao1}; // ← ao1
                verts[1] = new int[]{x,   y,   z+1, 0, 1, d, textureId, quad.ao0}; // ← ao0
                verts[2] = new int[]{x,   y+h, z+1, 0, 0, d, textureId, quad.ao2}; // ← ao2
                verts[3] = new int[]{x,   y+h, z+1, 0, 0, d, textureId, quad.ao2};
                verts[4] = new int[]{x+w, y+h, z+1, 1, 0, d, textureId, quad.ao3}; // ← ao3
                verts[5] = new int[]{x+w, y,   z+1, 1, 1, d, textureId, quad.ao1};
                break;
            case WEST:
                verts[0] = new int[]{x+1, y,   z,   1, 1, d, textureId, quad.ao0};
                verts[1] = new int[]{x+1, y,   z+w, 0, 1, d, textureId, quad.ao1};
                verts[2] = new int[]{x+1, y+h, z+w, 0, 0, d, textureId, quad.ao3};
                verts[3] = new int[]{x+1, y+h, z+w, 0, 0, d, textureId, quad.ao3};
                verts[4] = new int[]{x+1, y+h, z,   1, 0, d, textureId, quad.ao2};
                verts[5] = new int[]{x+1, y,   z,   1, 1, d, textureId, quad.ao0};
                break;
            case EAST:
                verts[0] = new int[]{x, y,   z+w, 1, 1, d, textureId, quad.ao1}; // ← ao1
                verts[1] = new int[]{x, y,   z,   0, 1, d, textureId, quad.ao0}; // ← ao0
                verts[2] = new int[]{x, y+h, z,   0, 0, d, textureId, quad.ao2}; // ← ao2
                verts[3] = new int[]{x, y+h, z,   0, 0, d, textureId, quad.ao2};
                verts[4] = new int[]{x, y+h, z+w, 1, 0, d, textureId, quad.ao3}; // ← ao3
                verts[5] = new int[]{x, y,   z+w, 1, 1, d, textureId, quad.ao1};
                break;
            case UP: // y fixe à y+1, s'étend sur X et Z
                verts[0] = new int[]{x,   y+1, z,   0, 0, d, textureId, quad.ao0};
                verts[1] = new int[]{x+w, y+1, z,   1, 0, d, textureId, quad.ao1};
                verts[2] = new int[]{x+w, y+1, z+h, 1, 1, d, textureId, quad.ao3};
                verts[3] = new int[]{x+w, y+1, z+h, 1, 1, d, textureId, quad.ao3};
                verts[4] = new int[]{x,   y+1, z+h, 0, 1, d, textureId, quad.ao2};
                verts[5] = new int[]{x,   y+1, z,   0, 0, d, textureId, quad.ao0};
                break;
            case DOWN: // y fixe à y, Z inversé
                verts[0] = new int[]{x,   y, z+h, 0, 0, d, textureId, quad.ao0};
                verts[1] = new int[]{x+w, y, z+h, 1, 0, d, textureId, quad.ao1};
                verts[2] = new int[]{x+w, y, z,   1, 1, d, textureId, quad.ao3};
                verts[3] = new int[]{x+w, y, z,   1, 1, d, textureId, quad.ao3};
                verts[4] = new int[]{x,   y, z,   0, 1, d, textureId, quad.ao2};
                verts[5] = new int[]{x,   y, z+h, 0, 0, d, textureId, quad.ao0};
                break;
        }

        int[] array = this.arrayBuffer; // 6 sommets * 7 valeurs
        for(int i = 0; i < 6; i++){
            array[i*8]   = verts[i][0]; // x
            array[i*8+1] = verts[i][1]; // y
            array[i*8+2] = verts[i][2]; // z
            array[i*8+3] = verts[i][3]; // u
            array[i*8+4] = verts[i][4]; // v
            array[i*8+5] = d;           // dir
            array[i*8+6] = textureId;   // textureId
            array[i*8+7] = verts[i][7]; // ao
        }

        try {
            this.addObjectFaceVertexToArray(array, 0, 0, 0, w, h, light);
        } catch(ArrayEmpty e){ e.printStackTrace(); }
    }

    /**
     * Retourne le nombre de sommets dans le chunk
     * @return
     */
    public int getVertexCount(){
        return this.vertexCount;
    }

    /**
     * Définit la taille du buffer pour les vertexs
     * @param blockCount int
     */
    @ChunkThread
    public void setupSubChunkBuffers(int blockCount){
        this.vertexCount = 0;
        int needed = blockCount > 0 ? blockCount * 36 : 1;
        // Ne réalloue que si le tableau actuel est trop petit
        // 6 faces * 6 sommets par bloc = 36 vertices max par bloc visible
        // blockCount * 36 = taille exacte au pire cas
        if (this.vertexArrayPack == null || this.vertexArrayPack.length < needed){
            this.vertexArrayPack = new int[needed * 2];
        }
        this.vertexBufferPack = null;
    }

    /**
     * Remplis le buffer avec le tableau de sommet
     */
    @ChunkThread
    public void fillSubChunkVertexBufferWithArray(){
        // Libère l'ancien buffer off-heap s'il existe encore
        if (this.vertexBufferPack != null){
            MemoryUtil.memFree(this.vertexBufferPack);
            this.vertexBufferPack = null;
        }
        if (this.vertexCount == 0){
            this.vertexArrayPack = null; // libère le tableau Java aussi
            return;
        }
        // Alloue EXACTEMENT la taille nécessaire, off-heap
        this.vertexBufferPack = MemoryUtil.memAllocInt(this.vertexCount * 2);
        for(int i = 0; i < this.vertexCount; i++){
            this.vertexBufferPack.put(this.vertexArrayPack[i * 2]); // packed vertex
            this.vertexBufferPack.put(this.vertexArrayPack[i * 2 + 1]); // packed UV
        }
        this.vertexBufferPack.flip(); // remet la position du buffer à 0, sinon il lirai de la fin des données à la fin du buffer
        // Le tableau Java temporaire n'est plus nécessaire
        this.vertexArrayPack = null;
    }

    /**
     * Créer les buffers sur la CG <br>
     * Définis les ids des buffers créer avec les sommets
     */
    @OpenGLThread
    public void createSubChunkVBOVAO(){
        if (this.ids == null){
            this.ids = new int[4];
        }
        // Supprime les anciens buffers GPU avant d'en créer de nouveaux
        if (this.ids[0] != 0){ 
            BuffersManager.deleteVBOVAO(new int[]{this.ids[0], this.ids[1]});
            this.ids[0] = 0;
            this.ids[1] = 0;
        }
        // ids est initialisé, on peut return si chunk vide
        if (this.vertexBufferPack == null){
            return;
        }
        int[] result = BuffersManager.createSubChunkVBOVAO(this.vertexBufferPack);
        this.ids[0] = result[0];
        this.ids[1] = result[1];

        MemoryUtil.memFree(this.vertexBufferPack);
        this.vertexBufferPack = null;
    }

    @OpenGLThread
    public void deleteSubChunkVBOVAO(){
        if (this.ids == null) return;
        // ids[0]=vbo, ids[1]=vao, ids[2]=vbo border, ids[3]=vao border
        BuffersManager.deleteVBOVAO(new int[]{this.ids[0], this.ids[1]});
        this.deleteSubChunkBorderVBOVAO();
    }

    /**
     * Dessine les bordures d'un chunk avec des lignes
     */
    @OpenGLThread
    public void createSubChunkBorderVBOVAO(){      
        // 4 sommets du contour du chunk en bas
        int[] lines = {
            // bas
            0 , 0, 0 ,   16, 0, 0,
            16, 0, 0 ,   16, 0, 16,
            16, 0, 16,   0 , 0, 16,
            0 , 0, 16,   0 , 0, 0,
            // verticales
            0 , 0, 0 ,   0 , 16, 0,
            16, 0, 0 ,   16, 16, 0,
            16, 0, 16,   16, 16, 16,
            0 , 0, 16,   0 , 16, 16,
            // haut
            0 , 16, 0 ,  16, 16, 0,
            16, 16, 0 ,  16, 16, 16,
            16, 16, 16,  0 , 16, 16,
            0 , 16, 16,  0 , 16, 0
        };   
        
        IntBuffer linesBuffer = MemoryUtil.memAllocInt(lines.length);
        
        for(int i = 0; i < lines.length; i+=3){
            int packed = 0;
            packed |= ((lines[i]  ) & 0x1F) << 27;
            packed |= ((lines[i+1]) & 0x1F) << 22;
            packed |= ((lines[i+2]) & 0x1F) << 17;
            linesBuffer.put(packed);
        }
        linesBuffer.flip();

        int[] chunkBorderIds = BuffersManager.createVBOVAO(linesBuffer);
        this.ids[2] = chunkBorderIds[0];
        this.ids[3] = chunkBorderIds[1];
        MemoryUtil.memFree(linesBuffer);
    }

    @OpenGLThread
    public void deleteSubChunkBorderVBOVAO(){
        if (this.ids == null) return;
        if (this.ids[2] != 0){
            BuffersManager.deleteVBOVAO(new int[]{this.ids[2], this.ids[3]});
            this.ids[2] = 0;
            this.ids[3] = 0;
        }
    }

    /**
     * Retourne le l'id du vao de bordure si il existe
     * @return int
     */
    public int getBorderVAO(){
        return this.ids == null ? 0 : this.ids[2];
    }

    /**
     * Retourne le l'id du vao si il existe
     * @return int
     */
    public int getVAO() {
        return this.ids == null ? 0 : this.ids[1];
    }

    /**
     * Retourne le buffer avec les sommets packés
     * @return IntBuffer
     */
    public IntBuffer getVertexBufferPack(){
        return this.vertexBufferPack;
    }

    /**
     * Dessine la maille si les vbo et vao ont bien étaient initialisé
     * @throws Empty
     */
    @OpenGLThread
    public void drawSubChunkMesh() throws Empty{   
        if (this.ids == null) return; // un subchunk sans ids est juste vide, pas une erreur
        if (this.ids[1] == 0) return; // sub-chunk vide, rien à dessiner
        TexturesManager.getTextureRender().draw();

        // Vérifie que le border VAO existe avant de le dessiner
        if(KeyboardInput.isChunkBorderShow && this.ids[3] != 0){
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
        Render.monitor.addTriangles(this.vertexCount / 3); 
    } 

    // À appeler si le SubChunk est détruit avant que son buffer ait été uploadé
    public void cleanup(){
        if (this.vertexBufferPack != null){
            MemoryUtil.memFree(this.vertexBufferPack);
            this.vertexBufferPack = null;
        }
        this.vertexArrayPack = null;
    }
}
