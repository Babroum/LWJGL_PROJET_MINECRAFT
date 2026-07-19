package demo.Meshing;

import java.nio.IntBuffer;

import org.lwjgl.system.MemoryUtil;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import demo.Annotations.ChunkThread;
import demo.Annotations.OpenGLThread;
import demo.Events.KeyboardInput;
import demo.Managers.BuffersManager;
import demo.Rendering.Render;
import demo.Managers.TextureManager;

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

    private final int ITEM_TO_PACK = 9;

    private final int[][] vertsBuffer = new int[6][ITEM_TO_PACK];
    private final int[] arrayBuffer   = new int[6 * ITEM_TO_PACK];

    /**
     * Le nombre de sommet dans la maille
     */
    private int vertexCount;

    // ids des vao vbo, et le vbo et vao du chunkborder
    private int[] ids; 

    /**
     * Ajoute un quad au donnée du mesh.
     * @param quad Un quad
     * @param textureId Identifiant de texture du quad
     */
    @ChunkThread
    public void addQuadVertexToArray(Quad quad, int textureId) {
        int x = quad.x;
        int y = quad.y;
        int z = quad.z;
        int w = quad.width;
        int h = quad.height;
        int d = quad.depth;
        int dir = 0;

        int[][] verts = this.vertsBuffer; // 6 sommets * {x, y, z, u, v, dir, texId, ao, light}

        switch(quad.direction){
        case 0: dir=0; // face nord, profondeur = d
            setVert(verts,0,x+w,y,  z+d,1,0,quad.ao1,quad.skyLight,quad.blockLight);
            setVert(verts,1,x,  y,  z+d,0,0,quad.ao0,quad.skyLight,quad.blockLight);
            setVert(verts,2,x,  y+h,z+d,0,1,quad.ao2,quad.skyLight,quad.blockLight);
            setVert(verts,3,x,  y+h,z+d,0,1,quad.ao2,quad.skyLight,quad.blockLight);
            setVert(verts,4,x+w,y+h,z+d,1,1,quad.ao3,quad.skyLight,quad.blockLight);
            setVert(verts,5,x+w,y,  z+d,1,0,quad.ao1,quad.skyLight,quad.blockLight);
            break;
        case 1: dir=1;
            setVert(verts,0,x,  y,  z,0,0,quad.ao0,quad.skyLight,quad.blockLight);
            setVert(verts,1,x+w,y,  z,1,0,quad.ao1,quad.skyLight,quad.blockLight);
            setVert(verts,2,x+w,y+h,z,1,1,quad.ao3,quad.skyLight,quad.blockLight);
            setVert(verts,3,x+w,y+h,z,1,1,quad.ao3,quad.skyLight,quad.blockLight);
            setVert(verts,4,x,  y+h,z,0,1,quad.ao2,quad.skyLight,quad.blockLight);
            setVert(verts,5,x,  y,  z,0,0,quad.ao0,quad.skyLight,quad.blockLight);
            break;
        case 2: dir=2; // face est, largeur = d (axe Z)
            setVert(verts,0,x+w,y,  z,  0,0,quad.ao0,quad.skyLight,quad.blockLight);
            setVert(verts,1,x+w,y,  z+d,1,0,quad.ao1,quad.skyLight,quad.blockLight);
            setVert(verts,2,x+w,y+h,z+d,1,1,quad.ao3,quad.skyLight,quad.blockLight);
            setVert(verts,3,x+w,y+h,z+d,1,1,quad.ao3,quad.skyLight,quad.blockLight);
            setVert(verts,4,x+w,y+h,z,  0,1,quad.ao2,quad.skyLight,quad.blockLight);
            setVert(verts,5,x+w,y,  z,  0,0,quad.ao0,quad.skyLight,quad.blockLight);
            break;
        case 3: dir=3;
            setVert(verts,0,x,y,  z+d,1,0,quad.ao1,quad.skyLight,quad.blockLight);
            setVert(verts,1,x,y,  z,  0,0,quad.ao0,quad.skyLight,quad.blockLight);
            setVert(verts,2,x,y+h,z,  0,1,quad.ao2,quad.skyLight,quad.blockLight);
            setVert(verts,3,x,y+h,z,  0,1,quad.ao2,quad.skyLight,quad.blockLight);
            setVert(verts,4,x,y+h,z+d,1,1,quad.ao3,quad.skyLight,quad.blockLight);
            setVert(verts,5,x,y,  z+d,1,0,quad.ao1,quad.skyLight,quad.blockLight);
            break;
        case 4: dir=4; // face du haut
            setVert(verts,0,x,  y+h,z,  0,0,quad.ao0,quad.skyLight,quad.blockLight);
            setVert(verts,1,x+w,y+h,z,  1,0,quad.ao1,quad.skyLight,quad.blockLight);
            setVert(verts,2,x+w,y+h,z+d,1,1,quad.ao3,quad.skyLight,quad.blockLight);
            setVert(verts,3,x+w,y+h,z+d,1,1,quad.ao3,quad.skyLight,quad.blockLight);
            setVert(verts,4,x,  y+h,z+d,0,1,quad.ao2,quad.skyLight,quad.blockLight);
            setVert(verts,5,x,  y+h,z,  0,0,quad.ao0,quad.skyLight,quad.blockLight);
            break;
        case 5: dir=5; // face du bas
            setVert(verts,0,x,  y,z+d,0,1,quad.ao2,quad.skyLight,quad.blockLight);
            setVert(verts,1,x+w,y,z+d,1,1,quad.ao3,quad.skyLight,quad.blockLight);
            setVert(verts,2,x+w,y,z,  1,0,quad.ao1,quad.skyLight,quad.blockLight);
            setVert(verts,3,x+w,y,z,  1,0,quad.ao1,quad.skyLight,quad.blockLight);
            setVert(verts,4,x,  y,z,  0,0,quad.ao0,quad.skyLight,quad.blockLight);
            setVert(verts,5,x,  y,z+d,0,1,quad.ao2,quad.skyLight,quad.blockLight);
            break;
    }

        int[] array = this.arrayBuffer; // 6 sommets * 9 valeurs
        for(int i = 0; i < 6; i++){
            array[i*ITEM_TO_PACK]    = verts[i][0]; // x
            array[i*ITEM_TO_PACK+1]  = verts[i][1]; // y
            array[i*ITEM_TO_PACK+2]  = verts[i][2]; // z
            array[i*ITEM_TO_PACK+3]  = verts[i][3]; // u
            array[i*ITEM_TO_PACK+4]  = verts[i][4]; // v
            array[i*ITEM_TO_PACK+5]  = dir;         // dir

            array[i*ITEM_TO_PACK+6]  = verts[i][5]; // ao
            array[i*ITEM_TO_PACK+7]  = verts[i][6]; // skyLight
            array[i*ITEM_TO_PACK+8]  = verts[i][7]; // blocLight
        }
        try {
            this.addObjectFaceVertexToArray(array, w, h, d, textureId);
        } catch(Exception e){ e.printStackTrace(); }
    }

    // utilitaire pour simplifier
    private void setVert(int[][] v, int i, int x, int y, int z, int u, int vv, int ao, int sky, int block){
        v[i][0]=x;
        v[i][1]=y; 
        v[i][2]=z; 
        v[i][3]=u; 
        v[i][4]=vv;
        v[i][5]=ao; 
        v[i][6]=sky; 
        v[i][7]=block;
    }

    /**
     * Ajoute les mailles d'un cube à la maille du chunk <br>
     * <b>Vertex Packing Ici</b>
     * @param array
     * @param w
     * @param h
     */
    /*
    [ 0000 0000 0 | 0000 0000 0 | 0000 0000 0 | 0 | 0 | 000 ]
           x             y             z        u   v   dir
    [ 0000 0 | 0000 0 | 0000 0 | 00 | 0000 | 0000 | 000 0000 ]
        w        h        d      ao    sl     bl        ? 

    texture max = taille int
    */
    @ChunkThread
    public void addObjectFaceVertexToArray(int[] array, int w, int h, int d, int textureId){
        if (array.length < ITEM_TO_PACK){
            throw new RuntimeException("Array Empty");
        }
        // on garde la face
        for(int i = 0; i < array.length; i += ITEM_TO_PACK){
            int packed = 0;
            // |= c'est le OU, permet de combiner sans supprimer les valeurs
            // le "x" max est 255 donc pourquoi pas avoir << 24, car 255 = 1111 1111 donc 8 bits ?
            // le centre du cube n'est pas centré donc pour avoir le sommet le plus loin on fait x + 16 donc 240 + 16 = 256 ce qui dépasse 8bits
            packed |= (array[i])    << 23; // 9 bits, décale de 23 bits (donc prend les 9 premiers bits, 32 - 23 = 9)
            packed |= (array[i+1])  << 14; // 9 bits
            packed |= (array[i+2])  << 5;  // 9 bits
            packed |= (array[i+3])  << 4;  // 1 bit
            packed |= (array[i+4])  << 3;  // 1 bit
            packed |=  array[i+5];         // 3 bits
            
            int packed2 = 0;
            packed2 |= (w)                   << 27; // 5  bits
            packed2 |= (h)                   << 22; // 5  bits
            packed2 |= (d)                   << 17; // 5  bits
            packed2 |= (array[i+6]  & 0x3)   << 15; // 2  bits valeur 0-3
            packed2 |= (array[i+7]  & 0xF)   << 11; // 4  bits valeur 0-15
            packed2 |= (array[i+8]  & 0xF)   << 7;  // 4  bits valeur 0-15        
            // 7 bits libres restants
 
            this.vertexArrayPack[this.vertexCount * 3]     = packed;
            this.vertexArrayPack[this.vertexCount * 3 + 1] = packed2;
            this.vertexArrayPack[this.vertexCount * 3 + 2] = textureId;

            this.vertexCount++;
        }    
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
        // blocCount * 36 = taille exacte au pire cas
        if (this.vertexArrayPack == null || this.vertexArrayPack.length < needed){
            this.vertexArrayPack = new int[needed * 3];
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
        this.vertexBufferPack = MemoryUtil.memAllocInt(this.vertexCount * 3);

        for(int i = 0; i < this.vertexCount; i++){
            this.vertexBufferPack.put(this.vertexArrayPack[i * 3]); // packed data1
            this.vertexBufferPack.put(this.vertexArrayPack[i * 3 + 1]); // packed data2
            this.vertexBufferPack.put(this.vertexArrayPack[i * 3 + 2]); // textureId
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
        // copie locale pour éviter une race condition avec le thread qui remplit/vide vertexBufferPack
        IntBuffer localBuffer = this.vertexBufferPack;
        // ids est initialisé, on peut return si chunk vide
        if (localBuffer == null){
            return;
        }
        // Supprime les anciens buffers GPU avant d'en créer de nouveaux
        if (this.ids[0] != 0){ 
            BuffersManager.deleteVBOVAO(new int[]{this.ids[0], this.ids[1]});
            this.ids[0] = 0;
            this.ids[1] = 0;
        }
        int[] result = BuffersManager.createSubChunkVBOVAO(localBuffer);
        this.ids[0] = result[0];
        this.ids[1] = result[1];

        MemoryUtil.memFree(localBuffer);
        // ne remet à null que si personne n'a remplacé la référence entre-temps
        if (this.vertexBufferPack == localBuffer){
            this.vertexBufferPack = null;
        }
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
     * Dessine la maille avec le shader principal (opaque/glass/transparent)
     */
    @OpenGLThread
    public void drawSubChunkMesh(){   
        if (this.ids == null) return;
        if (this.ids[1] == 0) return;
        TextureManager.bind();

        if(KeyboardInput.isChunkBorderShow && this.ids[3] != 0){
            GL30.glBindVertexArray(ids[3]);
            GL11.glDrawArrays(GL11.GL_LINES, 0, 24);
            GL30.glBindVertexArray(0);  
        }
        GL30.glBindVertexArray(ids[1]);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, this.vertexCount);
        GL30.glBindVertexArray(0);        
        Render.monitor.incrementDrawCalls();
        Render.monitor.addTriangles(this.vertexCount / 3); 
    } 

    /**
     * Dessine la maille avec le shader liquide
     */
    @OpenGLThread
    public void drawSubChunkMeshLiquid(){   
        if (this.ids == null) return;
        if (this.ids[1] == 0) return;
        TextureManager.bindLiquid();

        GL30.glBindVertexArray(ids[1]);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, this.vertexCount);
        GL30.glBindVertexArray(0);        
        Render.monitor.incrementDrawCalls();
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
