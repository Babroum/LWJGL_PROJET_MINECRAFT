package demo.Geometry;

import demo.Meshing.Face;
import demo.Others.BlockDefinition;
import demo.Others.Blocks;

@Deprecated
public class Cube extends Object3D{

    public Cube(){
        this(Blocks.NULL);
    }

    public Cube(BlockDefinition type){   
        Face face = new Face();        
        face = new Face();
        face.direction = "north";
        face.texture = type.getTextureLayer("north");
        face.addVertex(1, 0, 1);
        face.addVertex(0, 0, 1);
        face.addVertex(0, 1, 1);

        face.addVertex(0, 1, 1);
        face.addVertex(1, 1, 1);    
        face.addVertex(1, 0, 1);   
        this.mesh.addFaces(face);

        face.direction = "south";
        face.texture = type.getTextureLayer("south");   
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
        face.direction = "west";
        face.texture = type.getTextureLayer("west");
        face.addVertex(1, 0, 0);
        face.addVertex(1, 0, 1);
        face.addVertex(1, 1, 1);

        face.addVertex(1, 1, 1);
        face.addVertex(1, 1, 0);       
        face.addVertex(1, 0, 0); 
        this.mesh.addFaces(face);

        face = new Face();
        face.direction = "east";
        face.texture = type.getTextureLayer("east");
        face.addVertex(0, 0, 1);
        face.addVertex(0, 0, 0);
        face.addVertex(0, 1, 0);

        face.addVertex(0, 1, 0);
        face.addVertex(0, 1, 1);      
        face.addVertex(0, 0, 1);  
        this.mesh.addFaces(face);

        face = new Face();
        face.direction = "up";
        face.texture = type.getTextureLayer("up");
        face.addVertex(0, 1, 0);
        face.addVertex(1, 1, 0);
        face.addVertex(1, 1, 1);

        face.addVertex(1, 1, 1);
        face.addVertex(0, 1, 1);     
        face.addVertex(0, 1, 0);   
        this.mesh.addFaces(face);

        face = new Face();
        face.direction = "down";
        face.texture = type.getTextureLayer("down");
        face.addVertex(0, 0, 1);
        face.addVertex(1, 0, 1);
        face.addVertex(1, 0, 0);

        face.addVertex(1, 0, 0);
        face.addVertex(0, 0, 0);
        face.addVertex(0, 0, 1);
        this.mesh.addFaces(face);
    }
}
