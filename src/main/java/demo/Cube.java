package demo;

public class Cube extends Object3D{
    public Cube(){
        Face face = new Face();
        face.addVertex(new Vertex(0.5f, 0.5f, 0.5f));
        face.addVertex(new Vertex(-0.5f, 0.5f, 0.5f));
        face.addVertex(new Vertex(-0.5f, -0.5f, 0.5f));
        face.addVertex(new Vertex(0.5f, -0.5f, 0.5f));
        this.mesh.addFaces(face);

        face = new Face();
        face.addVertex(new Vertex(0.5f, 0.5f, -0.5f));
        face.addVertex(new Vertex(-0.5f, 0.5f, -0.5f));
        face.addVertex(new Vertex(-0.5f, 0.5f, 0.5f));
        face.addVertex(new Vertex(0.5f, 0.5f, 0.5f));
        this.mesh.addFaces(face);

        face = new Face();
        face.addVertex(new Vertex(-0.5f, 0.5f, 0.5f));
        face.addVertex(new Vertex(-0.5f, 0.5f, -0.5f));
        face.addVertex(new Vertex(-0.5f, -0.5f, -0.5f));
        face.addVertex(new Vertex(-0.5f, -0.5f, 0.5f));
        this.mesh.addFaces(face);
        
        face = new Face();
        face.addVertex(new Vertex(0.5f, 0.5f, -0.5f));
        face.addVertex(new Vertex(0.5f, 0.5f, 0.5f));
        face.addVertex(new Vertex(0.5f, -0.5f, 0.5f));
        face.addVertex(new Vertex(0.5f, -0.5f, -0.5f));
        this.mesh.addFaces(face);
        
        face = new Face();
        face.addVertex(new Vertex(0.5f, -0.5f, 0.5f));
        face.addVertex(new Vertex(-0.5f, -0.5f, 0.5f));
        face.addVertex(new Vertex(-0.5f, -0.5f, -0.5f));
        face.addVertex(new Vertex(0.5f, -0.5f, -0.5f));
        this.mesh.addFaces(face);
        
        face = new Face();
        face.addVertex(new Vertex(-0.5f, 0.5f, -0.5f));
        face.addVertex(new Vertex(0.5f, 0.5f, -0.5f));
        face.addVertex(new Vertex(0.5f, -0.5f, -0.5f));
        face.addVertex(new Vertex(-0.5f, -0.5f, -0.5f));
        this.mesh.addFaces(face);
    }
}
