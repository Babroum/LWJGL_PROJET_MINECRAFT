package demo.Geometry;

import demo.Enum.Directions;
import demo.Enum.Textures;
import demo.Meshing.Face;

public class Cube extends Object3D{
    public Cube(Textures texture){   
        Face face = new Face();        
        face.texture = texture;
        face.addVertex(0.5f, -0.5f, 0.5f);
        face.addVertex(-0.5f, -0.5f, 0.5f);
        face.addVertex(-0.5f, 0.5f, 0.5f);
        face.addVertex(0.5f, 0.5f, 0.5f);
        face.direction = Directions.SOUTH;
        this.mesh.addFaces(face);

        face = new Face();
        face.texture = texture;
        face.addVertex(-0.5f, -0.5f, -0.5f);
        face.addVertex(0.5f, -0.5f, -0.5f);
        face.addVertex(0.5f, 0.5f, -0.5f);
        face.addVertex(-0.5f, 0.5f, -0.5f);
        face.direction = Directions.NORTH;
        this.mesh.addFaces(face);

        face = new Face();
        face.texture = texture;
        face.addVertex(-0.5f, -0.5f, 0.5f);
        face.addVertex(-0.5f, -0.5f, -0.5f);
        face.addVertex(-0.5f, 0.5f, -0.5f);
        face.addVertex(-0.5f, 0.5f, 0.5f);
        face.direction = Directions.WEST;
        this.mesh.addFaces(face);

        face = new Face();
        face.texture = texture;
        face.addVertex(0.5f, -0.5f, -0.5f);
        face.addVertex(0.5f, -0.5f, 0.5f);
        face.addVertex(0.5f, 0.5f, 0.5f);
        face.addVertex(0.5f, 0.5f, -0.5f);
        face.direction = Directions.EAST;
        this.mesh.addFaces(face);

        face = new Face();
        face.texture = texture;
        face.addVertex(0.5f, 0.5f, 0.5f);
        face.addVertex(-0.5f, 0.5f, 0.5f);
        face.addVertex(-0.5f, 0.5f, -0.5f);
        face.addVertex(0.5f, 0.5f, -0.5f);
        face.direction = Directions.UP;
        this.mesh.addFaces(face);

        face = new Face();
        face.texture = texture;
        face.addVertex(0.5f, -0.5f, -0.5f);
        face.addVertex(-0.5f, -0.5f, -0.5f);
        face.addVertex(-0.5f, -0.5f, 0.5f);
        face.addVertex(0.5f, -0.5f, 0.5f);
        face.direction = Directions.DOWN;
        this.mesh.addFaces(face);

    }
}
