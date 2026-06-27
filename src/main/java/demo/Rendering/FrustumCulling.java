package demo.Rendering;

import org.joml.FrustumIntersection;
import org.joml.Matrix4f;

import demo.Managers.PlayersManager;

/**
 * Frustum Culling des chunks<br>
 * Les chunks non visibles sont cachées
 */
public class FrustumCulling {
    
    private static final FrustumIntersection frustumIntersect = new FrustumIntersection();
    private static final Matrix4f matViewProj = new Matrix4f();

    /**
     * Multiplie la matrice de vue et de projection<br>
     */
    public static void recalculateMatrix(){
        Render.monitor.resetChunkVisibleCount();
        Render.getProjection().mul(PlayersManager.player.getCamera().getViewMatrix(), FrustumCulling.matViewProj);
        FrustumCulling.frustumIntersect.set(FrustumCulling.matViewProj);
    }

    /**
     * Teste si l'AABB du chunk intersecte ou est à l'intérieur du frustrum
     * @param minX minimum en X
     * @param minY minimum en Y
     * @param minZ minimum en Z
     * @param maxX maximum en X
     * @param maxY maximum en Y
     * @param maxZ maximum en Z
     * @return Retourne vrai si le chunk est visible
     */
    public static boolean testAABB(int minX, int minY, int minZ, int maxX, int maxY, int maxZ){        
        if (FrustumCulling.frustumIntersect.testAab(minX, minY, minZ, maxX, maxY, maxZ)){
            return true;
        }
        return false;
    }
}
