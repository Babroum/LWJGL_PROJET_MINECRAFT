package demo.Geometry;

import demo.Enum.BlocksType;
import demo.Enum.Directions;
import demo.Meshing.Face;

public class Cube extends Object3D{

    public Cube(){
        this(BlocksType.NULL);
    }

    public Cube(BlocksType type){   

        Face face = new Face();        
        face.direction = Directions.SOUTH;
        face.texture = type.getTexture(face.direction);   
        // triangle 1
        face.addVertex(0, 0, 0); // 1
        face.addVertex(1, 0, 0); // 2
        face.addVertex(1, 1, 0); // 3
        // triangle 2
        face.addVertex(1, 1, 0); // 3
        face.addVertex(0, 1, 0); // 4
        face.addVertex(0, 0, 0); // 1 
        this.mesh.addFaces(face);

        face = new Face();
        face.direction = Directions.NORTH;
        face.texture = type.getTexture(face.direction);
        face.addVertex(1, 0, 1);
        face.addVertex(0, 0, 1);
        face.addVertex(0, 1, 1);

        face.addVertex(0, 1, 1);
        face.addVertex(1, 1, 1);    
        face.addVertex(1, 0, 1);   
        this.mesh.addFaces(face);

        face = new Face();
        face.direction = Directions.WEST;
        face.texture = type.getTexture(face.direction);
        face.addVertex(1, 0, 0);
        face.addVertex(1, 0, 1);
        face.addVertex(1, 1, 1);

        face.addVertex(1, 1, 1);
        face.addVertex(1, 1, 0);       
        face.addVertex(1, 0, 0); 
        this.mesh.addFaces(face);

        face = new Face();
        face.direction = Directions.EAST;
        face.texture = type.getTexture(face.direction);
        face.addVertex(0, 0, 1);
        face.addVertex(0, 0, 0);
        face.addVertex(0, 1, 0);

        face.addVertex(0, 1, 0);
        face.addVertex(0, 1, 1);      
        face.addVertex(0, 0, 1);  
        this.mesh.addFaces(face);

        face = new Face();
        face.direction = Directions.UP;
        face.texture = type.getTexture(face.direction);
        face.addVertex(0, 1, 0);
        face.addVertex(1, 1, 0);
        face.addVertex(1, 1, 1);

        face.addVertex(1, 1, 1);
        face.addVertex(0, 1, 1);     
        face.addVertex(0, 1, 0);   
        this.mesh.addFaces(face);

        face = new Face();
        face.direction = Directions.DOWN;
        face.texture = type.getTexture(face.direction);
        face.addVertex(0, 0, 1);
        face.addVertex(1, 0, 1);
        face.addVertex(1, 0, 0);

        face.addVertex(1, 0, 0);
        face.addVertex(0, 0, 0);
        face.addVertex(0, 0, 1);
        this.mesh.addFaces(face);
    }
}
