package demo.Geometry;

import demo.Enum.Directions;
import demo.Enum.Textures;
import demo.Meshing.Face;

public class Cube extends Object3D{

    public Cube(){
        this(Textures.NULL);
    }

    public Cube(Textures texture){   
        Face face = new Face();        
        face.direction = Directions.SOUTH;
        face.texture = texture.getTexture(face.direction);
        face.addVertex(0, 0, 0);
        face.addVertex(1, 0, 0);
        face.addVertex(1, 1, 0);
        face.addVertex(0, 1, 0);        
        this.mesh.addFaces(face);

        face = new Face();
        face.direction = Directions.NORTH;
        face.texture = texture.getTexture(face.direction);
        face.addVertex(1, 0, 1);
        face.addVertex(0, 0, 1);
        face.addVertex(0, 1, 1);
        face.addVertex(1, 1, 1);       
        this.mesh.addFaces(face);

        face = new Face();
        face.direction = Directions.WEST;
        face.texture = texture.getTexture(face.direction);
        face.addVertex(1, 0, 0);
        face.addVertex(1, 0, 1);
        face.addVertex(1, 1, 1);
        face.addVertex(1, 1, 0);        
        this.mesh.addFaces(face);

        face = new Face();
        face.direction = Directions.EAST;
        face.texture = texture.getTexture(face.direction);
        face.addVertex(0, 0, 1);
        face.addVertex(0, 0, 0);
        face.addVertex(0, 1, 0);
        face.addVertex(0, 1, 1);        
        this.mesh.addFaces(face);

        face = new Face();
        face.direction = Directions.UP;
        face.texture = texture.getTexture(face.direction);
        face.addVertex(0, 1, 0);
        face.addVertex(1, 1, 0);
        face.addVertex(1, 1, 1);
        face.addVertex(0, 1, 1);        
        this.mesh.addFaces(face);

        face = new Face();
        face.direction = Directions.DOWN;
        face.texture = texture.getTexture(face.direction);
        face.addVertex(0, 0, 1);
        face.addVertex(1, 0, 1);
        face.addVertex(1, 0, 0);
        face.addVertex(0, 0, 0);       
        this.mesh.addFaces(face);

    }
    /*/ Ancienne coordonnées
    public Cube(Textures texture){   
        Face face = new Face();        
        face.direction = Directions.SOUTH;
        face.texture = texture.getTexture(face.direction);
        face.addVertex(0.5f, -0.5f, 0.5f);
        face.addVertex(-0.5f, -0.5f, 0.5f);
        face.addVertex(-0.5f, 0.5f, 0.5f);
        face.addVertex(0.5f, 0.5f, 0.5f);        
        this.mesh.addFaces(face);

        face = new Face();
        face.direction = Directions.NORTH;
        face.texture = texture.getTexture(face.direction);
        face.addVertex(-0.5f, -0.5f, -0.5f);
        face.addVertex(0.5f, -0.5f, -0.5f);
        face.addVertex(0.5f, 0.5f, -0.5f);
        face.addVertex(-0.5f, 0.5f, -0.5f);        
        this.mesh.addFaces(face);

        face = new Face();
        face.direction = Directions.WEST;
        face.texture = texture.getTexture(face.direction);
        face.addVertex(-0.5f, -0.5f, 0.5f);
        face.addVertex(-0.5f, -0.5f, -0.5f);
        face.addVertex(-0.5f, 0.5f, -0.5f);
        face.addVertex(-0.5f, 0.5f, 0.5f);        
        this.mesh.addFaces(face);

        face = new Face();
        face.direction = Directions.EAST;
        face.texture = texture.getTexture(face.direction);
        face.addVertex(0.5f, -0.5f, -0.5f);
        face.addVertex(0.5f, -0.5f, 0.5f);
        face.addVertex(0.5f, 0.5f, 0.5f);
        face.addVertex(0.5f, 0.5f, -0.5f);        
        this.mesh.addFaces(face);

        face = new Face();
        face.direction = Directions.UP;
        face.texture = texture.getTexture(face.direction);
        face.addVertex(0.5f, 0.5f, 0.5f);
        face.addVertex(-0.5f, 0.5f, 0.5f);
        face.addVertex(-0.5f, 0.5f, -0.5f);
        face.addVertex(0.5f, 0.5f, -0.5f);        
        this.mesh.addFaces(face);

        face = new Face();
        face.direction = Directions.DOWN;
        face.texture = texture.getTexture(face.direction);
        face.addVertex(0.5f, -0.5f, -0.5f);
        face.addVertex(-0.5f, -0.5f, -0.5f);
        face.addVertex(-0.5f, -0.5f, 0.5f);
        face.addVertex(0.5f, -0.5f, 0.5f);        
        this.mesh.addFaces(face);

    }*/
}
