package demo.Geometry;

import demo.Enum.Direction;
import demo.Meshing.Face;

public class Cube extends Object3D{
    public Cube(){   
        Face face = new Face();
        face.addVertex(0.5f, 0.5f, 0.5f);
        face.addVertex(-0.5f, 0.5f, 0.5f);
        face.addVertex(-0.5f, -0.5f, 0.5f);
        face.addVertex(0.5f, -0.5f, 0.5f);
        face.direction = Direction.SOUTH;
        this.mesh.addFaces(face);

        face = new Face();
        face.addVertex(0.5f, 0.5f, -0.5f);
        face.addVertex(-0.5f, 0.5f, -0.5f);
        face.addVertex(-0.5f, 0.5f, 0.5f);
        face.addVertex(0.5f, 0.5f, 0.5f);
        face.direction = Direction.UP;
        this.mesh.addFaces(face);

        face = new Face();
        face.addVertex(-0.5f, 0.5f, 0.5f);
        face.addVertex(-0.5f, 0.5f, -0.5f);
        face.addVertex(-0.5f, -0.5f, -0.5f);
        face.addVertex(-0.5f, -0.5f, 0.5f);
        face.direction = Direction.EAST;
        this.mesh.addFaces(face);
        
        face = new Face();
        face.addVertex(0.5f, 0.5f, -0.5f);
        face.addVertex(0.5f, 0.5f, 0.5f);        
        face.addVertex(0.5f, -0.5f, 0.5f);
        face.addVertex(0.5f, -0.5f, -0.5f);
        face.direction = Direction.WEST;
        this.mesh.addFaces(face);
        
        face = new Face();
        face.addVertex(0.5f, -0.5f, 0.5f);
        face.addVertex(-0.5f, -0.5f, 0.5f);
        face.addVertex(-0.5f, -0.5f, -0.5f);
        face.addVertex(0.5f, -0.5f, -0.5f);
        face.direction = Direction.DOWN;
        this.mesh.addFaces(face);
        
        face = new Face();
        face.addVertex(-0.5f, 0.5f, -0.5f);
        face.addVertex(0.5f, 0.5f, -0.5f);
        face.addVertex(0.5f, -0.5f, -0.5f);
        face.addVertex(-0.5f, -0.5f, -0.5f);
        face.direction = Direction.NORTH;
        this.mesh.addFaces(face);
    }
}
