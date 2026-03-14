package demo.Geometry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayDeque;

import org.joml.Matrix4f;

import demo.Annotations.ChunkThread;
import demo.Annotations.OpenGLThread;
import demo.Enum.BlocksType;
import demo.Enum.Directions;
import demo.Events.KeyboardInput;
import demo.Exceptions.IncorrectCoordinates;
import demo.Managers.ChunksManager;
import demo.Managers.ShadersManager;
import demo.Managers.TexturesManager;
import demo.Meshing.SubChunkMesh;
import demo.Meshing.Quad;
import demo.Rendering.Render;

public class SubChunk extends Node3D{

    public static final int CHUNKSIZEX = 16;
    public static final int CHUNKSIZEY = 16;
    public static final int CHUNKSIZEZ = 16;
    public static final int MAXBLOCK = 4096;    

    private final Matrix4f model = new Matrix4f();

    private SubChunk westNeighbor;
    private SubChunk eastNeighbor;
    private SubChunk northNeighbor;
    private SubChunk southNeighbor;
    private SubChunk upNeighbor;
    private SubChunk downNeighbor;

    // donne au mesh le subChunk au quel il appartient
    public SubChunkMesh mesh = new SubChunkMesh();
    public byte[] blocks;
    public byte[] lightMap;

    private List<Quad> quads;

    private boolean isGenerating = false;

    // contient des 1 et 0 (block plein, block vide(air))
    public BitSet bitSet = new BitSet(SubChunk.MAXBLOCK); 

    private static class FaceData {
        int[] northAxis = new int[SubChunk.CHUNKSIZEY * SubChunk.CHUNKSIZEZ];
        int[] southAxis = new int[SubChunk.CHUNKSIZEY * SubChunk.CHUNKSIZEZ];
        int[] westAxis  = new int[SubChunk.CHUNKSIZEY * SubChunk.CHUNKSIZEX];
        int[] eastAxis  = new int[SubChunk.CHUNKSIZEY * SubChunk.CHUNKSIZEX];
        int[] upAxis    = new int[SubChunk.CHUNKSIZEZ * SubChunk.CHUNKSIZEX];
        int[] downAxis  = new int[SubChunk.CHUNKSIZEZ * SubChunk.CHUNKSIZEX];

        int[] northMask = new int[SubChunk.CHUNKSIZEY * SubChunk.CHUNKSIZEZ];
        int[] southMask = new int[SubChunk.CHUNKSIZEY * SubChunk.CHUNKSIZEZ];
        int[] westMask  = new int[SubChunk.CHUNKSIZEY * SubChunk.CHUNKSIZEX];
        int[] eastMask  = new int[SubChunk.CHUNKSIZEY * SubChunk.CHUNKSIZEX];
        int[] upMask    = new int[SubChunk.CHUNKSIZEZ * SubChunk.CHUNKSIZEX];
        int[] downMask  = new int[SubChunk.CHUNKSIZEZ * SubChunk.CHUNKSIZEX];

        int[] northAoMask = new int[SubChunk.CHUNKSIZEY * SubChunk.CHUNKSIZEZ * SubChunk.CHUNKSIZEX];
        int[] southAoMask = new int[SubChunk.CHUNKSIZEY * SubChunk.CHUNKSIZEZ * SubChunk.CHUNKSIZEX];
        int[] westAoMask  = new int[SubChunk.CHUNKSIZEY * SubChunk.CHUNKSIZEX * SubChunk.CHUNKSIZEX];
        int[] eastAoMask  = new int[SubChunk.CHUNKSIZEY * SubChunk.CHUNKSIZEX * SubChunk.CHUNKSIZEX];
        int[] upAoMask    = new int[SubChunk.CHUNKSIZEZ * SubChunk.CHUNKSIZEX * SubChunk.CHUNKSIZEX];
        int[] downAoMask  = new int[SubChunk.CHUNKSIZEZ * SubChunk.CHUNKSIZEX * SubChunk.CHUNKSIZEX];

        int[] northLightMask = new int[SubChunk.CHUNKSIZEY * SubChunk.CHUNKSIZEZ * SubChunk.CHUNKSIZEX];
        int[] southLightMask = new int[SubChunk.CHUNKSIZEY * SubChunk.CHUNKSIZEZ * SubChunk.CHUNKSIZEX];
        int[] westLightMask  = new int[SubChunk.CHUNKSIZEY * SubChunk.CHUNKSIZEX * SubChunk.CHUNKSIZEX];
        int[] eastLightMask  = new int[SubChunk.CHUNKSIZEY * SubChunk.CHUNKSIZEX * SubChunk.CHUNKSIZEX];
        int[] upLightMask    = new int[SubChunk.CHUNKSIZEZ * SubChunk.CHUNKSIZEX * SubChunk.CHUNKSIZEX];
        int[] downLightMask  = new int[SubChunk.CHUNKSIZEZ * SubChunk.CHUNKSIZEX * SubChunk.CHUNKSIZEX];
    }

    // obligé sinon les threads qui construisent les chunks pour modifié les infos
    // Thread A écrit dans northAxis[0], Thread B écrit dans northAxis[0] au même moment -> les données se corrompent mutuellement
    // c'est pour cela qu'un threadlocal, va créer une instance de FACE_DATA_LOCAL sur chaque thread
    private static final ThreadLocal<FaceData> FACE_DATA_LOCAL = ThreadLocal.withInitial(() -> new FaceData());

    private static final int[][] LIGHT_NEIGHBORS = {
        {1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}
    };



    @ChunkThread
    public SubChunk(){
        this.quads = new ArrayList<Quad>();
        this.blocks = new byte[SubChunk.MAXBLOCK];    
        this.lightMap = new byte[SubChunk.MAXBLOCK]; 
        Arrays.fill(this.lightMap, (byte)0);
        // définit la taille du buffer et du tableau de vertex  
        this.mesh.setupSubChunkBuffers(this.bitSet.cardinality());   
    }

    // ------------------------------ Lumière ------------------------------
    // PRIVATE

    /**
     * Encode (x,y,z,light) en un Long pour les files BFS. <br>
     * Offset +32 sur chaque coord pour absorber les valeurs négatives de bordure.
     * @param x Coordonnées en x.
     * @param y Coordonnées en y.
     * @param z Coordonnées en z.
     * @param light Intensité de lumière.
     * @return Retourne les 4 paramètres encodés dans un long.
     */
    private static long encodeLightEntry(int x, int y, int z, int light) {
        return ((long)(x+32) << 40) | ((long)(y+32) << 24) | ((long)(z+32) << 8) | (light & 0xFF);
    }

    /**
     * Retourne le subChunk qui contient (x,y,z) exprimé en coords locales à this.
     * @param x Coordonnées en x.
     * @param y Coordonnées en y.
     * @param z Coordonnées en z.
     * @return Retourne le subChunk qui contient (x,y,z)
     */
    private SubChunk getSubChunkAt(int x, int y, int z) {
        if (x >= 0 && x < CHUNKSIZEX && y >= 0 && y < CHUNKSIZEY && z >= 0 && z < CHUNKSIZEZ)
            return this;
        if (y >= CHUNKSIZEY && upNeighbor    != null) return upNeighbor   .getSubChunkAt(x, y - CHUNKSIZEY, z);
        if (y < 0           && downNeighbor  != null) return downNeighbor .getSubChunkAt(x, y + CHUNKSIZEY, z);
        if (x >= CHUNKSIZEX && westNeighbor  != null) return westNeighbor .getSubChunkAt(x - CHUNKSIZEX, y, z);
        if (x < 0           && eastNeighbor  != null) return eastNeighbor .getSubChunkAt(x + CHUNKSIZEX, y, z);
        if (z >= CHUNKSIZEZ && northNeighbor != null) return northNeighbor.getSubChunkAt(x, y, z - CHUNKSIZEZ);
        if (z < 0           && southNeighbor != null) return southNeighbor.getSubChunkAt(x, y, z + CHUNKSIZEZ);
        return this;
    }

    /**
     * Ajoute dans dirty les 6 voisins de chaque chunk déjà présent. <br>
     * Nécessaire : leurs faces lisent les lightMaps des chunks modifiés.
     * @param dirty Set des subChunks à recalculés.
     */
    private static void addBorderNeighbors(Set<SubChunk> dirty) {
        Set<SubChunk> border = new HashSet<>();
        for (SubChunk s : dirty) {
            if (s.northNeighbor != null) border.add(s.northNeighbor);
            if (s.southNeighbor != null) border.add(s.southNeighbor);
            if (s.eastNeighbor  != null) border.add(s.eastNeighbor);
            if (s.westNeighbor  != null) border.add(s.westNeighbor);
            if (s.upNeighbor    != null) border.add(s.upNeighbor);
            if (s.downNeighbor  != null) border.add(s.downNeighbor);
        }
        dirty.addAll(border); // Tout les chunks à recalculés seront ajoutés dans le même set
    }

    /**
     * BFS de propagation : répand la lumière depuis les entrées de queue vers les voisins libres. <br>
     * Met à jour dirty avec les subChunks touchés.
     * @param queue Données (x,y,z,light) encodés.
     * @param dirty Set des subChunks à recalculés.
     */
    // ArrayDeque est utilisé en mode FIFO, ce qui garantit que la lumière se propage couche par couche
    private void bfsPropagate(ArrayDeque<Long> queue, Set<SubChunk> dirty) {
        while (!queue.isEmpty()) {
            long cur  = queue.poll();
            // décode les valeurs
            int cx    = ((int)(cur >> 40) & 0xFF) - 32;
            int cy    = ((int)(cur >> 24) & 0xFF) - 32;
            int cz    = ((int)(cur >>  8) & 0xFF) - 32;
            int cl    =  (int)(cur)       & 0xFF;

            // si une source de lumière = 0 elle ne doit rien propagé car elle est à 0, logique
            // si une source de lumière = 1 elle ne doit rien propagé car elle ne ferait que propager 0 sur les voisins
            if (cl <= 1) continue;

            // pour chaque block voisin
            for (int[] n : LIGHT_NEIGHBORS) {
                // on récupère ses informations
                int nx = cx+n[0], ny = cy+n[1], nz = cz+n[2];
                // si le block n'est pas de l'air on skip
                if (isBlockSolid(nx, ny, nz)) continue;
                // l'intensité de lumière baisse
                byte newLight = (byte)(cl - 1);
                // si l'intensité de lumière du block voisin et supérieur ou égal à l'intensité que l'on doit appliqué alors on skip
                if ((getLightAt(nx, ny, nz) & 0xFF) >= (newLight & 0xFF)) continue;
                // applique la nouvelle intensité de lumière
                setLightAt(nx, ny, nz, newLight);
                dirty.add(getSubChunkAt(nx, ny, nz));
                // ajoute le voisin dans la queue car lui aussi doit retraité ses voisins
                queue.add(encodeLightEntry(nx, ny, nz, newLight & 0xFF));
            }
        }
    }

    /**
     * BFS de suppression : éteint les blocs dont la lumière dépendait de la source retirée. <br>
     * Les blocs avec une lumière indépendante (>= lumière courante) sont empilés dans repropagate.
     * @param removeQueue File des blocks à éteindre.
     * @param repropagate File des blocks avec une lumière à recalculé.
     * @param dirty Set des subChunks à recalculés.
     */
    private void bfsRemove(ArrayDeque<long[]> removeQueue, ArrayDeque<Long> repropagate, Set<SubChunk> dirty) {
        // même fonctionnement que Propagate mais en sens inverse
        while (!removeQueue.isEmpty()) {
            long[] cur = removeQueue.poll();
            int cx = (int)cur[0], cy = (int)cur[1], cz = (int)cur[2], cl = (int)cur[3];

            for (int[] n : LIGHT_NEIGHBORS) {
                int nx = cx+n[0], ny = cy+n[1], nz = cz+n[2];

                if (isBlockSolid(nx, ny, nz)) continue;

                int nl = getLightAt(nx, ny, nz) & 0xFF;
                if (nl == 0) continue;
                if (nl < cl) {
                    setLightAt(nx, ny, nz, (byte) 0);
                    dirty.add(getSubChunkAt(nx, ny, nz));
                    removeQueue.add(new long[]{nx, ny, nz, nl});
                } else {
                    // source indépendante ou égale : sera re-propagée après
                    repropagate.add(encodeLightEntry(nx, ny, nz, nl));
                }
            }
        }
    }

    /**
     * Restaure le skylight sur les blocs encore ouverts au ciel dans les chunks dirty. <br>
     * La propagation est lancée directement depuis le subChunk concerné (coords locales à s).
     * @param dirty Set des subChunks à recalculés.
     */
    private static void restoreSkylight(Set<SubChunk> dirty) {
        // restoreSkylight lit et écrit dans dirty en même temps.
        // Java interdit de modifier une collection pendant qu'on l'itère avec un for-each
        // donc on fait une snapshot avec new 
        for (SubChunk s : new HashSet<>(dirty)) {
            for (int sx = 0; sx < CHUNKSIZEX; sx++) {
                for (int sz = 0; sz < CHUNKSIZEZ; sz++) {
                    for (int sy = 0; sy < CHUNKSIZEY; sy++) {
                        int idx = sx + sy * CHUNKSIZEX + sz * CHUNKSIZEX * CHUNKSIZEY;
                        if (s.lightMap[idx] < ChunksManager.SKY_LIGHT
                                && s.blocks[idx] == (byte) BlocksType.AIR.getId()
                                && s.isColumnOpenToSky(sx, sy, sz)) {
                            s.lightMap[idx] = ChunksManager.SKY_LIGHT;
                            dirty.addAll(s.propagateLightFrom(sx, sy, sz, ChunksManager.SKY_LIGHT));
                        }
                    }
                }
            }
        }
    }

    /**
     * Re-propage toutes les sources indépendantes accumulées dans repropagate. <br>
     * Les coords dans la file sont relatives à this et peuvent déborder sur les voisins <br>
     * via la chaîne de délégation de setLightAt.
     * @param repropagate File des blocks à recalculés.
     * @param dirty Set des subChunks à recalculés.
     */
    private void repropagateAll(ArrayDeque<Long> repropagate, Set<SubChunk> dirty) {
        while (!repropagate.isEmpty()) {
            long cur = repropagate.poll();
            // décode
            int rx = ((int)(cur >> 40) & 0xFF) - 32;
            int ry = ((int)(cur >> 24) & 0xFF) - 32;
            int rz = ((int)(cur >>  8) & 0xFF) - 32;
            int rl =  (int)(cur)       & 0xFF;

            // si une source de lumière = 0 elle ne doit rien propagé car elle est à 0, logique
            // si une source de lumière = 1 elle ne doit rien propagé car elle ne ferait que propager 0 sur les voisins
            if (rl <= 1) continue; // <- rl=0 remettrait des blocs à 0, rl=1 ne propage que du 0

            // rl = lumière qu'il avait quand on l'a enfilé dans repropagate
            // getLightAt(...) = lumière qu'il a maintenant dans la lightmap
            // getLightAt(...) < rl, le BFS l'a éteint entre-temps, pas de source indépendante, on skip
            // getLightAt(...) >= rl,  sa lumière n'a pas bougé, vraie source indépendante, repropage
            if ((getLightAt(rx, ry, rz) & 0xFF) < rl) continue;

            dirty.addAll(propagateLightFrom(rx, ry, rz, (byte) rl));
        }
    }

    /**
     * Détermine si aucun bloc solide ne bloque la colonne jusqu'au ciel.
     * @param x Coordonnées en x.
     * @param y Coordonnées en y, en dessous de cette valeur rien n'est fait.
     * @param z Coordonnées en z.
     * @return Retourne true si aucun bloc solide ne bloque la colonne jusqu'au ciel
     */
    private boolean isColumnOpenToSky(int x, int y, int z) {
        // regarde les blocks au dessus dans le même subChunk
        for (int cy = y + 1; cy < CHUNKSIZEY; cy++) {
            if (blocks[x + cy * CHUNKSIZEX + z * CHUNKSIZEX * CHUNKSIZEY] != (byte) BlocksType.AIR.getId())
                return false;
        }
        // regarde les blocks au dessus dans le subChunk Up
        SubChunk above = upNeighbor;
        while (above != null) {
            for (int cy = 0; cy < CHUNKSIZEY; cy++) {
                if (above.blocks[x + cy * CHUNKSIZEX + z * CHUNKSIZEX * CHUNKSIZEY] != (byte) BlocksType.AIR.getId())
                    // si il y a un block alors false
                    return false;
            }
            above = above.upNeighbor;
        }
        return true;
    }

    /**
     * Remplit la colonne vers le bas avec SKY_LIGHT sans atténuation, <br>
     * puis propage latéralement avec atténuation depuis chaque bloc de la colonne.
     * @param x Coordonnées en x.
     * @param y Coordonnées en y, position de départ.
     * @param z Coordonnées en z.
     * @return Set des subChunks à recalculés.
     */
    private Set<SubChunk> propagateSkyLightColumn(int x, int y, int z) {
        Set<SubChunk> dirty = new HashSet<>();
        ArrayDeque<Long> lateralQueue = new ArrayDeque<>();

        // descend la colonne à pleine intensité
        for (int d = 0; d < CHUNKSIZEY * 32; d++) {
            int ry = y - d;
            if (isBlockSolid(x, ry, z)) break;
            setLightAt(x, ry, z, ChunksManager.SKY_LIGHT);
            dirty.add(getSubChunkAt(x, ry, z));
            // enfile chaque bloc de la colonne pour propagation latérale
            lateralQueue.add(encodeLightEntry(x, ry, z, ChunksManager.SKY_LIGHT & 0xFF));
        }

        // propage latéralement depuis toute la colonne
        bfsPropagate(lateralQueue, dirty);
        addBorderNeighbors(dirty);
        return dirty;
    }

    // PUBLIC

    /**
     * Récupère la lumière d'un bloc, délègue aux voisins si hors-limites.
     * @param x Coordonnées en x.
     * @param y Coordonnées en y.
     * @param z Coordonnées en z.
     * @return Retourne l'intensité de lumière d'un block.
     */
    public byte getLightAt(int x, int y, int z) {
        if (x >= 0 && x < CHUNKSIZEX && y >= 0 && y < CHUNKSIZEY && z >= 0 && z < CHUNKSIZEZ)
            return lightMap[x + y * CHUNKSIZEX + z * CHUNKSIZEX * CHUNKSIZEY];
        if (y >= CHUNKSIZEY && upNeighbor    != null) return upNeighbor   .getLightAt(x, y - CHUNKSIZEY, z);
        if (y < 0           && downNeighbor  != null) return downNeighbor .getLightAt(x, y + CHUNKSIZEY, z);
        if (x >= CHUNKSIZEX && westNeighbor  != null) return westNeighbor .getLightAt(x - CHUNKSIZEX, y, z);
        if (x < 0           && eastNeighbor  != null) return eastNeighbor .getLightAt(x + CHUNKSIZEX, y, z);
        if (z >= CHUNKSIZEZ && northNeighbor != null) return northNeighbor.getLightAt(x, y, z - CHUNKSIZEZ);
        if (z < 0           && southNeighbor != null) return southNeighbor.getLightAt(x, y, z + CHUNKSIZEZ);
        return 0;
    }

    /**
     * Définit la lumière d'un bloc, délègue aux voisins si hors-limites.
     * @param x Coordonnées en x.
     * @param y Coordonnées en y.
     * @param z Coordonnées en z.
     * @param value Intensité de lumière à donné au block.
     */
    @ChunkThread
    public void setLightAt(int x, int y, int z, byte value) {
        if (x >= 0 && x < CHUNKSIZEX && y >= 0 && y < CHUNKSIZEY && z >= 0 && z < CHUNKSIZEZ) {
            lightMap[x + y * CHUNKSIZEX + z * CHUNKSIZEX * CHUNKSIZEY] = value;
            return;
        }
        if (y >= CHUNKSIZEY && upNeighbor    != null) { upNeighbor   .setLightAt(x, y - CHUNKSIZEY, z, value); return; }
        if (y < 0           && downNeighbor  != null) { downNeighbor .setLightAt(x, y + CHUNKSIZEY, z, value); return; }
        if (x >= CHUNKSIZEX && westNeighbor  != null) { westNeighbor .setLightAt(x - CHUNKSIZEX, y, z, value); return; }
        if (x < 0           && eastNeighbor  != null) { eastNeighbor .setLightAt(x + CHUNKSIZEX, y, z, value); return; }
        if (z >= CHUNKSIZEZ && northNeighbor != null) { northNeighbor.setLightAt(x, y, z - CHUNKSIZEZ, value); return; }
        if (z < 0           && southNeighbor != null) { southNeighbor.setLightAt(x, y, z + CHUNKSIZEZ, value); return; }
    }

    /**
     * Propage la lumière depuis (x,y,z) avec lightValue. <br>
     * @param x Coordonnées en x.
     * @param y Coordonnées en y.
     * @param z Coordonnées en z.
     * @param lightValue Intensité de lumière à donné au block.
     * @return Set des subChunks à recalculés.
     */
    @ChunkThread
    public Set<SubChunk> propagateLightFrom(int x, int y, int z, byte lightValue) {
        Set<SubChunk> dirty  = new HashSet<>();
        ArrayDeque<Long> queue = new ArrayDeque<>();

        // donne l'intensité au block
        setLightAt(x, y, z, lightValue);
        // ajoute le chunk au set des subChunks à recalculés
        dirty.add(this);
        // ajoute le block à la file des blocks qui doivent propagé leurs lumière
        queue.add(encodeLightEntry(x, y, z, lightValue & 0xFF));

        // propage la lumière
        bfsPropagate(queue, dirty);
        // ajoute les voisins du chunk car ils sont forcément touché par la propagation de lumière
        addBorderNeighbors(dirty);
        return dirty;
    }

    /**
     * Supprime la source de lumière en (x,y,z), éteint les blocs dépendants, <br>
     * restaure le skylight si nécessaire, puis re-propage les sources indépendantes. 
     * @param x Coordonnées en x.
     * @param y Coordonnées en y.
     * @param z Coordonnées en z.
     * @return Set des subChunks à recalculés.
     */
    @ChunkThread
    public Set<SubChunk> removeLightAt(int x, int y, int z) {
        return removeLightAt(x, y, z, (byte) 1);
    }

    /**
     * Supprime la source de lumière en (x,y,z), éteint les blocs dépendants, <br>
     * restaure le skylight si nécessaire, puis re-propage les sources indépendantes.
     * @param x Coordonnées en x.
     * @param y Coordonnées en y.
     * @param z Coordonnées en z.
     * @param minLight Intensité de lumière minimal à appliquer.
     * @return Set des subChunks à recalculés.
     */
    @ChunkThread
    public Set<SubChunk> removeLightAt(int x, int y, int z, byte minLight) {
        Set<SubChunk>    dirty       = new HashSet<>();
        ArrayDeque<long[]> removeQueue = new ArrayDeque<>();
        ArrayDeque<Long>   repropagate = new ArrayDeque<>();

        // récupère l'ancienne lumière avant la modification
        int oldLight = getLightAt(x, y, z) & 0xFF;
        // donne l'intensité au block
        setLightAt(x, y, z, minLight);
        // ajoute le chunk au set des subChunks à recalculés
        dirty.add(this);
        // ajoute le block à la file des blocks qui doivent supprimer leurs intensité de lumière
        removeQueue.add(new long[]{x, y, z, oldLight});

        // supprime toutes les intensités de lumière
        bfsRemove(removeQueue, repropagate, dirty);
        // applique la lumière du ciel si possible
        restoreSkylight(dirty);
        // repropage cette nouvelle intensité appliqué par le ciel
        repropagateAll(repropagate, dirty);
        // ajoute les voisins du chunk car ils sont forcément touché par la propagation de lumière 
        addBorderNeighbors(dirty);
        return dirty;
    }

    /**
     * Coupe le puit de skylight quand un bloc est posé en (x,y,z). <br>
     * Zéro toute la colonne sous le bloc en une passe atomique avant tout BFS, <br>
     * ce qui empêche le BFS de re-propager vers le bas avant la fin du zeroing.
     * @param x Coordonnées en x.
     * @param y Coordonnées en y.
     * @param z Coordonnées en z.
     * @return Set des subChunks à recalculés.
     */
    // zeroing = éteindre toute la colonne en une seule passe avant de laisser le BFS regarder les voisins.
    public Set<SubChunk> cutSkyLightColumn(int x, int y, int z, byte oldLight) {
        Set<SubChunk>    dirty       = new HashSet<>();
        ArrayDeque<long[]> removeQueue = new ArrayDeque<>();
        ArrayDeque<Long>   repropagate = new ArrayDeque<>();

        dirty.add(this);

        // traite la position du bloc posé lui-même avec son ANCIENNE lumière
        // addBlockToSubChunk a déjà mis lightMap[P]=0, mais les latéraux
        // autour de P croient encore que cette case était éclairée
        if ((oldLight & 0xFF) > 0) {
            removeQueue.add(new long[]{x, y, z, oldLight & 0xFF});
        }

        // Passe 1 — zéro atomique de toute la colonne vers le bas
        // tout se passe avant que bfsRemove démarre
        // d = 1, block posé, augmente plus on descend sous le block
        for (int d = 1; d < CHUNKSIZEY * 32; d++) {
            int ry = y - d; // coordonnées du block en y
            // si on rencontre un block solide, on ne s'occupe pas de la lumière en dessous
            if (isBlockSolid(x, ry, z)) break;
            // récupère la lumière actuel du block
            int light = getLightAt(x, ry, z) & 0xFF;
            if (light == 0) break;
            setLightAt(x, ry, z, (byte) 0);
            dirty.add(getSubChunkAt(x, ry, z));
            removeQueue.add(new long[]{x, ry, z, light});
        }

        // Passe 2 — suppression latérale, restauration skylight, re-propagation
        // ne verra que des 0 dans la colonne
        bfsRemove(removeQueue, repropagate, dirty);
        // applique la lumière du ciel si possible
        restoreSkylight(dirty);
        // repropage cette nouvelle intensité appliqué par le ciel
        repropagateAll(repropagate, dirty);
        // ajoute les voisins du chunk car ils sont forcément touché par la propagation de lumière 
        addBorderNeighbors(dirty);
        return dirty;
    }

    // ---------------------------- FIN Lumière ----------------------------

    /**
     * Place le subChunk avec des coordonnées par rapport au autres subChunk
     * @param x int
     * @param y int
     * @param z int
     */
    @ChunkThread
    public void setSubChunkPosition(int x, int y, int z){
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /**
     * Génère la maille d'un subChunk
     */
    @ChunkThread
    public void generateSubChunkMesh(){
        // Lignes de blocs pleins/vides sur l'axe (ex: ligne X = 16 bits, 1 = bloc plein, 0 = bloc vide)
        // Utilisé pour calculer quelles faces sont visibles (face culling)
        // les tableaux sont créer seulement pour le face culling puis après détruit par le GC 
        // car il ne servent plus à rien tant que le chunk n'est pas rebuils

        FaceData faceData = FACE_DATA_LOCAL.get();
        if (this.quads == null) this.quads = new ArrayList<>(); // réinitialise si null après un précédent mesh

        // calcule le face culling
        this.calculateSubChunkFaceCulling(faceData);

        // collecte les types uniques
        // le Set n'accepte pas les doublons donc si j'ai 500 GRASS dans le set il n'y aura que 1 GRASS
        Set<Byte> types = new HashSet<>();
        for(byte b : this.blocks){
            if(b != (byte)BlocksType.AIR.getId()) types.add(b);
        }     

        // calcule le greedy meshing pour chaque type de block
        
        for(byte blockType : types){
            this.calculateSubChunkBinaryGreedyMeshing(blockType, faceData);            
            this.uploadSubChunkMesh(blockType);
        }
        // rempli le buffer de la maille avec les données pack (vertex packing)
        this.mesh.fillSubChunkVertexBufferWithArray();


        this.quads = null;
    }

    /**
     * Retourne le block à la position
     * @param x int
     * @param y int
     * @param z int
     * @return BlockType
     */
    public BlocksType getBlock(int x, int y, int z){
        if (x < 0 || x >= SubChunk.CHUNKSIZEX ||
            y < 0 || y >= SubChunk.CHUNKSIZEY ||
            z < 0 || z >= SubChunk.CHUNKSIZEZ
        ){throw new IncorrectCoordinates("Coordonées dans le subChunk invalide");}
        int index = x + y * SubChunk.CHUNKSIZEX + z * SubChunk.CHUNKSIZEX * SubChunk.CHUNKSIZEY;
        return BlocksType.fromId(this.blocks[index]);
    }

    /**
     * Retourne tout les blocks du sous chunk
     * @return byte[]
     */
    public byte[] getAllBlocks(){
        return this.blocks;
    }
   

    /**
     * Remplis le vbo avec les données des objets avant le rendu
     */
    @OpenGLThread
    public void setupSubChunkVBOVAO(){
        if (this.mesh.getVertexCount() == 0) return;  // subchunk vide, rien à uploader
        // créer les vbo vao avec le buffer rempli
        this.mesh.createSubChunkVBOVAO();
        if (KeyboardInput.isChunkBorderShow && this.mesh.getBorderVAO() == 0 && this.mesh.getVertexCount() > 0){
            this.mesh.createSubChunkBorderVBOVAO();
        }
    }

    /**
     * Supprime le subChunk
     */
    @OpenGLThread
    public void removeSubChunk(){
        // Libère le buffer CPU off-heap s'il n'a pas encore été uploadé
        this.mesh.cleanup();
        // Libère les buffers GPU
        this.mesh.deleteSubChunkVBOVAO();
        this.cleanup();
    }

    /**
     * Ajoute un block dans le subChunk <br> 
     * (coordonnées locales du subChunk)
     * @param type BlockType
     * @param x int
     * @param y int
     * @param z int
     * @param rebuild
     * @throws IncorrectCoordinates
     */
    @ChunkThread
    public void addBlockToSubChunk(BlocksType type, int x, int y, int z, boolean rebuild) throws IncorrectCoordinates{
        if (x < 0 || x >= SubChunk.CHUNKSIZEX ||
            y < 0 || y >= SubChunk.CHUNKSIZEY ||
            z < 0 || z >= SubChunk.CHUNKSIZEZ
        ){throw new IncorrectCoordinates("Coordonées dans le subChunk invalide");}
        int index = x + y * SubChunk.CHUNKSIZEX + z * SubChunk.CHUNKSIZEX * SubChunk.CHUNKSIZEY;
        this.blocks[index] = (byte) type.getId();
        this.lightMap[index] = 0;
        // Ne rebuild pas pendant la génération initiale
        if (!this.isGenerating && rebuild){
            this.rebuildSubChunk();
        }
    }

    /**
     * Retire un block dans le subChunk <br>
     * (coordonnées locales du subChunk)
     * @param x int
     * @param y int
     * @param z int
     * @throws IncorrectCoordinates
     */
    @ChunkThread
    public void removeBlockFromSubChunk(int x, int y, int z) throws IncorrectCoordinates{
        if (x < 0 || x >= SubChunk.CHUNKSIZEX ||
            y < 0 || y >= SubChunk.CHUNKSIZEY ||
            z < 0 || z >= SubChunk.CHUNKSIZEZ
        ){throw new IncorrectCoordinates("Coordonées dans le subChunk invalide");}
        int index = x + y * SubChunk.CHUNKSIZEX + z * SubChunk.CHUNKSIZEX * SubChunk.CHUNKSIZEY;
        this.blocks[index] = (byte)BlocksType.AIR.getId();

        this.fillSubChunkBitSet();

        Set<SubChunk> toRebuild;

        if (isColumnOpenToSky(x, y, z)) {
            // cas 1 : ciel visible, rien à supprimer, on propage directement SKY_LIGHT
            // removeLightAt serait inutile ici car oldLight=0 donc BFS ne fait rien
            toRebuild = this.propagateSkyLightColumn(x, y, z);
        } else {
            // cas 2 : pas de ciel, le bloc avait peut-être de la lumière (torche voisine...)
            // removeLightAt éteint ce qui dépendait de lui et re-propage les sources voisines
            toRebuild = this.removeLightAt(x, y, z, (byte) 0);
        }

        // ajoute au set toRebuild au lieu de rebuild puis re rebuild
        if (x == SubChunk.CHUNKSIZEX - 1 && this.getNeighborWest()  != null) toRebuild.add(this.getNeighborWest());
        if (x == 0                        && this.getNeighborEast()  != null) toRebuild.add(this.getNeighborEast());
        if (z == SubChunk.CHUNKSIZEZ - 1  && this.getNeighborNorth() != null) toRebuild.add(this.getNeighborNorth());
        if (z == 0                        && this.getNeighborSouth() != null) toRebuild.add(this.getNeighborSouth());        
        if (y == SubChunk.CHUNKSIZEY - 1  && this.getNeighborUp()   != null) toRebuild.add(this.getNeighborUp());
        if (y == 0                        && this.getNeighborDown()  != null) toRebuild.add(this.getNeighborDown());

        for (SubChunk s : toRebuild) {
            if (s != this) s.rebuildSubChunk();
        }

        this.rebuildSubChunk();
    }

    @ChunkThread
    public void rebuildSubChunk(){
        this.fillSubChunkBitSet();
        synchronized(this.mesh){            
            this.linkNeighbors();
            this.mesh.setupSubChunkBuffers(this.bitSet.cardinality());
            this.generateSubChunkMesh();
        }
        // met le thread en pause si l'array est plein, le thread se relance automatiquement au moment ou OpenGLThread poll un chunk
        try { Render.dirtySubChunks.put(this); } catch(InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    /**
     * Envoie les quads à dessiné avec le face cullings
     */
    @ChunkThread
    private void uploadSubChunkMesh(byte blockType){

        for(Quad quad : this.quads){
            BlocksType type = BlocksType.fromId(blockType);
            if(type == BlocksType.NULL) continue;
            int textureId = TexturesManager.getIndex(type.getTexture(quad.direction));
            this.mesh.addQuadVertexToArray(quad, textureId);
        }        
    }

    /**
     * Comme son nom l'indique
     * Calcule aussi avec les faces des chunks voisins
     */
    @ChunkThread
    private void calculateSubChunkFaceCulling(FaceData faceData){
        Arrays.fill(faceData.eastAxis,  0); Arrays.fill(faceData.westAxis,  0);
        Arrays.fill(faceData.northAxis, 0); Arrays.fill(faceData.southAxis, 0);
        Arrays.fill(faceData.upAxis,    0); Arrays.fill(faceData.downAxis,  0);

        // Récup ouest est, on remplit l'int qui correspond a la ligne avec le BitSet du subChunk
        for (int y = 0; y < SubChunk.CHUNKSIZEY; y++){
            for (int z = 0; z < SubChunk.CHUNKSIZEZ; z++){
                int index = y + z * SubChunk.CHUNKSIZEY;      
                for (int x = 0; x < SubChunk.CHUNKSIZEX; x++){
                    int bitSetIndex = x + y * SubChunk.CHUNKSIZEX + z * SubChunk.CHUNKSIZEX * SubChunk.CHUNKSIZEY;                    
                    faceData.eastAxis[index] |= (this.bitSet.get(bitSetIndex) ? 1 : 0) << (31 - x);                    
                }
                int ligne = faceData.eastAxis[index];
                
                // Est : récupère les bits les plus à droite du voisin est (x = 15)
                int ligneNeighborEast = 0;
                // si il y a un voisin
                if (this.getNeighborEast() != null){
                    int bitSetIndex = 15 + y * SubChunk.CHUNKSIZEX + z * SubChunk.CHUNKSIZEX * SubChunk.CHUNKSIZEY;
                    // on récupère la valeur du bitSet à l'index
                    // met le dernier bit a 1
                    ligneNeighborEast = (this.getNeighborEast().bitSet.get(bitSetIndex) ? 1 : 0);
                }                                 
                // si on récup le bit le plus a droite du voisin il faut le mettre le plus a gauche pour le subChunk actuel
                ligneNeighborEast = ligneNeighborEast << 31;

                int ligneNeighborWest = 0;
                if (this.getNeighborWest() != null){
                    int bitSetIndex = 0 + y * SubChunk.CHUNKSIZEX + z * SubChunk.CHUNKSIZEX * SubChunk.CHUNKSIZEY;
                    ligneNeighborWest = (this.getNeighborWest().bitSet.get(bitSetIndex) ? 1 : 0);
                }   
                // si on récup le bit le plus a droite du voisin il faut le mettre le plus a droite par rapport a la ligne donc au milieu de l'int pour le subChunk actuel
                ligneNeighborWest = ligneNeighborWest << 16;

                // le >> ajoute un 1 au début le >>> ajoute un 0
                faceData.eastAxis[index] = ligne & ~((ligne >>> 1) | ligneNeighborEast); // faces vers l'est + le bloc voisin
                faceData.westAxis[index] = ligne & ~((ligne << 1) | ligneNeighborWest); // faces vers l'ouest + le bloc voisin     
            }
        }
        // Récup nord sud
        for (int y = 0; y < SubChunk.CHUNKSIZEY; y++){
            for (int x = 0; x < SubChunk.CHUNKSIZEX; x++){
                int index = y + x * SubChunk.CHUNKSIZEY;
                for (int z = 0; z < SubChunk.CHUNKSIZEZ; z++){
                    int bitSetIndex = x + y * SubChunk.CHUNKSIZEX + z * SubChunk.CHUNKSIZEX * SubChunk.CHUNKSIZEY;
                    faceData.northAxis[index] |= (this.bitSet.get(bitSetIndex) ? 1 : 0) << (31 - z);
                }
                int ligne = faceData.northAxis[index];

                int ligneNeighborSouth = 0;
                if (this.getNeighborSouth() != null){
                    int bitSetIndex = x + y * SubChunk.CHUNKSIZEX + 15 * SubChunk.CHUNKSIZEX * SubChunk.CHUNKSIZEY;
                    ligneNeighborSouth = (this.getNeighborSouth().bitSet.get(bitSetIndex) ? 1 : 0);
                }
                ligneNeighborSouth = ligneNeighborSouth << 31;     

                int ligneNeighborNorth = 0;
                if (this.getNeighborNorth() != null){
                    int bitSetIndex = x + y * SubChunk.CHUNKSIZEX + 0 * SubChunk.CHUNKSIZEX * SubChunk.CHUNKSIZEY;
                    ligneNeighborNorth = (this.getNeighborNorth().bitSet.get(bitSetIndex) ? 1 : 0);
                }
                ligneNeighborNorth = ligneNeighborNorth << 16;  

                faceData.southAxis[index] = ligne & ~((ligne >>> 1) | ligneNeighborSouth); // faces vers le sud  
                faceData.northAxis[index] = ligne & ~((ligne << 1) | ligneNeighborNorth); // faces vers le nord
            }
        }
        // Récup haut bas
        for (int z = 0; z < SubChunk.CHUNKSIZEZ; z++){
            for (int x = 0; x < SubChunk.CHUNKSIZEX; x++){
                int index = z + x * SubChunk.CHUNKSIZEZ;
                for (int y = 0; y < SubChunk.CHUNKSIZEY; y++){
                    int bitSetIndex = x + y * SubChunk.CHUNKSIZEX + z * SubChunk.CHUNKSIZEX * SubChunk.CHUNKSIZEY;
                    faceData.upAxis[index] |= (this.bitSet.get(bitSetIndex) ? 1 : 0) << (31 - y);
                }
                int ligne = faceData.upAxis[index];
                int ligneNeighborUp = 0;
                if (this.getNeighborUp() != null){
                    int bitSetIndex = x + 0 * SubChunk.CHUNKSIZEX + z * SubChunk.CHUNKSIZEX * SubChunk.CHUNKSIZEY;
                    ligneNeighborUp = (this.getNeighborUp().bitSet.get(bitSetIndex) ? 1 : 0);
                }
                ligneNeighborUp = ligneNeighborUp << 16;
                int ligneNeighborDown = 0;
                if (this.getNeighborDown() != null){
                    int bitSetIndex = x + 15 * SubChunk.CHUNKSIZEX + z * SubChunk.CHUNKSIZEX * SubChunk.CHUNKSIZEY;
                    ligneNeighborDown = (this.getNeighborDown().bitSet.get(bitSetIndex) ? 1 : 0);
                }
                ligneNeighborDown = ligneNeighborDown << 31;
                faceData.upAxis[index] = ligne & ~((ligne << 1) | ligneNeighborUp); // faces vers le haut
                faceData.downAxis[index] = ligne & ~((ligne >>> 1) | ligneNeighborDown); // faces vers le bas  
            }
        }
    }

    /**
     * Peut pas faire plus simple comme nom
     */
    private void calculateSubChunkBinaryGreedyMeshing(byte blockType, FaceData faceData){
        this.quads.clear();
        Arrays.fill(faceData.westMask,  0); Arrays.fill(faceData.eastMask,  0);
        Arrays.fill(faceData.northMask, 0); Arrays.fill(faceData.southMask, 0);
        Arrays.fill(faceData.upMask,    0); Arrays.fill(faceData.downMask,  0);
        Arrays.fill(faceData.westAoMask,  0); Arrays.fill(faceData.eastAoMask,  0);
        Arrays.fill(faceData.northAoMask, 0); Arrays.fill(faceData.southAoMask, 0);
        Arrays.fill(faceData.upAoMask,    0); Arrays.fill(faceData.downAoMask,  0);
        Arrays.fill(faceData.westLightMask,  0); Arrays.fill(faceData.eastLightMask,  0);
        Arrays.fill(faceData.northLightMask, 0); Arrays.fill(faceData.southLightMask, 0);
        Arrays.fill(faceData.upLightMask,    0); Arrays.fill(faceData.downLightMask,  0);

        // rempli les masques avec les infos du face culling
        for (int y = 0; y < SubChunk.CHUNKSIZEY; y++){
            for (int z = 0; z < SubChunk.CHUNKSIZEZ; z++){                
                for (int x = 0; x < SubChunk.CHUNKSIZEX; x++){                    
                    int ligne = y + x * SubChunk.CHUNKSIZEX;

                    // filtre par type
                    int blockIndex = x + y * SubChunk.CHUNKSIZEX + z * SubChunk.CHUNKSIZEX * SubChunk.CHUNKSIZEY;
                    if(this.blocks[blockIndex] != blockType) continue;

                    int westBit = (faceData.westAxis[y + z * SubChunk.CHUNKSIZEY] >>> (31 - x)) & 1;
                    faceData.westMask[ligne] |= (westBit << (31 - z));  
                    if(westBit == 1){
                        int ao0 = computeAO(x+1, y, z-1,   x+1, y-1, z,   x+1, y-1, z-1);
                        int ao1 = computeAO(x+1, y, z+1,   x+1, y-1, z,   x+1, y-1, z+1);
                        int ao2 = computeAO(x+1, y, z-1,   x+1, y+1, z,   x+1, y+1, z-1);
                        int ao3 = computeAO(x+1, y, z+1,   x+1, y+1, z,   x+1, y+1, z+1);
                        faceData.westAoMask[ligne * SubChunk.CHUNKSIZEX + z] = ao0|(ao1<<2)|(ao2<<4)|(ao3<<6);
         
                        faceData.westLightMask[ligne * SubChunk.CHUNKSIZEX + z] = getLightAt(x+1, y, z) & 0xF;
                    }

                    int eastBit = (faceData.eastAxis[y + z * SubChunk.CHUNKSIZEY] >> (31 - x)) & 1;
                    faceData.eastMask[ligne] |= (eastBit << (31 - z));
                    if(eastBit == 1){
                        int ao0 = computeAO(x-1, y, z-1,   x-1, y-1, z,   x-1, y-1, z-1);
                        int ao1 = computeAO(x-1, y, z+1,   x-1, y-1, z,   x-1, y-1, z+1);
                        int ao2 = computeAO(x-1, y, z-1,   x-1, y+1, z,   x-1, y+1, z-1);
                        int ao3 = computeAO(x-1, y, z+1,   x-1, y+1, z,   x-1, y+1, z+1);                        
                        faceData.eastAoMask[ligne * SubChunk.CHUNKSIZEX + z] = ao0|(ao1<<2)|(ao2<<4)|(ao3<<6);

                        faceData.eastLightMask[ligne * SubChunk.CHUNKSIZEX + z] = getLightAt(x-1, y, z) & 0xF;
                    }
                }
            }
        }        
        for (int y = 0; y < SubChunk.CHUNKSIZEY; y++){
            for (int x = 0; x < SubChunk.CHUNKSIZEX; x++){
                for (int z = 0; z < SubChunk.CHUNKSIZEZ; z++){                    
                    int ligne = y + z * SubChunk.CHUNKSIZEZ;

                    int blockIndex = x + y * SubChunk.CHUNKSIZEX + z * SubChunk.CHUNKSIZEX * SubChunk.CHUNKSIZEY;
                    if(this.blocks[blockIndex] != blockType) continue;

                    // En gros je récup tous les bits d'un int d'Axis, que je met en 1er position des int de mask a chaque boucle
                    int northBit = (faceData.northAxis[y + x * SubChunk.CHUNKSIZEY] >>> (31 - z)) & 1; // décalle de 31 bits + z vers la droite puis récupère le 1er bit
                    faceData.northMask[ligne] |= (northBit << (31 - x));
                    if(northBit == 1){
                        int ao0 = computeAO(x-1, y, z+1,   x, y-1, z+1,   x-1, y-1, z+1);
                        int ao1 = computeAO(x+1, y, z+1,   x, y-1, z+1,   x+1, y-1, z+1);
                        int ao2 = computeAO(x-1, y, z+1,   x, y+1, z+1,   x-1, y+1, z+1);
                        int ao3 = computeAO(x+1, y, z+1,   x, y+1, z+1,   x+1, y+1, z+1);
                        faceData.northAoMask[ligne * SubChunk.CHUNKSIZEX + x] = ao0|(ao1<<2)|(ao2<<4)|(ao3<<6);

                        faceData.northLightMask[ligne * SubChunk.CHUNKSIZEX + x] = getLightAt(x, y, z+1) & 0xF;
                    }

                    int southBit = (faceData.southAxis[y + x * SubChunk.CHUNKSIZEY] >> (31 - z)) & 1; // décalle de 31 bits - z vers la droite puis récupère le 1er bit*
                    faceData.southMask[ligne] |= (southBit << (31 - x));
                    if(southBit == 1){
                        int ao0 = computeAO(x-1, y, z-1,   x, y-1, z-1,   x-1, y-1, z-1);
                        int ao1 = computeAO(x+1, y, z-1,   x, y-1, z-1,   x+1, y-1, z-1);
                        int ao2 = computeAO(x-1, y, z-1,   x, y+1, z-1,   x-1, y+1, z-1);
                        int ao3 = computeAO(x+1, y, z-1,   x, y+1, z-1,   x+1, y+1, z-1);
                        faceData.southAoMask[ligne * SubChunk.CHUNKSIZEX + x] = ao0|(ao1<<2)|(ao2<<4)|(ao3<<6);

                        faceData.southLightMask[ligne * SubChunk.CHUNKSIZEX + x] = getLightAt(x, y, z-1) & 0xF;
                    }

                }
            }
        }
        for (int y = 0; y < SubChunk.CHUNKSIZEY; y++){
            for (int z = 0; z < SubChunk.CHUNKSIZEZ; z++){
                for (int x = 0; x < SubChunk.CHUNKSIZEX; x++){
                    int ligne = z + y * SubChunk.CHUNKSIZEZ;

                    int blockIndex = x + y * SubChunk.CHUNKSIZEX + z * SubChunk.CHUNKSIZEX * SubChunk.CHUNKSIZEY;
                    if(this.blocks[blockIndex] != blockType) continue;

                    int upBit = (faceData.upAxis[z + x * SubChunk.CHUNKSIZEZ] >>> (31 - y)) & 1;
                    faceData.upMask[ligne] |= (upBit << (31 - x));
                    if(upBit == 1){
                        int ao0 = computeAO(x-1, y+1, z,   x, y+1, z-1,   x-1, y+1, z-1);
                        int ao1 = computeAO(x+1, y+1, z,   x, y+1, z-1,   x+1, y+1, z-1);
                        int ao2 = computeAO(x-1, y+1, z,   x, y+1, z+1,   x-1, y+1, z+1);
                        int ao3 = computeAO(x+1, y+1, z,   x, y+1, z+1,   x+1, y+1, z+1);
                        faceData.upAoMask[ligne * SubChunk.CHUNKSIZEX + x] = ao0|(ao1<<2)|(ao2<<4)|(ao3<<6);
                   
                        faceData.upLightMask[ligne * SubChunk.CHUNKSIZEX + x] = getLightAt(x, y+1, z) & 0xF;
                    }

                    int downBit = (faceData.downAxis[z + x * SubChunk.CHUNKSIZEZ] >>> (31 - y)) & 1;
                    faceData.downMask[ligne] |= (downBit << (31 - x));
                    if(downBit == 1){
                        int ao0 = computeAO(x-1, y-1, z,   x, y-1, z-1,   x-1, y-1, z-1);
                        int ao1 = computeAO(x+1, y-1, z,   x, y-1, z-1,   x+1, y-1, z-1);
                        int ao2 = computeAO(x-1, y-1, z,   x, y-1, z+1,   x-1, y-1, z+1);
                        int ao3 = computeAO(x+1, y-1, z,   x, y-1, z+1,   x+1, y-1, z+1);
                        faceData.downAoMask[ligne * SubChunk.CHUNKSIZEX + x] = ao0|(ao1<<2)|(ao2<<4)|(ao3<<6);

                        faceData.downLightMask[ligne * SubChunk.CHUNKSIZEX + x] = getLightAt(x, y-1, z) & 0xF;
                    }
                }
            }
        }

        this.greedyMeshingPerFace(faceData.westMask, faceData.westAoMask, faceData.westLightMask, Directions.WEST);
        this.greedyMeshingPerFace(faceData.eastMask, faceData.eastAoMask, faceData.eastLightMask, Directions.EAST);
        this.greedyMeshingPerFace(faceData.northMask, faceData.northAoMask, faceData.northLightMask, Directions.NORTH);
        this.greedyMeshingPerFace(faceData.southMask, faceData.southAoMask, faceData.southLightMask, Directions.SOUTH);
        this.greedyMeshingPerFace(faceData.upMask, faceData.upAoMask, faceData.upLightMask, Directions.UP);
        this.greedyMeshingPerFace(faceData.downMask, faceData.downAoMask, faceData.downLightMask, Directions.DOWN);
    }

    // fait le greedy pour chaque face
    private void greedyMeshingPerFace(int[] faceMask, int[] faceAoMask, int[] faceLightMask, Directions dir){
        for(int ligne = 0; ligne < faceMask.length; ligne++){
            int i = 0; // indice en largeur byte car dépasse pas 15
            while(i < SubChunk.CHUNKSIZEX){              
                if (faceMask[ligne] == 0) break; // si il n'y a pas de face           
                int j = 1; // indice en longueur/hauteur

                int[] coord = new int[3]; // point de départ, fin quad en x, fin quad en y
                while((i < SubChunk.CHUNKSIZEX) && ((faceMask[ligne] >>> (31 - i)) & 1) != 1){ // tant que l'on a pas trouvé la 1er face pleine
                    i++;
                }
                coord[0] = i;// x commence à i
                while((i < SubChunk.CHUNKSIZEX) && ((faceMask[ligne] >>> (31 - i)) & 1) != 0
                    && faceAoMask[ligne * SubChunk.CHUNKSIZEX + i] == faceAoMask[ligne * SubChunk.CHUNKSIZEX + coord[0]]
                    && faceLightMask[ligne * SubChunk.CHUNKSIZEX + i] == faceLightMask[ligne * SubChunk.CHUNKSIZEX + coord[0]]){
                    i++;
                }

                coord[1] = i;// x se termine à i
                 
                if (coord[0] >= coord[1]){
                    i++;
                    continue;
                }
                // exemple x=2 x=5 (5 - (2 - 1)) = 4 largeur
                //        1 << 4 = 00010000
                //  (1 << 4) - 1 = 00001111
                int width = coord[1] - coord[0];
                int mask = ((1 << width) - 1) << (32 - coord[1]);
                faceMask[ligne] = faceMask[ligne] & ~mask;
                
                // parcourt en hauteur 
                // si la ligne en dessous a les même bits entre les 2 x à 1 alors on la récup 
                int sliceStart = (ligne / SubChunk.CHUNKSIZEX) * SubChunk.CHUNKSIZEX;
                while((ligne + j < sliceStart + SubChunk.CHUNKSIZEX)
                    && (ligne + j < faceMask.length) 
                    && (faceMask[ligne + j] & mask) == mask){

                    boolean match = true;
                    for(int k = coord[0]; k < coord[1]; k++){
                        if(faceAoMask[(ligne+j) * SubChunk.CHUNKSIZEX + k] != faceAoMask[ligne * SubChunk.CHUNKSIZEX + k]
                        ||faceLightMask[(ligne+j) * SubChunk.CHUNKSIZEX + k] != faceLightMask[ligne * SubChunk.CHUNKSIZEX + k]){
                            match = false;
                            break;
                        }
                    }
                    if(!match) break;
                    faceMask[ligne + j] = faceMask[ligne + j] & ~mask;
                    j++;
                }
                // le quad descent de j
                coord[2] = j;       

                if (coord[0] < coord[1]){
                    int qx, qy, qz;
                    switch(dir){
                        case NORTH: case SOUTH:
                            // masque index = y + z * CHUNKSIZEZ, bit = X
                            qx = coord[0];
                            qy = ligne % SubChunk.CHUNKSIZEY;
                            qz = ligne / SubChunk.CHUNKSIZEZ;
                            break;
                        case WEST: case EAST:
                            // masque index = y + x * CHUNKSIZEX, bit = Z
                            qx = ligne / SubChunk.CHUNKSIZEY;
                            qy = ligne % SubChunk.CHUNKSIZEY;
                            qz = coord[0];
                            break;
                        case UP: case DOWN:
                            // masque index = y + z * CHUNKSIZEY, bit = X
                            qx = coord[0];
                            qy = ligne / SubChunk.CHUNKSIZEZ;  
                            qz = ligne % SubChunk.CHUNKSIZEZ; 
                            break;
                        default:
                            qx = qy = qz = 0;
                    }

                    int w = coord[1] - coord[0];
                    int h = coord[2];
                    
                    int packedAoBottomFirst = faceAoMask[ligne                   * SubChunk.CHUNKSIZEX + coord[0]    ];
                    int packedAoBottomLast  = faceAoMask[ligne                   * SubChunk.CHUNKSIZEX + (coord[1]-1)];
                    int packedAoTopFirst    = faceAoMask[(ligne + h - 1)         * SubChunk.CHUNKSIZEX + coord[0]    ];
                    int packedAoTopLast     = faceAoMask[(ligne + h - 1)         * SubChunk.CHUNKSIZEX + (coord[1]-1)];

                    int ao0 =  packedAoBottomFirst        & 0x3; // bas-gauche
                    int ao1 = (packedAoBottomLast  >> 2)  & 0x3; // bas-droite  <- dernier bloc
                    int ao2 = (packedAoTopFirst    >> 4)  & 0x3; // haut-gauche <- ligne du haut
                    int ao3 = (packedAoTopLast     >> 6)  & 0x3; // haut-droite <- dernier bloc, ligne du haut

                    int light = faceLightMask[ligne * SubChunk.CHUNKSIZEX + coord[0]] & 0xF;
                    

                    this.quads.add(new Quad(qx, qy, qz, w, h, dir, ao0, ao1, ao2, ao3, light));
                }   
            }           
        }
    }

    /**
     * Détermine si un block est solide <br>
     * Si le block n'est pas dans le chunk (voisin d'un chunk) <br>
     * On demande au voisin
     * @param x
     * @param y
     * @param z
     * @return
     */
    private boolean isBlockSolid(int x, int y, int z){
        // dans le subchunk courant
        if (x >= 0 && x < CHUNKSIZEX && y >= 0 && y < CHUNKSIZEY && z >= 0 && z < CHUNKSIZEZ){
            int index = x + y * CHUNKSIZEX + z * CHUNKSIZEX * CHUNKSIZEY;
            return this.blocks[index] != (byte)BlocksType.AIR.getId();
        }
        // en dehors délègue aux voisins, blocks en bordure
        if (y >= CHUNKSIZEY && this.upNeighbor != null){
            return this.upNeighbor.isBlockSolid(x, y - CHUNKSIZEY, z);
        }
        if (y < 0 && this.downNeighbor != null){
            return this.downNeighbor.isBlockSolid(x, y + CHUNKSIZEY, z);
        }
        if (x >= CHUNKSIZEX && this.westNeighbor != null){
            return this.westNeighbor.isBlockSolid(x - CHUNKSIZEX, y, z);
        }
        if (x < 0 && this.eastNeighbor != null){
            return this.eastNeighbor.isBlockSolid(x + CHUNKSIZEX, y, z);
        }
        if (z >= CHUNKSIZEZ && this.northNeighbor != null){
            return this.northNeighbor.isBlockSolid(x, y, z - CHUNKSIZEZ);
        }
        if (z < 0 && this.southNeighbor != null){
            return this.southNeighbor.isBlockSolid(x, y, z + CHUNKSIZEZ);
        }
        return false;
    }

    /**
     * Calcule l'ambient occlusion à appliquer sur le block
     * @param side1X
     * @param side1Y
     * @param side1Z
     * @param side2X
     * @param side2Y
     * @param side2Z
     * @param cornerX
     * @param cornerY
     * @param cornerZ
     * @return
     */
    private byte computeAO(int side1X, int side1Y, int side1Z,
                           int side2X, int side2Y, int side2Z,
                           int cornerX, int cornerY, int cornerZ){
        boolean s1 = isBlockSolid(side1X, side1Y, side1Z);
        boolean s2 = isBlockSolid(side2X, side2Y, side2Z);
        boolean c  = isBlockSolid(cornerX, cornerY, cornerZ);

        if (s1 && s2) {
            return 3;
        }
        return (byte)((s1 ? 1 : 0) + (s2 ? 1 : 0) + (c ? 1 : 0));
    }


    /**
     * Remplis le BitSet du subChunk (cube plein = 1, cube vide = 0)
     */
    @ChunkThread
    public void fillSubChunkBitSet(){
        this.bitSet.clear();
        for(int i = 0; i < SubChunk.MAXBLOCK; i++){
            if (this.blocks[i] != (byte)BlocksType.AIR.getId()){
                this.bitSet.set(i); // met a 1 l'indice i
            }
        }
    }
    
    /**
     * Récupère tout les voisins du subChunk
     */
    @ChunkThread
    public void linkNeighbors(){
        // ouest
        try{
            int chunkX = (int) Math.floor((float)this.x / SubChunk.CHUNKSIZEX);
            int chunkZ = (int) Math.floor((float)this.z / SubChunk.CHUNKSIZEZ);
            Chunk chunk = ChunksManager.getChunk(chunkX + 1, chunkZ);
            this.westNeighbor = chunk.getSubChunk(this.y / SubChunk.CHUNKSIZEY);
        }catch(IncorrectCoordinates e){
            this.westNeighbor = null;
        }  
        // est
        try{
            int chunkX = (int) Math.floor((float)this.x / SubChunk.CHUNKSIZEX);
            int chunkZ = (int) Math.floor((float)this.z / SubChunk.CHUNKSIZEZ);
            Chunk chunk = ChunksManager.getChunk(chunkX - 1, chunkZ);
            this.eastNeighbor = chunk.getSubChunk(this.y / SubChunk.CHUNKSIZEY);
        }catch(IncorrectCoordinates e){
            this.eastNeighbor = null;
        }   
        // nord
        try{
            int chunkX = (int) Math.floor((float)this.x / SubChunk.CHUNKSIZEX);
            int chunkZ = (int) Math.floor((float)this.z / SubChunk.CHUNKSIZEZ);
            Chunk chunk = ChunksManager.getChunk(chunkX, chunkZ + 1);
            this.northNeighbor = chunk.getSubChunk(this.y / SubChunk.CHUNKSIZEY);
        }catch(IncorrectCoordinates e){
            this.northNeighbor = null;
        }   
        // sud
        try{
            int chunkX = (int) Math.floor((float)this.x / SubChunk.CHUNKSIZEX);
            int chunkZ = (int) Math.floor((float)this.z / SubChunk.CHUNKSIZEZ);
            Chunk chunk = ChunksManager.getChunk(chunkX, chunkZ - 1);
            this.southNeighbor = chunk.getSubChunk(this.y / SubChunk.CHUNKSIZEY);
        }catch(IncorrectCoordinates e){
            this.southNeighbor = null;
        } 
        // haut
        try{
            int chunkX = (int) Math.floor((float)this.x / SubChunk.CHUNKSIZEX);
            int chunkZ = (int) Math.floor((float)this.z / SubChunk.CHUNKSIZEZ);
            Chunk chunk = ChunksManager.getChunk(chunkX, chunkZ);
            this.upNeighbor = chunk.getSubChunk((this.y / SubChunk.CHUNKSIZEY) + 1);
        }catch(IncorrectCoordinates e){
            this.upNeighbor = null;
        }  
        // bas
        try{
            int chunkX = (int) Math.floor((float)this.x / SubChunk.CHUNKSIZEX);
            int chunkZ = (int) Math.floor((float)this.z / SubChunk.CHUNKSIZEZ);
            Chunk chunk = ChunksManager.getChunk(chunkX, chunkZ);
            this.downNeighbor = chunk.getSubChunk((this.y / SubChunk.CHUNKSIZEY) - 1);
        }catch(IncorrectCoordinates e){
            this.downNeighbor = null;
        }  
    }

    /**
     * Retourne le subChunk voisin Ouest
     * @return SubChunk
     */
    public SubChunk getNeighborWest(){
        return this.westNeighbor;    
    }
    /**
     * Retourne le subChunk voisin Est
     * @return SubChunk
     */
    public SubChunk getNeighborEast(){
         return this.eastNeighbor;
    }
    /**
     * Retourne le subChunk voisin Nord
     * @return SubChunk
     */
    public SubChunk getNeighborNorth(){
        return this.northNeighbor;     
    }
    /**
     * Retourne le subChunk voisin Sud
     * @return SubChunk
     */
    public SubChunk getNeighborSouth(){
        return this.southNeighbor;   
    }
    /**
     * Retourne le subChunk voisin Haut
     * @return SubChunk
     */
    public SubChunk getNeighborUp(){
        return this.upNeighbor;  
    }
    /**
     * Retourne le subChunk voisin Bas
     * @return SubChunk
     */
    public SubChunk getNeighborDown(){
        return this.downNeighbor;  
    }

    public void setGenerating(boolean generating){
        this.isGenerating = generating;
    }

    /**
     * Dessine la maille du subChunk
     */
    @OpenGLThread
    @Override
    public void drawSpecific(){
        this.mesh.drawSubChunkMesh();
    }

    @ChunkThread
    @Override
    public void drawNode(Matrix4f parentModel){
        parentModel.translate(this.x, this.y, this.z, this.model);
        this.model.get(this.buffer);
        ShadersManager.runModel(this.buffer);
        this.drawSpecific();
        // pas d'itération sur les cubes enfants
    }
}
