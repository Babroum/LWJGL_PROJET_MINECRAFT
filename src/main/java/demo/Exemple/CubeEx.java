package demo.Exemple;

/**
 * Un Cube
 */
public class CubeEx extends Object3DEx{
    
    public CubeEx()
    {
        // Face de la boîte
        FaceEx face = new FaceEx();
        face.addVertex(-0.5f, 0.5f, 0.5f);
        face.addVertex(0.5f, 0.5f, 0.5f);
        face.addVertex(0.5f, -0.5f, 0.5f);
        face.addVertex(-0.5f, -0.5f, 0.5f);
        this.mesh.addFace(face);

        // Dos de la boîte
        face = new FaceEx();
        face.addVertex(-0.5f, -0.5f, -0.5f);
        face.addVertex(0.5f, -0.5f, -0.5f);
        face.addVertex(0.5f, 0.5f, -0.5f);
        face.addVertex(-0.5f, 0.5f, -0.5f);
        this.mesh.addFace(face);

        // Haut de la boîte
        face = new FaceEx();
        face.addVertex(-0.5f, 0.5f, -0.5f);
        face.addVertex(0.5f, 0.5f, -0.5f);
        face.addVertex(0.5f, 0.5f, 0.5f);
        face.addVertex(-0.5f, 0.5f, 0.5f);
        this.mesh.addFace(face);

        // Bas de la boîte
        face = new FaceEx();
        face.addVertex(-0.5f, -0.5f, 0.5f);
        face.addVertex(0.5f, -0.5f, 0.5f);
        face.addVertex(0.5f, -0.5f, -0.5f);
        face.addVertex(-0.5f, -0.5f, -0.5f);
        this.mesh.addFace(face);

        // Coté droit de la boîte
        face = new FaceEx();
        face.addVertex(0.5f, -0.5f, 0.5f);
        face.addVertex(0.5f, 0.5f, 0.5f);
        face.addVertex(0.5f, 0.5f, -0.5f);
        face.addVertex(0.5f, -0.5f, -0.5f);
        this.mesh.addFace(face);

        // Coté gauche de la boîte
        face = new FaceEx();
        face.addVertex(-0.5f, -0.5f, -0.5f);
        face.addVertex(-0.5f, 0.5f, -0.5f);
        face.addVertex(-0.5f, 0.5f, 0.5f);
        face.addVertex(-0.5f, -0.5f, 0.5f);
        this.mesh.addFace(face);
    }
}
